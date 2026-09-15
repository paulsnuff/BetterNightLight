package io.github.threefreetree.betternightlight.domain

import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

interface DispatcherProvider {
    val io: CoroutineDispatcher
}

class DefaultDispatcherProvider
    @Inject
    constructor() : DispatcherProvider {
        override val io: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO
    }
