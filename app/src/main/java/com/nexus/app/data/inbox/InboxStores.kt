package com.nexus.app.data.inbox

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object InboxInputValidator {
    private val ownerRepoRegex = Regex("^[A-Za-z0-9_.-]{1,100}$")
    private val branchRegex = Regex("^[A-Za-z0-9_./-]{1,100}$")

    fun normalizeAndValidate(config: InboxConfig): Result<InboxConfig> {
        val owner = config.owner.trim()
        val repo = config.repo.trim()
        val branch = config.branch.trim().ifEmpty { InboxConfig.DEFAULT_BRANCH }
        val cleanPath = config.pathPrefix.trim().trim('/')

        if (owner.isEmpty() || !ownerRepoRegex.matches(owner) || owner == "." || owner == "..") {
            return Result.failure(IllegalArgumentException("Informe um usuário/organização do GitHub válido."))
        }
        if (repo.isEmpty() || !ownerRepoRegex.matches(repo) || repo == "." || repo == "..") {
            return Result.failure(IllegalArgumentException("Informe um nome de repositório válido (ex.: NEXUS-INBOX)."))
        }
        if (!branchRegex.matches(branch) || branch.contains("..") || branch.startsWith("/") || branch.endsWith("/")) {
            return Result.failure(IllegalArgumentException("Nome de branch inválido."))
        }
        if (cleanPath.isNotEmpty()) {
            if (cleanPath.contains("..") || cleanPath.contains('?') || cleanPath.contains('#') ||
                cleanPath.contains('\\') || cleanPath.any { it.code < 32 }
            ) {
                return Result.failure(IllegalArgumentException("Caminho de pasta inválido na NEXUS-INBOX."))
            }
            val segments = cleanPath.split('/')
            if (segments.any { it.isEmpty() || it == "." || it == ".." }) {
                return Result.failure(IllegalArgumentException("Caminho de pasta contém segmentos inválidos."))
            }
        }

        return Result.success(
            config.copy(
                owner = owner,
                repo = repo,
                branch = branch,
                pathPrefix = cleanPath
            )
        )
    }

    fun validateTokenInput(rawToken: String): Result<String> {
        val trimmed = rawToken.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("O token de acesso não pode estar vazio."))
        }
        if (trimmed.any { it.isWhitespace() || it.code < 32 }) {
            return Result.failure(IllegalArgumentException("Token inválido: remova espaços ou quebras de linha."))
        }
        return Result.success(trimmed)
    }
}

interface InboxConfigStorage {
    fun loadConfig(): InboxConfig
    fun saveConfig(config: InboxConfig)
    fun getProcessedFileVersionKeys(): Set<String>
    fun addProcessedFileVersionKeys(keys: Set<String>)
    fun clearProcessedFileVersionKeys()
    fun getLastNotifiedFingerprint(): String?
    fun setLastNotifiedFingerprint(fingerprint: String?)
}

class SharedPrefsInboxConfigStorage(context: Context) : InboxConfigStorage {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadConfig(): InboxConfig {
        val lastChecked = prefs.getLong(KEY_LAST_CHECKED_AT, -1L).takeIf { it > 0L }
        val lastImport = prefs.getLong(KEY_LAST_IMPORT_AT, -1L).takeIf { it > 0L }
        return InboxConfig(
            owner = prefs.getString(KEY_OWNER, "").orEmpty(),
            repo = prefs.getString(KEY_REPO, InboxConfig.DEFAULT_REPO_NAME).orEmpty()
                .ifBlank { InboxConfig.DEFAULT_REPO_NAME },
            branch = prefs.getString(KEY_BRANCH, InboxConfig.DEFAULT_BRANCH).orEmpty()
                .ifBlank { InboxConfig.DEFAULT_BRANCH },
            pathPrefix = prefs.getString(KEY_PATH_PREFIX, "").orEmpty(),
            autoCheckEnabled = prefs.getBoolean(KEY_AUTO_CHECK_ENABLED, false),
            lastCheckedAtEpochMs = lastChecked,
            lastImportAtEpochMs = lastImport,
            lastError = prefs.getString(KEY_LAST_ERROR, null)
        )
    }

    override fun saveConfig(config: InboxConfig) {
        prefs.edit()
            .putString(KEY_OWNER, config.owner)
            .putString(KEY_REPO, config.repo)
            .putString(KEY_BRANCH, config.branch)
            .putString(KEY_PATH_PREFIX, config.pathPrefix)
            .putBoolean(KEY_AUTO_CHECK_ENABLED, config.autoCheckEnabled)
            .putLong(KEY_LAST_CHECKED_AT, config.lastCheckedAtEpochMs ?: -1L)
            .putLong(KEY_LAST_IMPORT_AT, config.lastImportAtEpochMs ?: -1L)
            .putString(KEY_LAST_ERROR, config.lastError)
            .apply()
    }

    override fun getProcessedFileVersionKeys(): Set<String> =
        prefs.getStringSet(KEY_PROCESSED_FILE_VERSIONS, emptySet())?.toSet() ?: emptySet()

    override fun addProcessedFileVersionKeys(keys: Set<String>) {
        if (keys.isEmpty()) return
        val merged = (getProcessedFileVersionKeys() + keys)
            .toList()
            .takeLast(MAX_TRACKED_FILE_KEYS)
            .toSet()
        prefs.edit().putStringSet(KEY_PROCESSED_FILE_VERSIONS, merged).apply()
    }

    override fun clearProcessedFileVersionKeys() {
        prefs.edit().remove(KEY_PROCESSED_FILE_VERSIONS).apply()
    }

    override fun getLastNotifiedFingerprint(): String? =
        prefs.getString(KEY_LAST_NOTIFIED_FINGERPRINT, null)

    override fun setLastNotifiedFingerprint(fingerprint: String?) {
        prefs.edit().putString(KEY_LAST_NOTIFIED_FINGERPRINT, fingerprint).apply()
    }

    companion object {
        private const val PREFS_NAME = "nexus_inbox_config_prefs"
        private const val KEY_OWNER = "owner"
        private const val KEY_REPO = "repo"
        private const val KEY_BRANCH = "branch"
        private const val KEY_PATH_PREFIX = "path_prefix"
        private const val KEY_AUTO_CHECK_ENABLED = "auto_check_enabled"
        private const val KEY_LAST_CHECKED_AT = "last_checked_at"
        private const val KEY_LAST_IMPORT_AT = "last_import_at"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_PROCESSED_FILE_VERSIONS = "processed_file_versions"
        private const val KEY_LAST_NOTIFIED_FINGERPRINT = "last_notified_fingerprint"
        private const val MAX_TRACKED_FILE_KEYS = 250
    }
}

interface InboxTokenStore {
    fun hasToken(): Boolean
    fun getToken(): String?
    fun saveToken(token: String)
    fun clearToken()
}

class AndroidKeystoreInboxTokenStore(context: Context) : InboxTokenStore {
    private val prefs = context.applicationContext.getSharedPreferences(SECRET_PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun hasToken(): Boolean = !prefs.getString(KEY_CIPHERTEXT, null).isNullOrBlank()

    @Synchronized
    override fun getToken(): String? {
        val ivEncoded = prefs.getString(KEY_IV, null) ?: return null
        val cipherEncoded = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        return try {
            val secretKey = getOrCreateSecretKey()
            val iv = Base64.getDecoder().decode(ivEncoded)
            val cipherBytes = Base64.getDecoder().decode(cipherEncoded)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            clearToken()
            null
        }
    }

    @Synchronized
    override fun saveToken(token: String) {
        val clean = InboxInputValidator.validateTokenInput(token).getOrThrow()
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        val ivEncoded = Base64.getEncoder().encodeToString(cipher.iv)
        val cipherEncoded = Base64.getEncoder().encodeToString(encrypted)
        prefs.edit()
            .putString(KEY_IV, ivEncoded)
            .putString(KEY_CIPHERTEXT, cipherEncoded)
            .apply()
    }

    @Synchronized
    override fun clearToken() {
        prefs.edit().remove(KEY_IV).remove(KEY_CIPHERTEXT).apply()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val SECRET_PREFS_NAME = "nexus_inbox_secret_prefs"
        private const val KEY_IV = "pat_iv"
        private const val KEY_CIPHERTEXT = "pat_ciphertext"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "nexus_inbox_github_pat_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}
