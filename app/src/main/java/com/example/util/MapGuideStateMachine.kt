package com.example.util

enum class MapGuideStep {
    NONE,
    SPOTLIGHT,
    LEGEND,
    DONE
}

/**
 * Mesin status murni untuk alur panduan membaca peta pada kunjungan pertama Beranda.
 */
object MapGuideStateMachine {
    const val MIN_SPOTLIGHT_WIDTH_DP = 320

    fun initialStep(
        hasSeenGuide: Boolean,
        completedNodeCount: Int,
        hasSpotlightTarget: Boolean,
        screenWidthDp: Int
    ): MapGuideStep {
        if (hasSeenGuide || completedNodeCount > 0) {
            return MapGuideStep.DONE
        }
        return if (hasSpotlightTarget && screenWidthDp >= MIN_SPOTLIGHT_WIDTH_DP) {
            MapGuideStep.SPOTLIGHT
        } else {
            MapGuideStep.LEGEND
        }
    }

    fun nextStep(current: MapGuideStep): MapGuideStep = when (current) {
        MapGuideStep.NONE -> MapGuideStep.SPOTLIGHT
        MapGuideStep.SPOTLIGHT -> MapGuideStep.LEGEND
        MapGuideStep.LEGEND -> MapGuideStep.DONE
        MapGuideStep.DONE -> MapGuideStep.DONE
    }

    fun skipToDone(): MapGuideStep = MapGuideStep.DONE
}
