package com.vidsave.app.data.remote

import com.vidsave.app.data.model.UpdateMetadata
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface UpdateApiService {

    @GET
    suspend fun fetchUpdateMetadata(
        @Url url: String
    ): Response<UpdateMetadata>
}
