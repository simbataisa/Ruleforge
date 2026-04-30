# Handoff 021 - InfoSec Architect → DevSecOps + Tech Lead

**Date:** 2026-04-30  
**Phase:** Epic 6 Phase 5 security resolution planning  
**From:** InfoSec Architect  
**To:** DevSecOps Engineer + Tech Lead  

## Summary

Reviewed active security gate findings and produced a concrete pass plan plus risk register entries.

## Saved Artifacts

- `docs/security/security-issue-resolution-plan.md`
- `docs/security/risk-register.md`

## Key Recommendations

1. Do not waive Story 028 for production: running containers as root is a baseline hardening failure.
2. Fix Dockerfiles first to clear overlapping Checkov, Trivy, and Semgrep findings.
3. Replace AI wildcard CORS with explicit allowed origins.
4. Migrate connector credential encryption from AES/CBC to AES/GCM with a backward-read plan if existing ciphertext may exist.
5. Add Redis TLS mode or document non-production-only cleartext behavior.
6. Upgrade SDK Java Jackson and frontend DOMPurify dependency paths.

## Next Action

DevSecOps should coordinate implementation of Story 028 first, then rerun Checkov, Trivy fs, and Semgrep before continuing with Story 029 and Story 030.
