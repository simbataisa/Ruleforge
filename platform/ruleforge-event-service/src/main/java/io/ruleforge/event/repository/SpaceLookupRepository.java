package io.ruleforge.event.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class SpaceLookupRepository {

    private final JdbcTemplate jdbcTemplate;

    public SpaceLookupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UUID> findOrgId(UUID spaceId) {
        return jdbcTemplate.query(
                "SELECT org_id FROM spaces WHERE id = ?",
                rs -> rs.next() ? Optional.of(rs.getObject("org_id", UUID.class)) : Optional.empty(),
                spaceId
        );
    }
}
