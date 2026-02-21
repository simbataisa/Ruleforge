package io.ruleforge.analytics.repository;

import io.ruleforge.analytics.model.DecisionLogDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data Elasticsearch repository for {@link DecisionLogDocument}.
 * Provides derived query methods for filtering decision logs by org, rule, and time range.
 */
public interface DecisionLogRepository extends ElasticsearchRepository<DecisionLogDocument, String> {

    List<DecisionLogDocument> findByOrgIdAndTimestampBetween(
            String orgId, Instant from, Instant to, Pageable pageable);

    List<DecisionLogDocument> findByRuleIdAndTimestampBetween(
            String ruleId, Instant from, Instant to, Pageable pageable);

    long countByRuleId(String ruleId);

    long countByOrgId(String orgId);
}
