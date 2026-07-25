"""AI-assisted organization and grounded Q&A for family memories."""

import asyncio
import json
import re

from app.schemas.memory_ai import (
    KnowledgeMemory,
    MemoryCitation,
    MemoryDraftRequest,
    MemoryDraftResponse,
)
from app.services.llm_service import call_llm
from app.utils.logger import logger


_JSON_BLOCK = re.compile(r"\{.*\}", re.DOTALL)
_WORD = re.compile(r"[\w\u4e00-\u9fff]+")
_AI_TIMEOUT_SECONDS = 35

_DRAFT_PROMPT = """你是家庭口述史编辑。请把口述内容整理为忠于原意、便于家人校订的记忆草稿。

只输出一个 JSON 对象，字段必须为：
title, summary, content, people, time_hint, location, topics, quote, confidence, needs_review。

规则：
1. 不补写口述中没有的事实；不确定的信息保留模糊表达。
2. content 使用自然中文分段，保留讲述者语气，不进行心理诊断或价值评判。
3. quote 只能逐字引用原文；没有合适原句时为 null。
4. people 和 topics 为字符串数组，time_hint/location 可为 null。
5. confidence 为 0 到 1；只要时间、地点或人物存在歧义，needs_review 必须为 true。
"""

_ASK_PROMPT = """你是家庭知识库检索助手。只能根据提供的家庭记忆回答。

只输出一个 JSON 对象：
{"answer":"...","source_ids":["记忆ID"]}

规则：
1. 不使用常识补齐家庭事实，不推测人物动机。
2. 每个事实都必须能由 source_ids 对应记忆支持。
3. 找不到答案时，answer 固定为“家庭记忆中暂未找到相关记录。”，source_ids 返回空数组。
4. 回答简洁、温和，最多 180 字。
"""


def _extract_json(raw: str) -> dict:
    """Parse a JSON object even when a model wrapped it in a code fence."""
    text = (raw or "").strip()
    if text.startswith("```"):
        text = "\n".join(
            line for line in text.splitlines() if not line.strip().startswith("```")
        ).strip()
    try:
        value = json.loads(text)
    except json.JSONDecodeError:
        match = _JSON_BLOCK.search(text)
        if not match:
            raise
        value = json.loads(match.group(0))
    if not isinstance(value, dict):
        raise ValueError("model response must be a JSON object")
    return value


def _clean_list(value, limit: int = 12) -> list[str]:
    if not isinstance(value, list):
        return []
    result = []
    for item in value:
        text = str(item).strip()
        if text and text not in result:
            result.append(text[:80])
    return result[:limit]


def _fallback_draft(req: MemoryDraftRequest) -> MemoryDraftResponse:
    transcript = req.transcript.strip()
    first_sentence = re.split(r"[。！？!?\n]", transcript, maxsplit=1)[0].strip()
    title = first_sentence[:28] or "一段家庭记忆"
    summary = transcript[:120] + ("..." if len(transcript) > 120 else "")
    people = [req.narrator.strip()] if req.narrator and req.narrator.strip() else []
    return MemoryDraftResponse(
        title=title,
        summary=summary,
        content=transcript,
        people=people,
        topics=["待整理"],
        quote=first_sentence[:100] or None,
        confidence=0.35,
        needs_review=True,
    )


async def create_memory_draft(req: MemoryDraftRequest) -> MemoryDraftResponse:
    context = {
        "narrator": req.narrator,
        "interview_prompt": req.interview_prompt,
        "known_people": req.known_people,
        "transcript": req.transcript,
    }
    messages = [
        {"role": "system", "content": _DRAFT_PROMPT},
        {"role": "user", "content": json.dumps(context, ensure_ascii=False)},
    ]
    try:
        raw = await asyncio.wait_for(call_llm(messages), timeout=_AI_TIMEOUT_SECONDS)
        data = _extract_json(raw)
        fallback = _fallback_draft(req)
        confidence = data.get("confidence", 0.5)
        try:
            confidence = max(0.0, min(1.0, float(confidence)))
        except (TypeError, ValueError):
            confidence = 0.5
        return MemoryDraftResponse(
            title=str(data.get("title") or fallback.title).strip()[:80],
            summary=str(data.get("summary") or fallback.summary).strip()[:300],
            content=str(data.get("content") or fallback.content).strip(),
            people=_clean_list(data.get("people")) or fallback.people,
            time_hint=(str(data["time_hint"]).strip()[:100] if data.get("time_hint") else None),
            location=(str(data["location"]).strip()[:100] if data.get("location") else None),
            topics=_clean_list(data.get("topics"), limit=8),
            quote=(str(data["quote"]).strip()[:240] if data.get("quote") else None),
            confidence=confidence,
            needs_review=bool(data.get("needs_review", True)),
        )
    except Exception as error:
        logger.warning(f"memory draft AI degraded: {type(error).__name__}")
        return _fallback_draft(req)


def _tokens(text: str) -> set[str]:
    """Create lightweight tokens for deterministic offline source ranking."""
    tokens: set[str] = set()
    for raw in _WORD.findall(text.lower()):
        if len(raw) <= 1:
            continue
        tokens.add(raw)
        if any("\u4e00" <= char <= "\u9fff" for char in raw):
            tokens.update(raw[index:index + 2] for index in range(len(raw) - 1))
    return tokens


def _rank_memories(question: str, memories: list[KnowledgeMemory]) -> list[KnowledgeMemory]:
    question_tokens = _tokens(question)
    scored = []
    for memory in memories:
        body = " ".join([memory.title, memory.content, *memory.people])
        overlap = len(question_tokens & _tokens(body))
        if overlap:
            scored.append((overlap, memory))
    scored.sort(key=lambda item: item[0], reverse=True)
    return [memory for _, memory in scored[:5]]


def _citation(memory: KnowledgeMemory) -> MemoryCitation:
    excerpt = memory.content.strip().replace("\n", " ")
    if len(excerpt) > 120:
        excerpt = excerpt[:120] + "..."
    return MemoryCitation(memory_id=memory.id, title=memory.title, excerpt=excerpt)


async def answer_from_memories(
    question: str,
    memories: list[KnowledgeMemory],
) -> tuple[str, list[MemoryCitation], bool]:
    candidates = _rank_memories(question, memories)
    if not candidates:
        return "家庭记忆中暂未找到相关记录。", [], False

    payload = {
        "question": question,
        "memories": [memory.model_dump() for memory in candidates],
    }
    messages = [
        {"role": "system", "content": _ASK_PROMPT},
        {"role": "user", "content": json.dumps(payload, ensure_ascii=False)},
    ]
    by_id = {memory.id: memory for memory in candidates}
    try:
        raw = await asyncio.wait_for(call_llm(messages), timeout=_AI_TIMEOUT_SECONDS)
        data = _extract_json(raw)
        source_ids = [str(value) for value in data.get("source_ids", [])]
        cited = [by_id[source_id] for source_id in source_ids if source_id in by_id]
        answer = str(data.get("answer") or "").strip()
        if not answer or answer == "家庭记忆中暂未找到相关记录。":
            lead = candidates[0]
            citation = _citation(lead)
            return f"找到一条可能相关的家庭记忆，请核对原文：{citation.excerpt}", [citation], True
        if not cited:
            # Never display an uncited model answer. Some compatible models do not
            # reliably echo IDs, so fall back to a verbatim excerpt with a source.
            lead = candidates[0]
            citation = _citation(lead)
            return f"在《{lead.title}》中记录了：{citation.excerpt}", [citation], True
        return answer[:500], [_citation(memory) for memory in cited], True
    except Exception as error:
        logger.warning(f"grounded Q&A AI degraded: {type(error).__name__}")
        lead = candidates[0]
        answer = f"在《{lead.title}》中记录了：{_citation(lead).excerpt}"
        return answer, [_citation(lead)], True
