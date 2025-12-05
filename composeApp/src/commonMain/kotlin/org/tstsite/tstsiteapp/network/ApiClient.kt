package org.tstsite.tstsiteapp.network

import org.tstsite.tstsiteapp.model.*

expect class ApiClient() {
    // ---------- AUTH ----------
    suspend fun login(pLogin: SesionRequest): SesionResponse
    suspend fun validate(token: String): ValidateResponse
    suspend fun profile(token: String): ProfileResponse
    suspend fun logout(token: String): LogoutResponse

    // ---------- USERS ----------
    suspend fun insertUser(token: String, userData: UserData): UserResponse
    suspend fun selectUser(token: String, username: String): UserResponse
    suspend fun updateUser(token: String, username: String, userData: UserData): UserResponse
    suspend fun deleteUser(token: String, username: String): UserResponse
    suspend fun listUsers(token: String): UsersListResponse
    suspend fun searchUsers(token: String, searchParams: UserSearchParams): UsersListResponse

    // ---------- CATALOG ----------
    suspend fun getCatalog(token: String): CatalogResponse
}