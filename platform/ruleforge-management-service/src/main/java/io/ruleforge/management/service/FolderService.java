package io.ruleforge.management.service;

import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.management.dto.CreateFolderRequest;
import io.ruleforge.management.dto.FolderDto;
import io.ruleforge.management.dto.UpdateFolderRequest;
import io.ruleforge.management.entity.FolderEntity;
import io.ruleforge.management.repository.FolderRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FolderService {

    private static final Logger log = LoggerFactory.getLogger(FolderService.class);

    private final FolderRepository folderRepository;

    public FolderService(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    @Transactional
    public FolderDto createFolder(UUID spaceId, CreateFolderRequest request) {
        FolderEntity folder = new FolderEntity();
        folder.setSpaceId(spaceId);
        folder.setParentId(request.parentId());
        folder.setName(request.name());
        folder.setSortOrder(request.sortOrder());
        folder = folderRepository.save(folder);

        log.info("Folder created: id={}, name={}, spaceId={}", folder.getId(), folder.getName(), spaceId);

        return toDto(folder, List.of());
    }

    @Transactional(readOnly = true)
    public List<FolderDto> listFolderTree(UUID spaceId) {
        List<FolderEntity> allFolders = folderRepository.findBySpaceId(spaceId);

        // Group by parentId
        Map<UUID, List<FolderEntity>> childrenMap = allFolders.stream()
                .filter(f -> f.getParentId() != null)
                .collect(Collectors.groupingBy(FolderEntity::getParentId));

        // Build tree from root folders
        List<FolderEntity> rootFolders = allFolders.stream()
                .filter(f -> f.getParentId() == null)
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .toList();

        return rootFolders.stream()
                .map(root -> buildTree(root, childrenMap))
                .toList();
    }

    @Transactional(readOnly = true)
    public FolderDto getFolder(UUID folderId) {
        FolderEntity folder = findFolderOrThrow(folderId);

        List<FolderEntity> allFolders = folderRepository.findBySpaceId(folder.getSpaceId());
        Map<UUID, List<FolderEntity>> childrenMap = allFolders.stream()
                .filter(f -> f.getParentId() != null)
                .collect(Collectors.groupingBy(FolderEntity::getParentId));

        return buildTree(folder, childrenMap);
    }

    @Transactional
    public FolderDto updateFolder(UUID folderId, UpdateFolderRequest request) {
        FolderEntity folder = findFolderOrThrow(folderId);

        if (request.name() != null) {
            folder.setName(request.name());
        }
        folder.setSortOrder(request.sortOrder());
        folder = folderRepository.save(folder);

        log.info("Folder updated: id={}", folderId);

        return toDto(folder, List.of());
    }

    @Transactional
    public void deleteFolder(UUID folderId) {
        FolderEntity folder = findFolderOrThrow(folderId);
        folderRepository.delete(folder);
        log.info("Folder deleted: id={}", folderId);
    }

    private FolderEntity findFolderOrThrow(UUID folderId) {
        return folderRepository.findById(folderId)
                .orElseThrow(() -> new EntityNotFoundException("Folder", folderId.toString()));
    }

    private FolderDto buildTree(FolderEntity folder, Map<UUID, List<FolderEntity>> childrenMap) {
        List<FolderEntity> children = childrenMap.getOrDefault(folder.getId(), List.of());
        List<FolderDto> childDtos = children.stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(child -> buildTree(child, childrenMap))
                .toList();

        return toDto(folder, childDtos);
    }

    private FolderDto toDto(FolderEntity folder, List<FolderDto> children) {
        return new FolderDto(
                folder.getId(),
                folder.getSpaceId(),
                folder.getName(),
                folder.getParentId(),
                folder.getSortOrder(),
                children
        );
    }
}
