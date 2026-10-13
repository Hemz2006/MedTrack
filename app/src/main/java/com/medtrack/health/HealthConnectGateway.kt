package com.medtrack.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.medtrack.schedule.Meal
import com.medtrack.schedule.MealType
import java.time.Instant
import androidx.health.connect.client.records.MealType as HcMealType

enum class HealthStatus { NOT_INSTALLED, NEEDS_UPDATE, NO_BACKGROUND_READ, NOT_CONNECTED, CONNECTED }

class HealthConnectGateway(private val context: Context) {
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
    )

    suspend fun status(): HealthStatus {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> return HealthStatus.NOT_INSTALLED
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> return HealthStatus.NEEDS_UPDATE
        }
        val client = HealthConnectClient.getOrCreate(context)
        val background = client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND)
        if (background != HealthConnectFeatures.FEATURE_STATUS_AVAILABLE) return HealthStatus.NO_BACKGROUND_READ
        val granted = client.permissionController.getGrantedPermissions()
        return if (granted.containsAll(permissions)) HealthStatus.CONNECTED else HealthStatus.NOT_CONNECTED
    }

    suspend fun mealsBetween(from: Instant, to: Instant): List<Meal>? {
        if (status() != HealthStatus.CONNECTED) return null
        val client = HealthConnectClient.getOrCreate(context)
        val response = client.readRecords(ReadRecordsRequest(NutritionRecord::class, TimeRangeFilter.between(from, to)))
        return response.records.map { Meal(it.startTime, it.mealType.toMealType()) }
    }
}

private fun Int.toMealType(): MealType? = when (this) {
    HcMealType.MEAL_TYPE_BREAKFAST -> MealType.BREAKFAST
    HcMealType.MEAL_TYPE_LUNCH -> MealType.LUNCH
    HcMealType.MEAL_TYPE_DINNER -> MealType.DINNER
    else -> null // snack or unknown
}
