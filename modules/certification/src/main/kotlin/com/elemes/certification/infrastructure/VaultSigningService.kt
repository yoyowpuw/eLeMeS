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
 */
@Component
class VaultSigningService(
    @Value("\${vault.base-url}") vaultBaseUrl: String,
    @Value("\${vault.token}") vaultToken: String,
) {
    companion object {
        private const val KEY_NAME = "certificate-signing"
    }

    private val restClient = RestClient.builder()
        .baseUrl(vaultBaseUrl)
        .defaultHeader("X-Vault-Token", vaultToken)
        .build()

    init {
        ensureTransitEngineEnabled()
        ensureSigningKeyExists()
    }

    /** Idempotent — Vault 400s if the mount already exists, which is expected on every restart after the first. */
    private fun ensureTransitEngineEnabled() {
        try {
            restClient.post().uri("/v1/sys/mounts/transit")
                .body(mapOf("type" to "transit"))
                .retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientResponseException) {
            if (ex.statusCode.value() != 400) throw ex
        }
    }

    /** Idempotent — Vault 400s if a key with this name already exists, which is expected on every restart after the first. */
    private fun ensureSigningKeyExists() {
        try {
            restClient.post().uri("/v1/transit/keys/{key}", KEY_NAME)
                .body(mapOf("type" to "rsa-2048"))
                .retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientResponseException) {
            if (ex.statusCode.value() != 400) throw ex
        }
    }

    /** Returns Vault's own signature format (`vault:v<N>:<base64>`) — the version is embedded, needed both to verify and to know which historical public key a signature corresponds to. */
    fun sign(payload: String): String {
        val response = transitResponse("/v1/transit/sign/{key}", KEY_NAME, mapOf("input" to base64(payload)))
        return response.data("signature") as String
    }

    fun verify(payload: String, signature: String): Boolean = try {
        val response = transitResponse("/v1/transit/verify/{key}", KEY_NAME, mapOf("input" to base64(payload), "signature" to signature))
        response.data("valid") as Boolean
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
    fun rotate(): Int {
        restClient.post().uri("/v1/transit/keys/{key}/rotate", KEY_NAME).retrieve().toBodilessEntity()
        return latestKeyVersion()
    }

    @Suppress("UNCHECKED_CAST")
    private fun keyMetadata(): Map<String, Any> {
        val response = restClient.get().uri("/v1/transit/keys/{key}", KEY_NAME).retrieve().body(Map::class.java) as Map<String, Any>
        return response["data"] as Map<String, Any>
    }

    @Suppress("UNCHECKED_CAST")
    private fun transitResponse(uri: String, key: String, body: Map<String, String>): TransitResponse {
        val response = restClient.post().uri(uri, key).body(body).retrieve().body(Map::class.java) as Map<String, Any>
        return TransitResponse(response["data"] as Map<String, Any>)
    }

    private fun base64(payload: String) = Base64.getEncoder().encodeToString(payload.toByteArray())

    private class TransitResponse(private val data: Map<String, Any>) {
        fun data(field: String): Any? = data[field]
    }
}
