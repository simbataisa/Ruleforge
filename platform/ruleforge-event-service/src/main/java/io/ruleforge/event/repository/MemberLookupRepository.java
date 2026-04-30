package io.ruleforge.event.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MemberLookupRepository {

    private final JdbcTemplate jdbcTemplate;

    public MemberLookupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UUID> findMemberId(UUID orgId, UUID userId) {
        return jdbcTemplate.query(
                "SELECT id FROM members WHERE org_id = ? AND user_id = ?",
                rs -> rs.next() ? Optional.of(rs.getObject("id", UUID.class)) : Optional.empty(),
                orgId,
                userId
        );
    }
}
