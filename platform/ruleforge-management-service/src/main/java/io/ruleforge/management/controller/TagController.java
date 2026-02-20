package io.ruleforge.management.controller;

import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.management.dto.CreateTagRequest;
import io.ruleforge.management.dto.TagDto;
import io.ruleforge.management.service.TagService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping("/organizations/{orgId}/tags")
    @ResponseStatus(HttpStatus.CREATED)
    @AuditLog(action = "CREATE_TAG", resourceType = "Tag")
    public ApiResponse<TagDto> createTag(@PathVariable UUID orgId,
                                         @Valid @RequestBody CreateTagRequest request) {
        TagDto tag = tagService.createTag(orgId, request);
        return ApiResponse.ok(tag, "Tag created successfully");
    }

    @GetMapping("/organizations/{orgId}/tags")
    public ApiResponse<List<TagDto>> listTags(@PathVariable UUID orgId) {
        List<TagDto> tags = tagService.listTags(orgId);
        return ApiResponse.ok(tags);
    }

    @PutMapping("/tags/{tagId}")
    @AuditLog(action = "UPDATE_TAG", resourceType = "Tag")
    public ApiResponse<TagDto> updateTag(@PathVariable UUID tagId,
                                         @Valid @RequestBody CreateTagRequest request) {
        TagDto tag = tagService.updateTag(tagId, request);
        return ApiResponse.ok(tag, "Tag updated successfully");
    }

    @DeleteMapping("/tags/{tagId}")
    @AuditLog(action = "DELETE_TAG", resourceType = "Tag")
    public ApiResponse<Void> deleteTag(@PathVariable UUID tagId) {
        tagService.deleteTag(tagId);
        return ApiResponse.ok(null, "Tag deleted successfully");
    }

    @PostMapping("/rules/{ruleId}/tags/{tagId}")
    @AuditLog(action = "ADD_TAG_TO_RULE", resourceType = "RuleTag")
    public ApiResponse<Void> addTagToRule(@PathVariable UUID ruleId,
                                          @PathVariable UUID tagId) {
        tagService.addTagToRule(ruleId, tagId);
        return ApiResponse.ok(null, "Tag added to rule successfully");
    }

    @DeleteMapping("/rules/{ruleId}/tags/{tagId}")
    @AuditLog(action = "REMOVE_TAG_FROM_RULE", resourceType = "RuleTag")
    public ApiResponse<Void> removeTagFromRule(@PathVariable UUID ruleId,
                                               @PathVariable UUID tagId) {
        tagService.removeTagFromRule(ruleId, tagId);
        return ApiResponse.ok(null, "Tag removed from rule successfully");
    }
}
