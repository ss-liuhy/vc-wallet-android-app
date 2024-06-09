package org.idp.wallet.verifiable_credentials_library.domain.type.oidc

import android.net.Uri

data class OpenIdConnectRequest(
    val url: String,
    val clientId: String,
    val scope: String,
    val redirectUri: String,
    val responseType: String = "code",
    val state: String? = null,
    val nonce: String? = null,
    val codeChallenge: String? = null,
    val codeChallengeMethod: CodeChallengeMethod? = null,
) {

  fun authenticationRequestUri(): String {
    val builder = Uri.parse(url).buildUpon()
    builder.appendQueryParameter("client_id", clientId)
    builder.appendQueryParameter("scope", scope)
    builder.appendQueryParameter("response_type", responseType)
    state?.let { builder.appendQueryParameter("state", it) }
    nonce?.let { builder.appendQueryParameter("nonce", it) }
    codeChallenge?.let { builder.appendQueryParameter("code_challenge", it) }
    codeChallengeMethod?.let {
      builder.appendQueryParameter("code_challenge_method", codeChallengeMethod.name)
    }
    return builder.build().toString()
  }
}

enum class CodeChallengeMethod {
  plain,
  s256
}
