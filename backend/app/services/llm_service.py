"""Small OpenAI-compatible Chat Completions client."""

import httpx

from app.config import settings


TIMEOUT_SECONDS = 35


def normalize_history_messages(messages: list[dict] | None) -> list[dict]:
    normalized: list[dict] = []
    for item in messages or []:
        if not isinstance(item, dict):
            continue
        role = item.get("role")
        if role not in {"user", "assistant", "system"} or item.get("content") is None:
            continue
        normalized.append({"role": role, "content": str(item.get("content"))})
    return normalized


async def call_llm(
    messages: list[dict],
    temperature: float = 0.2,
    max_tokens: int = 1024,
) -> str:
    """Call an OpenAI-compatible endpoint without exposing provider details to iOS."""
    if not settings.llm_configured:
        raise RuntimeError("LLM service is not configured")

    url = f"{settings.llm_api_base.rstrip('/')}/chat/completions"
    payload = {
        "model": settings.llm_model.strip(),
        "messages": normalize_history_messages(messages),
        "temperature": temperature,
        "max_tokens": max_tokens,
    }
    headers = {
        "Content-Type": "application/json; charset=utf-8",
        "Authorization": f"Bearer {settings.llm_api_key.strip()}",
    }

    timeout = httpx.Timeout(settings.llm_timeout_seconds)
    async with httpx.AsyncClient(timeout=timeout, trust_env=False) as client:
        response = await client.post(url, headers=headers, json=payload)
        response.raise_for_status()
        data = response.json()

    choices = data.get("choices") if isinstance(data, dict) else None
    if not choices or not isinstance(choices, list):
        raise ValueError("LLM response does not contain choices")
    message = choices[0].get("message") if isinstance(choices[0], dict) else None
    content = message.get("content") if isinstance(message, dict) else None
    if not isinstance(content, str) or not content.strip():
        raise ValueError("LLM response does not contain message content")
    return content.strip()


async def chat_completion(
    messages: list[dict],
    temperature: float = 0.2,
    max_tokens: int = 1024,
) -> str:
    return await call_llm(messages, temperature=temperature, max_tokens=max_tokens)
