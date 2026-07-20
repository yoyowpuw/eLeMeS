# Ch.40 §3 ADR-066: least-privilege policy for Certification's own service
# identity — "access strictly limited to the Certification context's
# signing operation," not blanket Vault access. Deliberately does NOT
# include:
#   - "delete" on transit/keys/certificate-signing (the key can be
#     rotated, never destroyed, by this identity)
#   - any path under sys/ (can't manage mounts, other policies, or auth
#     methods — this identity can't escalate its own privileges)
#   - any other transit key or secrets engine path (no lateral access to
#     unrelated secrets, even other keys in the same transit mount)
path "transit/sign/certificate-signing" {
  capabilities = ["create", "update"]
}

path "transit/verify/certificate-signing" {
  capabilities = ["create", "update"]
}

path "transit/keys/certificate-signing/rotate" {
  capabilities = ["update"]
}

path "transit/keys/certificate-signing" {
  capabilities = ["read"]
}
