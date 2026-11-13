package com.rogbooster.app

/** Estado global compartido entre la UI y los servicios. */
object GameState {
    @Volatile var isGameRunning: Boolean = false
    @Volatile var currentPackage: String? = null
}
