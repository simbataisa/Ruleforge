package io.ruleforge.auth.controller;

import io.ruleforge.auth.dto.CreateSsoConfigRequest;
import io.ruleforge.auth.dto.SsoConfigDto;
import io.ruleforge.auth.dto.UpdateSsoConfigRequest;
import io.ruleforge.auth.service.SsoConfigService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/sso")
public class SsoController {

    private final SsoConfigService ssoConfigService;

    public SsoController(SsoConfigService ssoConfigService) {
        this.ssoConfigService = ssoConfigService;
    }

    @PostMapping("/config")
    public ResponseEntity<SsoConfigDto> create(@Valid @RequestBody CreateSsoConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ssoConfigService.create(request));
    }

    @GetMapping("/config")
    public ResponseEntity<List<SsoConfigDto>> listByOrg(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ssoConfigService.findByOrgId(orgId));
    }

    @GetMapping("/config/{id}")
    public ResponseEntity<SsoConfigDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ssoConfigService.findById(id));
    }

    @PutMapping("/config/{id}")
    public ResponseEntity<SsoConfigDto> update(@PathVariable UUID id,
                                               @RequestBody UpdateSsoConfigRequest request) {
        return ResponseEntity.ok(ssoConfigService.update(id, request));
    }

    @DeleteMapping("/config/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ssoConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // SSO callback endpoints (stub implementations for Phase 2)
    @GetMapping("/saml/metadata")
    public ResponseEntity<String> samlMetadata() {
        return ResponseEntity.ok(
            "<?xml version=\"1.0\"?><EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"/>");
    }

    @PostMapping("/saml/acs")
    public ResponseEntity<Void> samlAcs() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/oidc/callback")
    public ResponseEntity<Void> oidcCallback(@RequestParam(required = false) String code,
                                              @RequestParam(required = false) String state) {
        return ResponseEntity.ok().build();
    }
}
