package com.example.util

enum class MapGuideStep {
    NONE,
    HEADER_PROFILE,
    RESUME_CARD,
    QUICK_ACTIONS,
    DAILY_SHIFT,
    SPOTLIGHT,
    BOTTOM_NAV,
    LEGEND,
    DONE
}

/**
 * Mesin status murni untuk alur panduan komprehensif seluruh fitur Beranda pada kunjungan pertama.
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
            MapGuideStep.HEADER_PROFILE
        } else {
            MapGuideStep.LEGEND
        }
    }

    fun nextStep(current: MapGuideStep): MapGuideStep = when (current) {
        MapGuideStep.NONE -> MapGuideStep.HEADER_PROFILE
        MapGuideStep.HEADER_PROFILE -> MapGuideStep.RESUME_CARD
        MapGuideStep.RESUME_CARD -> MapGuideStep.QUICK_ACTIONS
        MapGuideStep.QUICK_ACTIONS -> MapGuideStep.DAILY_SHIFT
        MapGuideStep.DAILY_SHIFT -> MapGuideStep.SPOTLIGHT
        MapGuideStep.SPOTLIGHT -> MapGuideStep.BOTTOM_NAV
        MapGuideStep.BOTTOM_NAV -> MapGuideStep.LEGEND
        MapGuideStep.LEGEND -> MapGuideStep.DONE
        MapGuideStep.DONE -> MapGuideStep.DONE
    }

    fun skipToDone(): MapGuideStep = MapGuideStep.DONE
}
