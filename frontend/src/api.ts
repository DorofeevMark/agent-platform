export type Agent = { id: string; name: string; owner: string; createdAt: string }

const apiBaseUrl = import.meta.env.VITE_CONTROL_PLANE_URL ?? ''

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, { headers: { 'Content-Type': 'application/json', ...options?.headers }, ...options })
  if (!response.ok) throw new Error((await response.text()) || `Request failed (${response.status})`)
  return response.json() as Promise<T>
}

export const listAgents = () => request<Agent[]>('/v1/agents')
export const createAgent = (agent: Pick<Agent, 'name' | 'owner'>) => request<Agent>('/v1/agents', { method: 'POST', body: JSON.stringify(agent) })
