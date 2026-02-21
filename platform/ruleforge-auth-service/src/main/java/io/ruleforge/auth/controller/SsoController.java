package io.ruleforge.auth.controller;

import io.ruleforge.auth.dto.AuthResponse;
import io.ruleforge.auth.dto.CreateSsoConfigRequest;
import io.ruleforge.auth.dto.SsoConfigDto;
import io.ruleforge.auth.dto.UpdateSsoConfigRequest;
import io.ruleforge.auth.service.SsoAuthenticationService;
import io.ruleforge.auth.service.SsoConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/sso")
public class SsoController {

    private static final Logger log = LoggerFactory.getLogger(SsoController.class);

    private final SsoConfigService ssoConfigService;
    private final SsoAuthenticationService ssoAuthenticationService;

    public SsoController(SsoConfigService ssoConfigService,
                         SsoAuthenticationService ssoAuthenticationService) {
        this.ssoConfigService = ssoConfigService;
        this.ssoAuthenticationService = ssoAuthenticationService;
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

    @GetMapping("/saml/metadata")
    public ResponseEntity<String> samlMetadata(@RequestParam(required = false) UUID orgId) {
        String metadata = ssoAuthenticationService.generateSamlMetadata(orgId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/xml")
                .body(metadata);
    }

    @PostMapping("/saml/acs")
    public ResponseEntity<AuthResponse> samlAcs(
            @RequestParam("SAMLResponse") String samlResponse,
            @RequestParam(value = "RelayState", required = false) String relayState) {
        log.info("Processing SAML ACS callback, relayState={}", relayState);
        AuthResponse response = ssoAuthenticationService.processSamlResponse(samlResponse, relayState);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/oidc/callback")
    public ResponseEntity<AuthResponse> oidcCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        if (error != null) {
            log.warn("OIDC callback received error: {}", error);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Processing OIDC callback, state={}", state);
        AuthResponse response = ssoAuthenticationService.processOidcCallback(code, state);
        return ResponseEntity.ok(response);
    }
}
