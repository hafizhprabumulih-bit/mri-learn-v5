package com.example

import com.example.util.MapGuideStateMachine
import com.example.util.MapGuideStep
import org.junit.Assert.assertEquals
import org.junit.Test

class MapGuideStateMachineTest {

    @Test
    fun firstVisitWithTargetAndWideScreenStartsAtHeaderProfileAndCyclesThroughAllFeatures() {
        var step = MapGuideStateMachine.initialStep(
            hasSeenGuide = false,
            completedNodeCount = 0,
            hasSpotlightTarget = true,
            screenWidthDp = 360
        )
        assertEquals(MapGuideStep.HEADER_PROFILE, step)
        step = MapGuideStateMachine.nextStep(step)
        assertEquals(MapGuideStep.RESUME_CARD, step)
        step = MapGuideStateMachine.nextStep(step)
        assertEquals(MapGuideStep.QUICK_ACTIONS, step)
        step = MapGuideStateMachine.nextStep(step)
        assertEquals(MapGuideStep.DAILY_SHIFT, step)
        step = MapGuideStateMachine.nextStep(step)
        assertEquals(MapGuideStep.SPOTLIGHT, step)
        step = MapGuideStateMachine.nextStep(step)
        assertEquals(MapGuideStep.BOTTOM_NAV, step)
        step = MapGuideStateMachine.nextStep(step)
        assertEquals(MapGuideStep.LEGEND, step)
        step = MapGuideStateMachine.nextStep(step)
        assertEquals(MapGuideStep.DONE, step)
    }

    @Test
    fun returningUserWithProgressSkipsGuideDirectlyToDone() {
        val step = MapGuideStateMachine.initialStep(
            hasSeenGuide = false,
            completedNodeCount = 3,
            hasSpotlightTarget = true,
            screenWidthDp = 360
        )
        assertEquals(MapGuideStep.DONE, step)
    }

    @Test
    fun alreadySeenUserAlwaysStartsAtDone() {
        val step = MapGuideStateMachine.initialStep(
            hasSeenGuide = true,
            completedNodeCount = 0,
            hasSpotlightTarget = true,
            screenWidthDp = 400
        )
        assertEquals(MapGuideStep.DONE, step)
    }

    @Test
    fun narrowScreenOrMissingTargetSkipsSpotlightToLegend() {
        val narrow = MapGuideStateMachine.initialStep(
            hasSeenGuide = false,
            completedNodeCount = 0,
            hasSpotlightTarget = true,
            screenWidthDp = 280
        )
        assertEquals(MapGuideStep.LEGEND, narrow)

        val missingTarget = MapGuideStateMachine.initialStep(
            hasSeenGuide = false,
            completedNodeCount = 0,
            hasSpotlightTarget = false,
            screenWidthDp = 360
        )
        assertEquals(MapGuideStep.LEGEND, missingTarget)
    }

    @Test
    fun skipTransitionsImmediatelyToDone() {
        assertEquals(MapGuideStep.DONE, MapGuideStateMachine.skipToDone())
    }
}
