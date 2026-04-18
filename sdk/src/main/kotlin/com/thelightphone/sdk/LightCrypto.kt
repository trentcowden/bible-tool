package com.thelightphone.sdk

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object LightCrypto {

    const val LIGHTOS_PACKAGE = "com.lightos"
    private const val KEYSTORE_ALIAS = "com.thelightphone.sdk.eckey"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    /**
     * Returns the Base64-encoded X.509 public key, generating a new key pair on first call.
     */
    fun getPublicKeyBase64(): String {
        ensureKeyPair()
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val publicKey = keyStore.getCertificate(KEYSTORE_ALIAS).publicKey
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Decrypts an ECIES payload using the device-generated private key.
     *
     * Expected format (Base64-encoded):
     *   [2-byte big-endian ephemeral public key length]
     *   [ephemeral public key (X.509/SubjectPublicKeyInfo)]
     *   [12-byte IV]
     *   [AES-GCM ciphertext + 16-byte auth tag]
     */
    fun decrypt(encryptedBase64: String): String {
        val payload = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        var offset = 0

        // Read ephemeral public key
        val ephemeralKeySize = ((payload[offset].toInt() and 0xFF) shl 8) or
                (payload[offset + 1].toInt() and 0xFF)
        offset += 2
        val ephemeralKeyBytes = payload.sliceArray(offset until offset + ephemeralKeySize)
        offset += ephemeralKeySize

        // Read IV
        val ivSize = 12
        val iv = payload.sliceArray(offset until offset + ivSize)
        offset += ivSize

        // Remaining is ciphertext + GCM tag
        val ciphertext = payload.sliceArray(offset until payload.size)

        // Get private key from Keystore
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val privateKey = keyStore.getKey(KEYSTORE_ALIAS, null) as java.security.PrivateKey

        // Reconstruct ephemeral public key
        val keyFactory = java.security.KeyFactory.getInstance("EC")
        val ephemeralPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(ephemeralKeyBytes))

        // ECDH key agreement
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(ephemeralPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        // Derive AES-256 key
        val aesKeyBytes = java.security.MessageDigest.getInstance("SHA-256").digest(sharedSecret)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        // Decrypt with AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun ensureKeyPair() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) return

        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_AGREE_KEY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER).apply {
            initialize(spec)
            generateKeyPair()
        }
    }
}
