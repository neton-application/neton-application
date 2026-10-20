# Neton Application repository guidance

For business features, forks, integration and review, use the installed `neton-application-development` skill, or read `skills/neton-application-development/SKILL.md` in a compatible neton-docs checkout. The canonical source and installation guide are at https://github.com/netonframework/netonframework.github.io/tree/main/skills. Do not assume a sibling checkout exists or install tools without user approval. Canonical specifications and skill bodies live in neton-docs; do not maintain duplicates here.

Resolve actual modules through this distribution's settings/build inputs and dependency locks. Reuse system/account/infra capabilities. Authentication is assembled by module-system; historical review findings must be checked against current code before being treated as open defects.

Inspect the working tree and preserve unrelated changes. Product-specific business requirements remain authoritative within the base contracts. Use disposable infrastructure for integration tests and headless browsers for acceptance. Report executed evidence, not assumed compliance.
