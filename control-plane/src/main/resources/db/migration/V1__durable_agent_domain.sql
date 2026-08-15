CREATE TABLE agents (
    id UUID PRIMARY KEY,
    name VARCHAR(63) NOT NULL UNIQUE,
    owner VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_versions (
    id UUID PRIMARY KEY,
    agent_id UUID NOT NULL REFERENCES agents(id),
    number INTEGER NOT NULL CHECK (number > 0),
    system_prompt TEXT NOT NULL,
    model VARCHAR(255) NOT NULL,
    tools_json JSONB NOT NULL,
    resource_profile VARCHAR(32) NOT NULL,
    config_digest CHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT agent_versions_agent_number_unique UNIQUE (agent_id, number)
);

CREATE TABLE deployments (
    id UUID PRIMARY KEY,
    agent_id UUID NOT NULL REFERENCES agents(id),
    environment VARCHAR(32) NOT NULL,
    active_revision_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT deployments_agent_environment_unique UNIQUE (agent_id, environment)
);

CREATE TABLE deployment_revisions (
    id UUID PRIMARY KEY,
    deployment_id UUID NOT NULL REFERENCES deployments(id),
    version_id UUID NOT NULL REFERENCES agent_versions(id),
    number INTEGER NOT NULL CHECK (number > 0),
    status VARCHAR(32) NOT NULL,
    approval_reference VARCHAR(255),
    manifest TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT deployment_revisions_deployment_number_unique UNIQUE (deployment_id, number)
);

ALTER TABLE deployments
    ADD CONSTRAINT deployments_active_revision_fk
    FOREIGN KEY (active_revision_id) REFERENCES deployment_revisions(id);

CREATE INDEX agent_versions_agent_created_idx ON agent_versions (agent_id, number);
CREATE INDEX deployments_agent_created_idx ON deployments (agent_id, created_at DESC);
CREATE INDEX deployment_revisions_deployment_created_idx ON deployment_revisions (deployment_id, number DESC);
CREATE INDEX deployment_revisions_version_idx ON deployment_revisions (version_id);

CREATE FUNCTION reject_immutable_history_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% records are immutable', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER agent_versions_immutable
    BEFORE UPDATE OR DELETE ON agent_versions
    FOR EACH ROW EXECUTE FUNCTION reject_immutable_history_mutation();

CREATE TRIGGER deployment_revisions_immutable
    BEFORE UPDATE OR DELETE ON deployment_revisions
    FOR EACH ROW EXECUTE FUNCTION reject_immutable_history_mutation();
