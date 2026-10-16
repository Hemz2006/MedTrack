package com.medtrack.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class StepsTest {
    private val start = LocalDate.parse("2026-10-13")
    private fun d(s: String) = LocalDate.parse(s)
    private val sixTimes = ScheduleRule.DailyTimes(spreadTimes(6))
    private val fourTimes = ScheduleRule.DailyTimes(spreadTimes(4))

    /** 6 times a day for 14 days, then 4 times a day until stopped. */
    private val taper = listOf(Schedule(sixTimes, start).copy(dose = "2 puffs")).withAddedStep()
        .let { it.withStep(1, it[1].copy(rules = listOf(fourTimes))) }

    @Test fun addingAStepEndsTheLastAfterTwoWeeksAndStartsTheNextTheDayAfter() {
        assertEquals(d("2026-10-26"), taper[0].endDate)
        assertEquals(d("2026-10-27"), taper[1].startDate)
        assertNull(taper[1].endDate)
        assertEquals("2 puffs", taper[1].dose) // copied, ready to change
    }

    @Test fun changingALengthMovesTheLaterSteps() {
        val three = taper.withStepLength(1, 7).withAddedStep()
        val longer = three.withStepLength(0, 21)
        assertEquals(d("2026-11-02"), longer[0].endDate)
        assertEquals(d("2026-11-03") to d("2026-11-09"), longer[1].startDate to longer[1].endDate)
        assertEquals(d("2026-11-10"), longer[2].startDate)
    }

    @Test fun movingTheStartMovesEveryStepAndIntervalAnchors() {
        val interval = listOf(Schedule(ScheduleRule.EveryNHours(6, start.atTime(6, 0)), start, start.plusDays(6)), Schedule(fourTimes, start.plusDays(7)))
        val moved = interval.withFirstStart(d("2026-10-20"))
        assertEquals(LocalDateTime.parse("2026-10-20T06:00"), (moved[0].rules.single() as ScheduleRule.EveryNHours).anchor)
        assertEquals(d("2026-10-26"), moved[0].endDate)
        assertEquals(d("2026-10-27"), moved[1].startDate)
    }

    @Test fun removingAStepClosesTheGap() {
        val three = taper.withStepLength(1, 7).withAddedStep()
        val withoutMiddle = three.withoutStep(1)
        assertEquals(d("2026-10-27"), withoutMiddle[1].startDate)
        val withoutFirst = three.withoutStep(0)
        assertEquals(start, withoutFirst[0].startDate)
        assertEquals(7, withoutFirst[0].lengthDays)
        assertEquals(taper.take(1), taper.take(1).withoutStep(0)) // the only step stays
    }

    @Test fun stepOnFindsTheRunningStep() {
        assertEquals(taper[0], taper.stepOn(d("2026-10-26")))
        assertEquals(taper[1], taper.stepOn(d("2026-10-27")))
        assertNull(taper.stepOn(d("2026-10-12")))
    }

    @Test fun validationNeedsBackToBackSteps() {
        assertNull(taper.stepsValidationError())
        assertEquals("Dose 1: set for how many days", listOf(taper[0].copy(endDate = null), taper[1]).stepsValidationError())
        assertEquals("Dose 2 starts before dose 1 ends", listOf(taper[0], taper[1].copy(startDate = start)).stepsValidationError())
        assertEquals("Dose 2: Add at least one time", taper.withStep(1, taper[1].copy(rules = listOf(ScheduleRule.DailyTimes(emptyList())))).stepsValidationError())
        assertEquals("Add at least one schedule", emptyList<Schedule>().stepsValidationError())
    }

    @Test fun describesStepsInWords() {
        assertEquals(
            "13 Oct – 26 Oct: 2 puffs, Daily at 8:00 AM, 11:00 AM, 1:30 PM, 4:30 PM, 7:00 PM, 10:00 PM\n" +
                "From 27 Oct: 2 puffs, Daily at 8:00 AM, 12:30 PM, 5:30 PM, 10:00 PM",
            taper.describeSteps(),
        )
        assertEquals("Dose 2 of 2: 2 puffs, Daily at 8:00 AM, 12:30 PM, 5:30 PM, 10:00 PM", taper.describeOn(d("2026-10-30")))
        assertEquals("Dose 1 of 2: 2 puffs, Daily at 8:00 AM, 11:00 AM, 1:30 PM, 4:30 PM, 7:00 PM, 10:00 PM, until 26 Oct", taper.describeOn(d("2026-10-01")))
    }

    @Test fun spreadTimesCoverEightToTenToTheHalfHour() {
        assertEquals(listOf(LocalTime.of(8, 0)), spreadTimes(1))
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(22, 0)), spreadTimes(2))
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(15, 0), LocalTime.of(22, 0)), spreadTimes(3))
        assertEquals(6, spreadTimes(6).size)
    }
}
