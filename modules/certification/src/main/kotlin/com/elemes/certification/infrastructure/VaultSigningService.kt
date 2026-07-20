package com.elemes.certification.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.Base64

/**
 * Ch.40 §3 ADR-066: the certificate-signing key lives in Vault's Transit
 * secrets engine (the local, self-hostable stand-in for the AKB's selected
 * cloud-native HSM-backed KMS — see docker-compose.yml), not in this
 * process. Every sign/verify call is a real network call to Vault; the
 * private key material never crosses that boundary in either direction —
 * only a payload in, a signature out. `certificate-signing` is an
 * RSA-2048 asymmetric key; Vault retains every previous key version
 * indefinitely, so a signature made before a [rotate] stays verifiable
 * after it — rotation only changes which version *new* signs use.
 *
 * Authenticates via AppRole (`role_id`/`secret_id`), not a shared root
 * token — the resulting client token is scoped to exactly the
 * `certificate-signing-policy` in `infra/vault/`: sign, verify, rotate,
 * and read on this one key, nothing else. It cannot create or delete keys,
 * enable secrets engines, or touch any other path in Vault — provisioning
 * the transit engine and the key itself is a one-time bootstrap step done
 * with the root token (see README), deliberately kept out of this class
 * and out of this service's own runtime identity entirely.
 */
@Component
class VaultSigningService(
    @Value("\${vault.base-url}") vaultBaseUrl: String,
    @Value("\${vault.role-id}") private val roleId: String,
    @Value("\${vault.secret-id}") private val secretId: String,
) {
    companion object {
        private const val KEY_NAME = "certificate-signing"
    }

    private val restClient = RestClient.builder().baseUrl(vaultBaseUrl).build()

    @Volatile
    private var clientToken: String = login()

    /** AppRole login: exchanges role_id + secret_id for a short-lived client token scoped to certificate-signing-policy. */
    @Suppress("UNCHECKED_CAST")
    private fun login(): String {
        val response = restClient.post().uri("/v1/auth/approle/login")
            .body(mapOf("role_id" to roleId, "secret_id" to secretId))
            .retrieve()
            .body(Map::class.java) as Map<String, Any>
        val auth = response["auth"] as Map<String, Any>
        return auth["client_token"] as String
    }

    /** Every operation goes through this — on a 403 (expired or revoked token), logs in again once and retries, rather than failing a legitimate request over routine token expiry. */
    private fun <T> withToken(op: (token: String) -> T): T = try {
        op(clientToken)
    } catch (ex: RestClientResponseException) {
        if (ex.statusCode.value() == 403) {
            clientToken = login()
            op(clientToken)
        } else {
            throw ex
        }
    }

    fun sign(payload: String): String = withToken { token ->
        transitResponse(token, "/v1/transit/sign/{key}", mapOf("input" to base64(payload))).data("signature") as String
    }

    fun verify(payload: String, signature: String): Boolean = try {
        withToken { token ->
            transitResponse(token, "/v1/transit/verify/{key}", mapOf("input" to base64(payload), "signature" to signature)).data("valid") as Boolean
        }
    } catch (ex: Exception) {
        false
    }

    /** Every key version's PEM-encoded public key, keyed by version number as a string — Ch.26 §6: a third party verifying a signature independently needs the specific historical version that produced it, not just the latest. */
    @Suppress("UNCHECKED_CAST")
    fun publicKeysByVersion(): Map<String, String> {
        val data = keyMetadata()
        val keys = data["keys"] as Map<String, Any>
        return keys.mapValues { (_, versionInfo) -> (versionInfo as Map<*, *>)["public_key"] as String }
    }

    fun latestKeyVersion(): Int = (keyMetadata()["latest_version"] as Number).toInt()

    /** Ch.40 §3: native key rotation — old signatures stay verifiable (see class doc), only new signs move to the new version. */
    fun rotate(): Int = withToken { token ->
        restClient.post().uri("/v1/transit/keys/{key}/rotate", KEY_NAME)
            .header("X-Vault-Token", token)
            .retrieve()
            .toBodilessEntity()
        latestKeyVersion()
    }

    @Suppress("UNCHECKED_CAST")
    private fun keyMetadata(): Map<String, Any> = withToken { token ->
        val response = restClient.get().uri("/v1/transit/keys/{key}", KEY_NAME)
            .header("X-Vault-Token", token)
            .retrieve()
            .body(Map::class.java) as Map<String, Any>
        response["data"] as Map<String, Any>
    }

    @Suppress("UNCHECKED_CAST")
    private fun transitResponse(token: String, uri: String, body: Map<String, String>): TransitResponse {
        val response = restClient.post().uri(uri, KEY_NAME)
            .header("X-Vault-Token", token)
            .body(body)
            .retrieve()
            .body(Map::class.java) as Map<String, Any>
        return TransitResponse(response["data"] as Map<String, Any>)
    }

    private fun base64(payload: String) = Base64.getEncoder().encodeToString(payload.toByteArray())

    private class TransitResponse(private val data: Map<String, Any>) {
        fun data(field: String): Any? = data[field]
    }
}
