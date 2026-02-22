package io.ruleforge.management.controller;

import io.ruleforge.common.audit.AuditLog;
import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.management.dto.CreateFolderRequest;
import io.ruleforge.management.dto.FolderDto;
import io.ruleforge.management.dto.UpdateFolderRequest;
import io.ruleforge.management.entity.SpaceEntity;
import io.ruleforge.management.repository.SpaceRepository;
import io.ruleforge.management.service.FolderService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class FolderController {

    private final FolderService folderService;
    private final SpaceRepository spaceRepository;

    public FolderController(FolderService folderService, SpaceRepository spaceRepository) {
        this.folderService = folderService;
        this.spaceRepository = spaceRepository;
    }

    /**
     * List folders across all spaces for the current org (convenience endpoint for frontend).
     */
    @GetMapping("/folders")
    public ApiResponse<List<FolderDto>> listFoldersForOrg(
            @RequestParam(required = false) UUID parentId) {
        UUID orgId = TenantContext.get().orgId();
        List<UUID> spaceIds = spaceRepository.findByOrgId(orgId).stream()
                .map(SpaceEntity::getId)
                .toList();

        List<FolderDto> allFolders = new ArrayList<>();
        for (UUID spaceId : spaceIds) {
            allFolders.addAll(folderService.listFolderTree(spaceId));
        }
        return ApiResponse.ok(allFolders);
    }

    @PostMapping("/spaces/{spaceId}/folders")
    @ResponseStatus(HttpStatus.CREATED)
    @AuditLog(action = "CREATE_FOLDER", resourceType = "Folder")
    public ApiResponse<FolderDto> createFolder(@PathVariable UUID spaceId,
                                               @Valid @RequestBody CreateFolderRequest request) {
        FolderDto folder = folderService.createFolder(spaceId, request);
        return ApiResponse.ok(folder, "Folder created successfully");
    }

    @GetMapping("/spaces/{spaceId}/folders")
    public ApiResponse<List<FolderDto>> listFolders(@PathVariable UUID spaceId) {
        List<FolderDto> folders = folderService.listFolderTree(spaceId);
        return ApiResponse.ok(folders);
    }

    @GetMapping("/folders/{folderId}")
    public ApiResponse<FolderDto> getFolder(@PathVariable UUID folderId) {
        FolderDto folder = folderService.getFolder(folderId);
        return ApiResponse.ok(folder);
    }

    @PutMapping("/folders/{folderId}")
    @AuditLog(action = "UPDATE_FOLDER", resourceType = "Folder")
    public ApiResponse<FolderDto> updateFolder(@PathVariable UUID folderId,
                                               @Valid @RequestBody UpdateFolderRequest request) {
        FolderDto folder = folderService.updateFolder(folderId, request);
        return ApiResponse.ok(folder, "Folder updated successfully");
    }

    @DeleteMapping("/folders/{folderId}")
    @AuditLog(action = "DELETE_FOLDER", resourceType = "Folder")
    public ApiResponse<Void> deleteFolder(@PathVariable UUID folderId) {
        folderService.deleteFolder(folderId);
        return ApiResponse.ok(null, "Folder deleted successfully");
    }
}
