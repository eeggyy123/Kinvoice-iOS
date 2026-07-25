"""Pydantic contracts for the focused family-memory AI endpoints."""

from pydantic import BaseModel, Field


class MemoryDraftRequest(BaseModel):
    transcript: str = Field(..., min_length=1, max_length=12000)
    narrator: str | None = Field(default=None, max_length=80)
    interview_prompt: str | None = Field(default=None, max_length=300)
    known_people: list[str] = Field(default_factory=list, max_length=20)


class MemoryDraftResponse(BaseModel):
    title: str = Field(..., max_length=80)
    summary: str = Field(..., max_length=300)
    content: str
    people: list[str] = Field(default_factory=list)
    time_hint: str | None = None
    location: str | None = None
    topics: list[str] = Field(default_factory=list)
    quote: str | None = None
    confidence: float = Field(default=0.5, ge=0, le=1)
    needs_review: bool = True


class KnowledgeMemory(BaseModel):
    id: str = Field(..., min_length=1, max_length=100)
    title: str = Field(..., min_length=1, max_length=100)
    content: str = Field(..., min_length=1, max_length=12000)
    people: list[str] = Field(default_factory=list, max_length=20)
    time_hint: str | None = Field(default=None, max_length=100)


class KnowledgeAskRequest(BaseModel):
    question: str = Field(..., min_length=1, max_length=500)
    memories: list[KnowledgeMemory] = Field(..., min_length=1, max_length=40)


class MemoryCitation(BaseModel):
    memory_id: str
    title: str
    excerpt: str


class KnowledgeAskResponse(BaseModel):
    answer: str
    citations: list[MemoryCitation] = Field(default_factory=list)
    grounded: bool = False

