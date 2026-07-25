"""Lightweight protection against accidental public API abuse."""

import hmac

from fastapi import Header, HTTPException

from app.config import settings


async def require_app_token(
    x_app_token: str | None = Header(default=None, alias="X-App-Token"),
) -> None:
    expected = settings.app_access_token.strip()
    if not expected:
        return
    if not x_app_token or not hmac.compare_digest(x_app_token, expected):
        raise HTTPException(status_code=401, detail="无效的应用访问凭证")
