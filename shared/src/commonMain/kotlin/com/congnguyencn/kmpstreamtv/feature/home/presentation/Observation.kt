package com.congnguyencn.kmpstreamtv.feature.home.presentation

import kotlinx.coroutines.Job

/** ObjC/Swift-friendly cancellation handle for a StateFlow collection. */
class Observation internal constructor(
    private val job: Job,
) {
    fun cancel() = job.cancel()
}
