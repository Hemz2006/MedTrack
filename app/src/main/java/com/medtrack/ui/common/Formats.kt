package com.medtrack.ui.common

import java.time.format.DateTimeFormatter
import java.util.Locale

val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val DAY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH)
val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
