package io.ruleforge.management.controller;

import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.CreateCommentRequest;
import io.ruleforge.management.dto.RuleVersionCommentDto;
import io.ruleforge.management.service.CommentService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/rules/{ruleId}/versions/{version}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ApiResponse<List<RuleVersionCommentDto>> getComments(@PathVariable UUID ruleId,
                                                                 @PathVariable int version) {
        List<RuleVersionCommentDto> comments = commentService.getComments(ruleId, version);
        return ApiResponse.ok(comments);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AuditLog(action = "ADD_COMMENT", resourceType = "RuleVersionComment")
    public ApiResponse<RuleVersionCommentDto> addComment(@PathVariable UUID ruleId,
                                                          @PathVariable int version,
                                                          @Valid @RequestBody CreateCommentRequest request) {
        UUID authorId = TenantContext.get().userId();
        RuleVersionCommentDto comment = commentService.addComment(ruleId, version, authorId,
                request.body(), request.parentId());
        return ApiResponse.ok(comment, "Comment added successfully");
    }
}
