package com.nuvio.tv.ui.appearance

import com.nuvio.tv.domain.model.AppAppearance
import com.nuvio.tv.domain.model.AppearanceContext
import com.nuvio.tv.BuildConfig

object AppearancePolicy {
    // Open only after the B renderer/harness is verified, never for other variants.
    private const val RENDERER_READY = true
    val NOTFLIX_AVAILABLE = laboratoryAvailable(BuildConfig.FLAVOR, BuildConfig.BUILD_TYPE,
        BuildConfig.APPLICATION_ID, RENDERER_READY)

    /** Allows the full lab build and both Notflix release packages. */
    fun laboratoryAvailable(flavor: String, buildType: String, applicationId: String, rendererReady: Boolean): Boolean =
        rendererReady && flavor == "full" && (
            (buildType == "debug" && applicationId == "com.nuviodebug.com") ||
                (buildType == "release" && applicationId in setOf("com.notflix.tv", "com.notflix.custom.tv")))

    fun effective(requested: AppAppearance, available: Boolean = NOTFLIX_AVAILABLE): AppAppearance =
        if (available) requested else AppAppearance.NUVIO
}

data class AppearanceState(
    val context: AppearanceContext? = null,
    val requested: AppAppearance = AppAppearance.NUVIO,
    val effective: AppAppearance = AppAppearance.NUVIO,
    val readFailed: Boolean = false,
    val saveFailed: Boolean = false,
    val selectionGeneration: Long = 0,
    val reduceHomeMotion: Boolean = false,
    val introEnabled: Boolean = false,
    val introSoundEnabled: Boolean = false,
    val introOptionsValid: Boolean = false,
    val previewEnabled: Boolean = false,
    val previewSoundEnabled: Boolean = false,
    val previewOptionsValid: Boolean = false,
    val uiSoundsEnabled: Boolean = true,
    val uiSoundsOptionsValid: Boolean = false,
    val wikipediaEnabled: Boolean = false,
    val wikipediaOptionsValid: Boolean = false
)
