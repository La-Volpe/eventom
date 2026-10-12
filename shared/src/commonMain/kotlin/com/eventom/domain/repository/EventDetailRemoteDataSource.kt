package com.eventom.domain.repository

import com.eventom.domain.model.EventDetail

/**
 * One event's detail payload (M4 renders it). Domain-side contract (F1 /
 * plan T11): the Ktor implementation stays in `com.eventom.data.remote`.
 * Separate from [EventRepository] so the list source stays a pure list
 * contract.
 */
interface EventDetailRemoteDataSource {
    suspend fun fetchEventDetail(id: Long): EventDetail
}