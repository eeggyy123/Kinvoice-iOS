"""Pydantic contracts for the focused family-memory AI endpoints."""

from typing import Literal

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


class InterviewTurn(BaseModel):
    role: Literal["assistant", "user"]
    content: str = Field(..., min_length=1, max_length=2000)


class InterviewNextRequest(BaseModel):
    narrator_name: str = Field(..., min_length=1, max_length=80)
    relation: str = Field(default="家人", max_length=40)
    theme: str = Field(default="人生故事", max_length=100)
    turns: list[InterviewTurn] = Field(default_factory=list, max_length=16)


class InterviewNextResponse(BaseModel):
    question: str = Field(..., min_length=1, max_length=160)
    reason: str = Field(default="继续了解这段经历", max_length=100)
    should_finish: bool = False
    degraded: bool = False


class InterviewProfile(BaseModel):
    display_name: str = Field(..., min_length=1, max_length=80)
    relation: str = Field(default="家人", max_length=40)
    bio: str = Field(default="", max_length=500)
    traits: list[str] = Field(default_factory=list, max_length=12)


class InterviewMemoryDraft(BaseModel):
    title: str = Field(..., min_length=1, max_length=80)
    summary: str = Field(default="", max_length=300)
    content: str = Field(..., min_length=1, max_length=4000)
    time_hint: str | None = Field(default=None, max_length=100)
    location: str | None = Field(default=None, max_length=100)
    topics: list[str] = Field(default_factory=list, max_length=8)
    quote: str | None = Field(default=None, max_length=240)
    source_turns: list[int] = Field(default_factory=list, max_length=8)
    needs_review: bool = True


class InterviewSummarizeRequest(InterviewNextRequest):
    pass


class InterviewSummarizeResponse(BaseModel):
    profile: InterviewProfile
    memories: list[InterviewMemoryDraft] = Field(default_factory=list, max_length=5)
    degraded: bool = False
