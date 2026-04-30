# Security Risk Register

**Date:** 2026-04-30  
**Owner:** InfoSec Architect / Tech Lead  
**Scope:** Epic 6 Phase 5 security gate findings  

| ID | Risk | Likelihood | Impact | Rating | Owner | Treatment | Status | Reference |
|----|------|------------|--------|--------|-------|-----------|--------|-----------|
| R-SEC-001 | Runtime containers run as root, increasing blast radius after container compromise. | Medium | High | High | DevSecOps | Mitigated by adding non-root users to runtime Dockerfiles. | Mitigated | Story 028 |
| R-SEC-002 | Containers lack image-level health checks, reducing failure detection and deployment safety. | Medium | Medium | Medium | DevSecOps | Mitigated by adding service-specific health checks. | Mitigated | Story 028 |
| R-SEC-003 | AI service wildcard CORS with credentials can allow unintended browser origins. | Medium | High | High | Backend / AI Service | Mitigated with explicit environment-configured origins. | Mitigated | Story 029 |
| R-SEC-004 | Connector credentials encrypted with AES/CBC lack authenticated integrity protection. | Low | High | Medium | Backend / Connector Service | Mitigated with AES-GCM for new ciphertext. Legacy ciphertext migration posture accepted for this release. | Mitigated / Accepted residual | Story 029 |
| R-SEC-005 | Redis connector uses raw socket and may permit cleartext connectivity in production configs. | Medium | Medium | Medium | Backend / Connector Service | Mitigated with TLS-capable connection mode. | Mitigated | Story 029 |
| R-SEC-006 | Frontend cookie parsing uses dynamic regex and triggers ReDoS scanner finding. | Low | Medium | Low | Frontend | Mitigated with non-regex cookie parsing. | Mitigated | Story 029 |
| R-SEC-007 | DOMPurify advisories may expose sanitizer bypass paths through editor dependencies. | Low | High | Medium | Frontend | Mitigated by dependency updates and clean moderate audit/Trivy evidence. | Mitigated | Story 030 |
| R-SEC-008 | SDK Java Jackson advisory may allow DoS through async parser number length bypass. | Low | Medium | Medium | SDK Owner | Mitigated by upgrading Jackson to a fixed compatible version. | Mitigated | Story 030 |
| R-SEC-009 | Historical local-development secrets remain in git history. | Low | Medium | Accepted | Tech Lead / Release Owner | Accepted for current release; verify no production use and revisit history rewrite. | Accepted | Story 031 |
| R-SEC-010 | Hardened Dockerfiles have not been image-build and runtime-smoke verified in this sandbox. | Low | Medium | Medium | DevSecOps | Accepted for this release; run image builds and smoke checks in a Docker-enabled environment before production promotion. | Accepted | Risk acceptance remaining release risks |
| R-SEC-011 | Connector AES-GCM remediation may not decrypt any pre-existing legacy CBC connector credentials. | Low | Medium | Medium | Backend / Connector Service | Accepted for this release; rotate/re-save legacy connector credentials or migrate before production connector use if legacy data exists. | Accepted | Risk acceptance remaining release risks |
| R-SEC-012 | Semgrep reports parser warnings for generated Gradle wrapper scripts. | Low | Low | Low | Tech Lead | Accepted as generated-wrapper scanner limitation; product source has 0 Semgrep findings. | Accepted | Risk acceptance remaining release risks |

## Risk Acceptance Requirements

Accepted release risks are documented in:

- `docs/security/risk-acceptance-historical-gitleaks-2026-04-30.md`
- `docs/security/risk-acceptance-remaining-release-risks-2026-04-30.md`
