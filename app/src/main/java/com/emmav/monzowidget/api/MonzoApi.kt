package com.emmav.monzowidget.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface MonzoApi {

    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun getAccessToken(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code") authorizationCode: String,
    ): Response<TokenResponse>

    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun refreshAccessToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String
    ): Response<TokenResponse>

    @GET("accounts")
    suspend fun accounts(): Response<AccountsResponse>

    @GET("balance")
    suspend fun balance(@Query("account_id") accountId: String): Response<ApiBalance>

    @GET("pots")
    suspend fun pots(@Query("current_account_id") accountId: String): Response<PotsResponse>
}

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("scope") val scope: String?,
)

@Serializable
data class AccountsResponse(val accounts: List<ApiAccount>)

@Serializable
data class ApiAccount(
    val id: String,
    val closed: Boolean,
    @SerialName("product_type") val productType: String,
    @SerialName("owner_type") val ownerType: String,
    @SerialName("country_code") val countryCode: String,
)

@Serializable
data class ApiBalance(
    val balance: Long,
    val currency: String
)

@Serializable
data class PotsResponse(val pots: List<ApiPot> = emptyList())

@Serializable
data class ApiPot(
    val id: String,
    val name: String,
    val balance: Long,
    val currency: String,
    val deleted: Boolean
)