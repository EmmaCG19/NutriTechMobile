package com.istea.nutritechmobile.model.interfaces

import com.google.android.gms.tasks.Task
import com.istea.nutritechmobile.data.RegistroCorporal
import com.istea.nutritechmobile.data.UserResponse

interface IRegistroCorporalRepository {
    suspend fun addCorporalRegistry(user: String, registro: RegistroCorporal): Task<Void>
    suspend fun updatePacienteInfo(paciente: UserResponse): Task<Void>
    suspend fun getLoggedUser():UserResponse?
    suspend fun updateLoggedUser(user: UserResponse)
    suspend fun logoutUser()
}