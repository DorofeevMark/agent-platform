"""Minimal config-driven agent runtime. It deliberately has no direct third-party tool clients."""
from os import getenv
from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field
import httpx

app = FastAPI(title="Orbit Agent Runtime", version="0.1.0")
GATEWAY_URL = getenv("ORBIT_TOOL_GATEWAY_URL", "http://localhost:8090")
VERSION_ID = getenv("ORBIT_AGENT_VERSION_ID", "local-version")

class InvokeRequest(BaseModel):
    message: str = Field(min_length=1, max_length=20_000)
    tool: str | None = None
    arguments: dict[str, object] = Field(default_factory=dict)

@app.get("/healthz")
def health() -> dict[str, str]:
    return {"status": "ok", "agentVersionId": VERSION_ID}

@app.post("/invoke")
async def invoke(request: InvokeRequest, authorization: str | None = Header(default=None)) -> dict:
    """Tool execution is delegated to the gateway, carrying the workload token onward."""
    if not request.tool:
        return {"agentVersionId": VERSION_ID, "message": "No tool selected; model adapter is not configured yet."}
    headers = {"X-Orbit-Agent-Version": VERSION_ID}
    if authorization:
        headers["Authorization"] = authorization
    async with httpx.AsyncClient(timeout=15) as client:
        response = await client.post(f"{GATEWAY_URL}/v1/tool-invocations", json={"tool": request.tool, "arguments": request.arguments}, headers=headers)
    if response.status_code >= 400:
        raise HTTPException(status_code=response.status_code, detail="Tool Gateway denied or failed the invocation")
    return {"agentVersionId": VERSION_ID, "toolResult": response.json()}
