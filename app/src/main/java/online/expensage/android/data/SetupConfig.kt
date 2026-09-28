package online.expensage.android.data

import androidx.core.net.toUri

data class SetupConfig(
  val endpointUrl: String,
  val accessKey: String,
)

data class PendingSetupConfig(
  val config: SetupConfig,
  val rawLink: String,
)

object SetupLinkParser {
  private const val SETUP_PATH = "/android/setup"

  fun parse(rawLink: String): SetupConfig {
    val normalized = rawLink.trim()
    require(normalized.isNotEmpty()) { "Paste your ExpenSage setup link first." }

    val uri = normalized.toUri()
    val scheme = uri.scheme?.lowercase()
    require(scheme == "https" || scheme == "http" || scheme == "expensage") { 
      "The setup link must start with http://, https://, or expensage://." 
    }
    
    val isCustomScheme = scheme == "expensage"
    if (!isCustomScheme) {
      require(uri.path?.trimEnd('/') == SETUP_PATH) { "This is not a valid ExpenSage Android setup link." }
    } else {
      require(uri.host == "setup") { "Custom scheme must use 'setup' as host (expensage://setup)." }
    }

    val endpointUrl = uri.getQueryParameter("endpointUrl")?.trim().orEmpty()
    val accessKey = uri.getQueryParameter("accessKey")?.trim().orEmpty()

    require(endpointUrl.isNotEmpty()) { "This setup link is missing the Shortcut URL." }
    require(accessKey.isNotEmpty()) { "This setup link is missing the access key." }

    validateEndpoint(endpointUrl)

    return SetupConfig(
      endpointUrl = endpointUrl,
      accessKey = accessKey,
    )
  }

  private fun validateEndpoint(endpointUrl: String) {
    val endpointUri = endpointUrl.toUri()
    val endpointScheme = endpointUri.scheme?.lowercase()
    require(endpointScheme == "https" || endpointScheme == "http") {
      "The Shortcut URL must start with http:// or https://."
    }
    require(!endpointUri.host.isNullOrBlank()) { "The Shortcut URL is not valid." }
  }
}
