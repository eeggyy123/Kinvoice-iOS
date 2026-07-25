"""Production entry point for the KinVoice competition app."""

from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api.memory_ai import router as memory_ai_router
from app.config import settings
from app.utils.logger import logger


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("KinVoice API starting: environment={}", settings.environment)
    yield
    logger.info("KinVoice API stopped")


app = FastAPI(
    title="KinVoice Family Archive API",
    description="Stateless AI organization and grounded Q&A for family memories.",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs" if settings.docs_enabled else None,
    redoc_url=None,
)

origins = [value.strip() for value in settings.allowed_origins.split(",") if value.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type", "X-App-Token"],
)

app.include_router(memory_ai_router)


@app.get("/", tags=["health"])
async def root():
    return {"status": "ok", "service": "KinVoice", "version": "1.0.0"}


@app.get("/health", tags=["health"])
async def health():
    return {"status": "ok", "llm_configured": settings.llm_configured}


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error("unhandled error: type={} path={}", type(exc).__name__, request.url.path)
    return JSONResponse(status_code=500, content={"detail": "服务器暂时不可用，请稍后重试"})
