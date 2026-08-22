package com.orbit.controlplane.support;

import com.orbit.controlplane.agents.application.AgentService;
import io.micronaut.context.ApplicationContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class PostgresIntegrationTest {
    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orbit_test")
            .withUsername("orbit")
            .withPassword("test-password");

    private ApplicationContext applicationContext;
    protected AgentService agentService;

    @BeforeAll
    void startApplication() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is required for PostgreSQL integration tests");
        postgres.start();
        applicationContext = ApplicationContext.run(java.util.Map.of(
                "datasources.default.url", postgres.getJdbcUrl(),
                "datasources.default.username", postgres.getUsername(),
                "datasources.default.password", postgres.getPassword()));
        agentService = applicationContext.getBean(AgentService.class);
    }

    @AfterEach
    void clearDatabase() throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("TRUNCATE deployment_revisions, deployments, agent_versions, agents CASCADE")) {
            statement.executeUpdate();
        }
    }

    @AfterAll
    void stopApplication() {
        if (applicationContext != null) {
            applicationContext.close();
        }
        postgres.stop();
    }

    protected Connection connection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
