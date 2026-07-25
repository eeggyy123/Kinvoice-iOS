"""Focused API surface for the KinVoice family-memory iOS client."""

from fastapi import APIRouter, Depends

from app.schemas.memory_ai import (
    KnowledgeAskRequest,
    KnowledgeAskResponse,
    MemoryDraftRequest,
    MemoryDraftResponse,
)
from app.services.memory_ai_service import answer_from_memories, create_memory_draft
from app.security import require_app_token
from app.utils.logger import logger


router = APIRouter(
    prefix="/v1",
    tags=["家庭知识库"],
    dependencies=[Depends(require_app_token)],
)


@router.post("/memories/draft", response_model=MemoryDraftResponse)
async def draft_memory(request: MemoryDraftRequest):
    """Turn a transcript into an editable draft without persisting its content."""
    logger.info(
        "memory draft request: chars={} known_people={}",
        len(request.transcript),
        len(request.known_people),
    )
    return await create_memory_draft(request)


@router.post("/knowledge/ask", response_model=KnowledgeAskResponse)
async def ask_knowledge(request: KnowledgeAskRequest):
    """Answer only from client-supplied memories and return source citations."""
    logger.info(
        "knowledge ask request: question_chars={} memories={}",
        len(request.question),
        len(request.memories),
    )
    answer, citations, grounded = await answer_from_memories(
        request.question,
        request.memories,
    )
    return KnowledgeAskResponse(
        answer=answer,
        citations=citations,
        grounded=grounded,
    )
