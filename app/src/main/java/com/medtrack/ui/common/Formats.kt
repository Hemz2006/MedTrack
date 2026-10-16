package com.medtrack.ui.common

import java.time.format.DateTimeFormatter
import java.util.Locale

val TIME: DateTimeFormatter = com.medtrack.schedule.TIME_12
val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
