package io.ruleforge.management.controller;

import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.AddTagsRequest;
import io.ruleforge.management.dto.TagAutocompleteDto;
import io.ruleforge.management.service.TagService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    /**
     * Tag autocomplete — returns tags with rule counts, filtered by prefix.
     * GET /v1/tags?prefix=fraud&limit=20
     */
    @GetMapping("/tags")
    public ApiResponse<List<TagAutocompleteDto>> autocomplete(
            @RequestParam(required = false) String prefix,
            @RequestParam(defaultValue = "20") int limit) {
        UUID orgId = TenantContext.get().orgId();
        List<TagAutocompleteDto> tags = tagService.autocomplete(orgId, prefix, Math.min(limit, 100));
        return ApiResponse.ok(tags);
    }

    /**
     * Add tags to a rule (idempotent).
     * POST /v1/rules/{ruleId}/tags  Body: { "tags": ["fraud", "payments"] }
     */
    @PostMapping("/rules/{ruleId}/tags")
    @AuditLog(action = "ADD_TAGS_TO_RULE", resourceType = "RuleTag")
    public ApiResponse<List<String>> addTagsToRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody AddTagsRequest request) {
        List<String> tags = tagService.addTagsToRule(ruleId, request.tags());
        return ApiResponse.ok(tags, "Tags added successfully");
    }

    /**
     * Remove a tag from a rule.
     * DELETE /v1/rules/{ruleId}/tags/{tag}
     */
    @DeleteMapping("/rules/{ruleId}/tags/{tag}")
    @AuditLog(action = "REMOVE_TAG_FROM_RULE", resourceType = "RuleTag")
    public ApiResponse<Void> removeTagFromRule(
            @PathVariable UUID ruleId,
            @PathVariable String tag) {
        tagService.removeTagFromRule(ruleId, tag);
        return ApiResponse.ok(null, "Tag removed successfully");
    }

    /**
     * Get all tags for a rule.
     * GET /v1/rules/{ruleId}/tags
     */
    @GetMapping("/rules/{ruleId}/tags")
    public ApiResponse<List<String>> getTagsForRule(@PathVariable UUID ruleId) {
        List<String> tags = tagService.getTagsForRule(ruleId);
        return ApiResponse.ok(tags);
    }
}
