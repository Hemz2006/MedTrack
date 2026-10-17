package com.medtrack.data

import com.medtrack.schedule.Schedule
import com.medtrack.schedule.ScheduleRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestockTest {
    private val today = LocalDate.parse("2026-10-17")
    private val daily = ScheduleRule.DailyTimes(listOf(LocalTime.of(8, 0)))

    private fun med(id: Long, end: LocalDate?, active: Boolean = true) = MedicationEntity(
        id = id, genericName = "Med $id", active = active, createdAt = Instant.EPOCH,
        steps = listOf(Schedule(daily, today.minusDays(10), end)),
    )

    @Test fun asksAboutTheCourseEndingSoonest() {
        val meds = listOf(med(1, today.plusDays(3)), med(2, today.plusDays(1)), med(3, today.plusDays(4)))
        assertEquals(2L, restockDue(meds, today, emptySet())?.id)
    }

    @Test fun skipsOngoingPausedEndedAndAlreadyAsked() {
        val asked = med(4, today.plusDays(2))
        val meds = listOf(med(1, null), med(2, today.plusDays(1), active = false), med(3, today.minusDays(1)), asked)
        assertNull(restockDue(meds, today, setOf(restockKey(asked, today.plusDays(2)))))
    }

    @Test fun anEndingTodayCountsAndANewEndDateAsksAgain() {
        assertEquals(1L, restockDue(listOf(med(1, today)), today, emptySet())?.id)
        val moved = med(1, today.plusDays(2))
        assertEquals(1L, restockDue(listOf(moved), today, setOf("1@$today"))?.id)
    }

    @Test fun usesTheLastDosesEnd() {
        val tapered = med(1, null).copy(steps = listOf(Schedule(daily, today.minusDays(10), today.minusDays(1)), Schedule(daily, today, today.plusDays(2))))
        assertEquals(today.plusDays(2), tapered.courseEnd)
        assertEquals(1L, restockDue(listOf(tapered), today, emptySet())?.id)
    }
}
