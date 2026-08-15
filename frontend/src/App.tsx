import { type FormEvent, useCallback, useEffect, useState } from 'react'
import { createAgent, listAgents, type Agent } from './api'

const formatCreatedAt = (value: string) => new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

export default function App() {
  const [agents, setAgents] = useState<Agent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const refresh = useCallback(async () => {
    setLoading(true); setError(null)
    try { setAgents(await listAgents()) } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not reach the control plane.') } finally { setLoading(false) }
  }, [])
  useEffect(() => { void refresh() }, [refresh])
  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const data = new FormData(event.currentTarget); setCreating(true); setError(null)
    try {
      const agent = await createAgent({ name: String(data.get('name')), owner: String(data.get('owner')) })
      setAgents((current) => [...current, agent]); event.currentTarget.reset()
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not create the agent.') } finally { setCreating(false) }
  }
  return <main>
    <header className="topbar"><a className="brand" href="/" aria-label="Orbit home"><span>O</span> orbit</a><div className="environment"><i /> Development</div><button className="avatar" type="button" aria-label="Open account menu">MD</button></header>
    <section className="hero"><p className="eyebrow">CONTROL PLANE</p><h1>Agents</h1><p className="lede">Define, test, and deploy internal AI agents without managing the infrastructure beneath them.</p></section>
    <section className="content" aria-label="Agents"><div className="section-heading"><div><h2>Your agents</h2><p>Agents are isolated by identity, environment, and resource policy.</p></div><button className="secondary" onClick={() => void refresh()} disabled={loading}>Refresh</button></div>
      {error && <div className="notice" role="alert">{error}</div>}
      <div className="agent-grid"><form className="new-agent" onSubmit={handleCreate}><div className="plus">+</div><h3>Create an agent</h3><p>Start with a name and owner. Configuration becomes immutable versions.</p><label>Agent name<input name="name" pattern="[a-z0-9-]{3,63}" placeholder="support-triage" required /></label><label>Owner<input name="owner" type="email" placeholder="maya@acme.test" required /></label><button className="primary" type="submit" disabled={creating}>{creating ? 'Creating…' : 'Create agent'}</button></form>
        {loading ? <p className="state">Loading agents…</p> : agents.map((agent) => <article className="agent-card" key={agent.id}><div className="agent-icon">✦</div><div><h3>{agent.name}</h3><p>{agent.owner}</p></div><footer><span>Created {formatCreatedAt(agent.createdAt)}</span><button type="button" aria-label={`Open ${agent.name}`}>→</button></footer></article>)}
        {!loading && agents.length === 0 && <p className="state">No agents yet. Create the first one to get started.</p>}</div></section>
  </main>
}
