import unittest
from unittest.mock import AsyncMock, patch

from app.schemas.memory_ai import KnowledgeMemory, MemoryDraftRequest
from app.services.memory_ai_service import answer_from_memories, create_memory_draft


class MemoryAIServiceTest(unittest.IsolatedAsyncioTestCase):
    async def test_draft_parses_fenced_json(self):
        response = """```json
        {"title":"奶奶的红烧肉","summary":"一份家传做法","content":"先炒糖色。","people":["奶奶"],"time_hint":null,"location":"家里","topics":["菜谱"],"quote":"不能用老抽上色","confidence":0.86,"needs_review":true}
        ```"""
        with patch(
            "app.services.memory_ai_service.call_llm",
            new=AsyncMock(return_value=response),
        ):
            result = await create_memory_draft(
                MemoryDraftRequest(transcript="奶奶说红烧肉要先炒糖色，不能用老抽上色。")
            )

        self.assertEqual(result.title, "奶奶的红烧肉")
        self.assertEqual(result.people, ["奶奶"])
        self.assertTrue(result.needs_review)

    async def test_draft_degrades_without_valid_json(self):
        with patch(
            "app.services.memory_ai_service.call_llm",
            new=AsyncMock(return_value="not json"),
        ):
            result = await create_memory_draft(
                MemoryDraftRequest(transcript="1998年，爸爸第一次坐火车去广州。", narrator="爸爸")
            )

        self.assertIn("1998年", result.title)
        self.assertEqual(result.people, ["爸爸"])
        self.assertTrue(result.needs_review)

    async def test_answer_requires_relevant_source(self):
        memories = [
            KnowledgeMemory(
                id="m1",
                title="祖传红烧肉",
                content="炒糖色时要用小火，不能用老抽上色。",
                people=["奶奶"],
            )
        ]
        answer, citations, grounded = await answer_from_memories("爷爷年轻时在哪里工作？", memories)
        self.assertFalse(grounded)
        self.assertEqual(citations, [])
        self.assertEqual(answer, "家庭记忆中暂未找到相关记录。")

    async def test_answer_returns_only_valid_citations(self):
        memories = [
            KnowledgeMemory(
                id="m1",
                title="祖传红烧肉",
                content="奶奶做红烧肉时先用小火炒糖色。",
                people=["奶奶"],
            )
        ]
        response = '{"answer":"奶奶会先用小火炒糖色。","source_ids":["m1","missing"]}'
        with patch(
            "app.services.memory_ai_service.call_llm",
            new=AsyncMock(return_value=response),
        ):
            answer, citations, grounded = await answer_from_memories("奶奶怎么做红烧肉？", memories)

        self.assertTrue(grounded)
        self.assertEqual(answer, "奶奶会先用小火炒糖色。")
        self.assertEqual([item.memory_id for item in citations], ["m1"])

    async def test_answer_replaces_uncited_model_text_with_source_excerpt(self):
        memories = [
            KnowledgeMemory(
                id="m1",
                title="祖传红烧肉",
                content="奶奶做红烧肉时先用小火炒糖色。",
                people=["奶奶"],
            )
        ]
        response = '{"answer":"奶奶会先炒糖色。","source_ids":[]}'
        with patch(
            "app.services.memory_ai_service.call_llm",
            new=AsyncMock(return_value=response),
        ):
            answer, citations, grounded = await answer_from_memories("奶奶怎么做红烧肉？", memories)

        self.assertTrue(grounded)
        self.assertIn("祖传红烧肉", answer)
        self.assertEqual([item.memory_id for item in citations], ["m1"])

    async def test_answer_surfaces_relevant_excerpt_when_model_is_too_conservative(self):
        memories = [
            KnowledgeMemory(
                id="m1",
                title="祖传红烧肉",
                content="奶奶做红烧肉时先用小火炒糖色。",
                people=["奶奶"],
            )
        ]
        response = '{"answer":"家庭记忆中暂未找到相关记录。","source_ids":[]}'
        with patch(
            "app.services.memory_ai_service.call_llm",
            new=AsyncMock(return_value=response),
        ):
            answer, citations, grounded = await answer_from_memories("奶奶怎么做红烧肉？", memories)

        self.assertTrue(grounded)
        self.assertIn("请核对原文", answer)
        self.assertEqual([item.memory_id for item in citations], ["m1"])


if __name__ == "__main__":
    unittest.main()
