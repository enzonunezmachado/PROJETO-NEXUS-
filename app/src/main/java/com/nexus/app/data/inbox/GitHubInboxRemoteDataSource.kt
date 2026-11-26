package com.nexus.app.data.inbox

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

interface InboxRemoteDataSource {
    suspend fun testConnection(config: InboxConfig, token: String): InboxConnectionTestResult
    suspend fun listInboxJsonFiles(config: InboxConfig, token: String): Result<List<InboxRemoteFile>>
    suspend fun downloadFileRawContent(config: InboxConfig, file: InboxRemoteFile, token: String): Result<String>
}

class GitHubInboxRemoteDataSource(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 12_000
) : InboxRemoteDataSource {

    private val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    private data class GitHubContentItem(
        val type: String = "",
        val name: String = "",
        val path: String = "",
        val sha: String = "",
        val size: Long = 0L,
        @SerialName("download_url") val downloadUrl: String? = null
    )

    override suspend fun testConnection(config: InboxConfig, token: String): InboxConnectionTestResult {
        val validConfig = InboxInputValidator.normalizeAndValidate(config).getOrElse { err ->
            return InboxConnectionTestResult.Failure(err.message ?: "Configuração inválida.")
        }
        val cleanToken = InboxInputValidator.validateTokenInput(token).getOrElse { err ->
            return InboxConnectionTestResult.Failure(err.message ?: "Token inválido.")
        }

        val repoUrl = buildRepoMetadataUrl(validConfig)
        val repoResp = executeGetWithSingleRetry(
            url = repoUrl,
            token = cleanToken,
            acceptHeader = "application/vnd.github+json"
        ).getOrElse { err ->
            return InboxConnectionTestResult.Failure(mapNetworkError(err))
        }
        if (repoResp.statusCode !in 200..299) {
            return InboxConnectionTestResult.Failure(mapHttpError(repoResp.statusCode, validConfig))
        }

        val filesResult = listInboxJsonFiles(validConfig, cleanToken)
        val files = filesResult.getOrElse { err ->
            return InboxConnectionTestResult.Failure(err.message ?: "Não foi possível listar a pasta NEXUS-INBOX.")
        }

        return InboxConnectionTestResult.Success(
            repoFullName = "${validConfig.owner}/${validConfig.repo}",
            branch = validConfig.branch,
            jsonFilesFound = files.size
        )
    }

    override suspend fun listInboxJsonFiles(config: InboxConfig, token: String): Result<List<InboxRemoteFile>> {
        val validConfig = InboxInputValidator.normalizeAndValidate(config).getOrElse {
            return Result.failure(it)
        }
        val cleanToken = InboxInputValidator.validateTokenInput(token).getOrElse {
            return Result.failure(it)
        }

        val contentsUrl = buildContentsUrl(validConfig, validConfig.pathPrefix)
        val resp = executeGetWithSingleRetry(
            url = contentsUrl,
            token = cleanToken,
            acceptHeader = "application/vnd.github+json"
        ).getOrElse { err ->
            return Result.failure(IllegalStateException(mapNetworkError(err)))
        }

        if (resp.statusCode == 404 && validConfig.pathPrefix.isBlank()) {
            return Result.success(emptyList())
        }
        if (resp.statusCode !in 200..299) {
            return Result.failure(IllegalStateException(mapHttpError(resp.statusCode, validConfig)))
        }

        val trimmed = resp.body.trim()
        val parsedItems: List<GitHubContentItem> = try {
            if (trimmed.startsWith("[")) {
                lenientJson.decodeFromString<List<GitHubContentItem>>(trimmed)
            } else {
                listOf(lenientJson.decodeFromString<GitHubContentItem>(trimmed))
            }
        } catch (ex: Exception) {
            return Result.failure(IllegalStateException("Resposta inesperada da API do GitHub: ${ex.message}"))
        }

        val jsonFiles = parsedItems
            .asSequence()
            .filter { it.type.equals("file", ignoreCase = true) }
            .filter { it.name.endsWith(".json", ignoreCase = true) }
            .filter { it.size in 1L..MAX_JSON_FILE_BYTES }
            .filter { isSafeRemotePath(it.path) }
            .map {
                InboxRemoteFile(
                    name = it.name,
                    path = it.path,
                    sha = it.sha,
                    sizeBytes = it.size
                )
            }
            .sortedBy { it.path }
            .toList()

        return Result.success(jsonFiles)
    }

    override suspend fun downloadFileRawContent(
        config: InboxConfig,
        file: InboxRemoteFile,
        token: String
    ): Result<String> {
        val validConfig = InboxInputValidator.normalizeAndValidate(config).getOrElse {
            return Result.failure(it)
        }
        val cleanToken = InboxInputValidator.validateTokenInput(token).getOrElse {
            return Result.failure(it)
        }
        if (!isSafeRemotePath(file.path)) {
            return Result.failure(IllegalArgumentException("Caminho de arquivo remoto inválido: ${file.name}"))
        }
        if (file.sizeBytes > MAX_JSON_FILE_BYTES) {
            return Result.failure(IllegalStateException("Arquivo '${file.name}' excede o limite de 2 MB."))
        }

        val fileUrl = buildContentsUrl(validConfig, file.path)
        val resp = executeGetWithSingleRetry(
            url = fileUrl,
            token = cleanToken,
            acceptHeader = "application/vnd.github.raw+json"
        ).getOrElse { err ->
            return Result.failure(IllegalStateException(mapNetworkError(err)))
        }

        if (resp.statusCode !in 200..299) {
            return Result.failure(IllegalStateException(mapHttpError(resp.statusCode, validConfig)))
        }

        if (resp.body.toByteArray(Charsets.UTF_8).size > MAX_JSON_FILE_BYTES) {
            return Result.failure(IllegalStateException("Conteúdo de '${file.name}' excede o limite de 2 MB."))
        }
        return Result.success(resp.body)
    }

    private data class HttpResp(val statusCode: Int, val body: String)

    private suspend fun executeGetWithSingleRetry(
        url: String,
        token: String,
        acceptHeader: String
    ): Result<HttpResp> {
        val first = executeGetOnce(url, token, acceptHeader)
        val shouldRetry = first.isFailure || (first.getOrNull()?.statusCode in 500..599)
        return if (shouldRetry) {
            executeGetOnce(url, token, acceptHeader)
        } else {
            first
        }
    }

    private suspend fun executeGetOnce(
        urlString: String,
        token: String,
        acceptHeader: String
    ): Result<HttpResp> = withContext(Dispatchers.IO) {
        runCatching {
            val parsedUrl = URL(urlString)
            require(parsedUrl.protocol.equals("https", ignoreCase = true)) {
                "Somente HTTPS é permitido."
            }
            require(parsedUrl.host.equals(GITHUB_API_HOST, ignoreCase = true)) {
                "Host remoto não autorizado."
            }

            val connection = (parsedUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("Accept", acceptHeader)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-GitHub-Api-Version", GITHUB_API_VERSION)
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try {
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                    val chars = CharArray(8192)
                    val sb = StringBuilder()
                    var total = 0
                    while (true) {
                        val read = reader.read(chars)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_JSON_FILE_BYTES) {
                            throw IOException("Resposta excedeu limite de tamanho permitido.")
                        }
                        sb.append(chars, 0, read)
                    }
                    sb.toString()
                }.orEmpty()
                HttpResp(statusCode = code, body = body)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun buildRepoMetadataUrl(config: InboxConfig): String =
        "https://$GITHUB_API_HOST/repos/${encodeSegment(config.owner)}/${encodeSegment(config.repo)}"

    private fun buildContentsUrl(config: InboxConfig, path: String): String {
        val cleanPath = path.trim().trim('/')
        val encodedPath = if (cleanPath.isEmpty()) {
            ""
        } else {
            "/" + cleanPath.split('/').joinToString("/") { encodeSegment(it) }
        }
        val encodedBranch = URLEncoder.encode(config.branch, "UTF-8").replace("+", "%20")
        return "https://$GITHUB_API_HOST/repos/${encodeSegment(config.owner)}/${encodeSegment(config.repo)}/contents$encodedPath?ref=$encodedBranch"
    }

    private fun encodeSegment(segment: String): String =
        URLEncoder.encode(segment, "UTF-8").replace("+", "%20")

    private fun isSafeRemotePath(path: String): Boolean {
        val clean = path.trim().trim('/')
        if (clean.isEmpty() || clean.contains("..") || clean.contains('\\')) return false
        return clean.split('/').none { it.isEmpty() || it == "." || it == ".." }
    }

    private fun mapHttpError(statusCode: Int, config: InboxConfig): String = when (statusCode) {
        401 -> "Token do GitHub inválido ou expirado. Atualize o token somente leitura."
        403 -> "Acesso negado ao repositório '${config.owner}/${config.repo}' ou limite da API atingido."
        404 -> "Repositório '${config.owner}/${config.repo}', branch '${config.branch}' ou pasta não encontrada."
        429 -> "Limite temporário de requisições no GitHub atingido. Tente novamente em alguns minutos."
        in 500..599 -> "Serviço do GitHub indisponível temporariamente (HTTP $statusCode)."
        else -> "Falha ao consultar NEXUS-INBOX (HTTP $statusCode)."
    }

    private fun mapNetworkError(err: Throwable): String = when (err) {
        is SocketTimeoutException -> "Tempo esgotado ao conectar com o GitHub. Verifique sua conexão."
        is IOException -> "Sem conexão ou falha de rede ao acessar a NEXUS-INBOX."
        else -> err.message ?: "Falha de comunicação com a NEXUS-INBOX."
    }

    companion object {
        private const val GITHUB_API_HOST = "api.github.com"
        private const val GITHUB_API_VERSION = "2022-11-28"
        private const val USER_AGENT = "NEXUS-Android/1.0"
        private const val MAX_JSON_FILE_BYTES = 2 * 1024 * 1024
    }
}
