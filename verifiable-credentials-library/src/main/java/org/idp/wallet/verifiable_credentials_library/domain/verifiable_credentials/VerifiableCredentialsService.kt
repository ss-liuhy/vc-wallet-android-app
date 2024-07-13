package org.idp.wallet.verifiable_credentials_library.domain.verifiable_credentials

// import id.walt.sdjwt.SDJwt
import com.nimbusds.jose.crypto.ECDSAVerifier
import eu.europa.ec.eudi.sdjwt.SdJwtVerifier
import eu.europa.ec.eudi.sdjwt.asJwtVerifier
import java.util.UUID
import kotlin.js.ExperimentalJsExport
import org.idp.wallet.verifiable_credentials_library.domain.type.oauth.TokenResponse
import org.idp.wallet.verifiable_credentials_library.domain.type.oidc.OidcMetadata
import org.idp.wallet.verifiable_credentials_library.domain.type.vc.CredentialIssuerMetadata
import org.idp.wallet.verifiable_credentials_library.domain.type.vc.CredentialResponse
import org.idp.wallet.verifiable_credentials_library.domain.type.vc.JwtVcConfiguration
import org.idp.wallet.verifiable_credentials_library.util.http.HttpClient
import org.idp.wallet.verifiable_credentials_library.util.jose.JoseUtils
import org.idp.wallet.verifiable_credentials_library.util.json.JsonUtils
import org.json.JSONObject

class VerifiableCredentialsService(
    val registry: VerifiableCredentialRegistry,
    val clientId: String
) {

  @OptIn(ExperimentalJsExport::class)
  suspend fun transform(
      format: String,
      type: String,
      rawVc: String,
      jwks: String
  ): VerifiableCredentialsRecord {
    return when (format) {
      "vc+sd-jwt" -> {
        val publicKey = JoseUtils.toPublicKey(jwks, "J1FwJP87C6-QN_WSIOmJAQc6n5CQ_bZdaFJ5GDnW1Rk")
        val jwtSignatureVerifier = ECDSAVerifier(publicKey).asJwtVerifier()
        val decodedSdJwt = SdJwtVerifier.verifyIssuance(jwtSignatureVerifier, rawVc).getOrThrow()
        val claim = decodedSdJwt.jwt.second
        return VerifiableCredentialsRecord(UUID.randomUUID().toString(), type, format, rawVc, claim)
      }
      "jwt_vc_json" -> {
        val jwt = JoseUtils.parse(rawVc)
        val payload = jwt.payload()
        VerifiableCredentialsRecord(UUID.randomUUID().toString(), type, format, rawVc, payload)
      }
      else -> {
        throw RuntimeException("unsupported format")
      }
    }
  }

  fun getAllCredentials(): Map<String, VerifiableCredentialsRecords> {
    return registry.getAll()
  }

  suspend fun getCredentialOffer(credentialOfferRequest: CredentialOfferRequest): CredentialOffer {
    val credentialOfferUri = credentialOfferRequest.credentialOfferUri()
    credentialOfferUri?.let {
      val response = HttpClient.get(it)
      return CredentialOfferCreator.create(response)
    }
    val credentialOffer = credentialOfferRequest.credentialOffer()
    credentialOffer?.let {
      val json = JSONObject(it)
      return CredentialOfferCreator.create(json)
    }
    throw CredentialOfferRequestException(
        "Credential offer request must contain either credential_offer or credential_offer_uri.")
  }

  suspend fun getCredentialIssuerMetadata(url: String): CredentialIssuerMetadata {
    val response = HttpClient.get(url)
    return JsonUtils.read(response.toString(), CredentialIssuerMetadata::class.java)
  }

  suspend fun getOidcMetadata(url: String): OidcMetadata {
    val response = HttpClient.get(url)
    return JsonUtils.read(response.toString(), OidcMetadata::class.java)
  }

  suspend fun requestTokenOnPreAuthorizedCode(
      url: String,
      preAuthorizationCode: String
  ): TokenResponse {
    val tokenRequest =
        hashMapOf(
            Pair("client_id", clientId),
            Pair("grant_type", "urn:ietf:params:oauth:grant-type:pre-authorized_code"),
            Pair("pre-authorized_code", preAuthorizationCode))
    val tokenRequestHeaders = hashMapOf(Pair("content-type", "application/x-www-form-urlencoded"))
    val response = HttpClient.post(url, headers = tokenRequestHeaders, requestBody = tokenRequest)
    return JsonUtils.read(response.toString(), TokenResponse::class.java)
  }

  suspend fun requestCredential(
      url: String,
      accessToken: String,
      format: String,
      vc: String
  ): CredentialResponse {
    val credentialRequest = hashMapOf(Pair("format", format), Pair("vct", vc))
    val credentialRequestHeader = hashMapOf(Pair("Authorization", "Bearer $accessToken"))
    val response = HttpClient.post(url, credentialRequestHeader, credentialRequest)
    return JsonUtils.read(response.toString(), CredentialResponse::class.java)
  }

  fun registerCredential(
      credentialIssuer: String,
      verifiableCredentialsRecord: VerifiableCredentialsRecord
  ) {
    registry.save(credentialIssuer, verifiableCredentialsRecord)
  }

  suspend fun getJwks(jwksEndpoint: String): String {
    val response = HttpClient.get(jwksEndpoint)
    return response.toString()
  }

  suspend fun getJwksConfiguration(jwtVcIssuerEndpoint: String): JwtVcConfiguration {
    val response = HttpClient.get(jwtVcIssuerEndpoint)
    return JsonUtils.read(response.toString(), JwtVcConfiguration::class.java)
  }
}
