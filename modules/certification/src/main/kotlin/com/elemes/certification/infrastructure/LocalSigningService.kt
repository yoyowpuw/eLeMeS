package com.elemes.certification.infrastructure

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Ch.40 §3 selected a cloud, HSM-backed KMS for the certificate-signing key
 * in production (ADR-066). This is the explicit local-dev stand-in: a
 * locally generated, file-persisted RSA keypair — not HSM-protected, not
 * rotated, not access-audited. Persisted to `.local-kms/` (gitignored) so
 * restarts don't invalidate previously-issued signatures during local
 * development; still not something to point real data at.
 */
@Component
class LocalSigningService {

    private val keyDir = Path.of(".local-kms")
    private val privateKeyFile = keyDir.resolve("certification-signing-key.pem")
    private val publicKeyFile = keyDir.resolve("certification-public-key.pem")

    private val privateKey: PrivateKey
    val publicKeyBase64: String

    init {
        Files.createDirectories(keyDir)
        if (Files.exists(privateKeyFile) && Files.exists(publicKeyFile)) {
            publicKeyBase64 = Files.readString(publicKeyFile).trim()
            privateKey = loadPrivateKey()
        } else {
            val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            privateKey = keyPair.private
            publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)
            Files.writeString(privateKeyFile, Base64.getEncoder().encodeToString(keyPair.private.encoded))
            Files.writeString(publicKeyFile, publicKeyBase64)
        }
    }

    fun sign(payload: String): String {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(payload.toByteArray())
        return Base64.getEncoder().encodeToString(signature.sign())
    }

    fun verify(payload: String, signatureBase64: String): Boolean {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initVerify(loadPublicKey())
        signature.update(payload.toByteArray())
        return signature.verify(Base64.getDecoder().decode(signatureBase64))
    }

    private fun loadPrivateKey(): PrivateKey {
        val bytes = Base64.getDecoder().decode(Files.readString(privateKeyFile).trim())
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes))
    }

    private fun loadPublicKey(): PublicKey {
        val bytes = Base64.getDecoder().decode(publicKeyBase64)
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(bytes))
    }
}
