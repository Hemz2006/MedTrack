package com.medtrack.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.medtrack.R
import com.medtrack.data.MedForm
import com.medtrack.ui.theme.medColors

@get:DrawableRes
val MedForm.iconRes: Int?
    get() = when (this) {
        MedForm.TABLET -> R.drawable.ic_form_tablet
        MedForm.CAPSULE -> R.drawable.ic_form_capsule
        MedForm.SYRUP -> R.drawable.ic_form_syrup
        MedForm.INHALER -> R.drawable.ic_form_inhaler
        MedForm.ROTACAP -> R.drawable.ic_form_rotacap
        MedForm.NEBULISER -> R.drawable.ic_form_nebuliser
        MedForm.DROPS -> R.drawable.ic_form_drops
        MedForm.INJECTION -> R.drawable.ic_form_injection
        MedForm.CREAM -> R.drawable.ic_form_cream
        MedForm.OTHER -> null
    }

/** A small outline picture of the medicine's form (tablet, inhaler…); nothing when the form is unknown. */
@Composable
fun FormIcon(form: MedForm?, modifier: Modifier = Modifier, size: Dp = 18.dp, tint: Color = MaterialTheme.medColors.inkMuted) {
    val res = form?.iconRes ?: return
    // The form is also said in words next to it, so the picture itself is decorative.
    Icon(painterResource(res), contentDescription = null, tint = tint, modifier = modifier.size(size))
}
