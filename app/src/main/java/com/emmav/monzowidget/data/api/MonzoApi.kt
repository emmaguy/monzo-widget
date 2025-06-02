package com.emmav.monzowidget.data.api

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
