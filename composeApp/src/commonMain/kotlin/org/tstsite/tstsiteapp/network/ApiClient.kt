package org.tstsite.tstsiteapp.network

import org.tstsite.tstsiteapp.model.SesionRequest
import org.tstsite.tstsiteapp.model.SesionResponse

expect class ApiClient() {

    suspend fun login(pLogin: SesionRequest): SesionResponse

}