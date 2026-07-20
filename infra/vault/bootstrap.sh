#!/usr/bin/env bash
# Ch.40 §3 ADR-066: one-time Vault bootstrap, run with the root token —
# everything here is privileged setup (enabling the transit engine,
# creating the signing key, writing the least-privilege policy, creating
# the AppRole) that Certification's own runtime credentials are
# deliberately NOT allowed to do (see certificate-signing-policy.hcl and
# VaultSigningService's doc comment). Vault dev mode has no
# docker-entrypoint-initdb.d equivalent, so unlike Postgres's
# init-app-role.sql this can't run itself on container start — run it
# manually once after `docker compose up -d vault`.
#
# Re-running this against an already-bootstrapped Vault is safe for the
# engine/key/policy/role steps (idempotent-ish — Vault no-ops or 400s
# harmlessly on "already exists"), but generates a BRAND NEW secret_id
# every time, which will not match modules/certification/src/main/
# resources/application.yml's committed value. If you re-run this,
# update vault.secret-id there to match the new output.
set -euo pipefail

VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="elemes-root-token"

curl -s -X POST "$VAULT_ADDR/v1/sys/mounts/transit" \
  -H "X-Vault-Token: $VAULT_TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"transit"}' > /dev/null || true

curl -s -X POST "$VAULT_ADDR/v1/transit/keys/certificate-signing" \
  -H "X-Vault-Token: $VAULT_TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"rsa-2048"}' > /dev/null || true

curl -s -X PUT "$VAULT_ADDR/v1/sys/policies/acl/certificate-signing-policy" \
  -H "X-Vault-Token: $VAULT_TOKEN" -H "Content-Type: application/json" \
  -d "{\"policy\": $(python3 -c "import json,sys; print(json.dumps(open('$(dirname "$0")/certificate-signing-policy.hcl').read()))" 2>/dev/null || node -e "console.log(JSON.stringify(require('fs').readFileSync('$(dirname "$0")/certificate-signing-policy.hcl','utf8')))")}" \
  > /dev/null

curl -s -X POST "$VAULT_ADDR/v1/sys/auth/approle" \
  -H "X-Vault-Token: $VAULT_TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"approle"}' > /dev/null || true

curl -s -X POST "$VAULT_ADDR/v1/auth/approle/role/certification-service" \
  -H "X-Vault-Token: $VAULT_TOKEN" -H "Content-Type: application/json" \
  -d '{"token_policies":"certificate-signing-policy","token_ttl":"1h","token_max_ttl":"4h","secret_id_ttl":"0"}' > /dev/null

ROLE_ID=$(curl -s "$VAULT_ADDR/v1/auth/approle/role/certification-service/role-id" \
  -H "X-Vault-Token: $VAULT_TOKEN" | grep -o '"role_id":"[^"]*"' | cut -d'"' -f4)

SECRET_ID=$(curl -s -X POST "$VAULT_ADDR/v1/auth/approle/role/certification-service/secret-id" \
  -H "X-Vault-Token: $VAULT_TOKEN" | grep -o '"secret_id":"[^"]*"' | cut -d'"' -f4)

echo "Bootstrap complete. Put these in modules/certification/src/main/resources/application.yml if different from what's already committed:"
echo "  vault.role-id:   $ROLE_ID"
echo "  vault.secret-id: $SECRET_ID"
