package com.medtrack.ui.common

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medtrack.R
import com.medtrack.data.MedicationEntity
import com.medtrack.data.Pharmacy
import com.medtrack.data.orderSearches
import com.medtrack.data.title
import com.medtrack.ui.theme.medColors

/**
 * "Order", then a choice of pharmacy for each name the medication goes by (the name on the pack, then the generic
 * name): opens that pharmacy's search results in its app or the browser. [onOrdered] runs when one is chosen.
 */
@Composable
fun OrderButton(med: MedicationEntity, modifier: Modifier = Modifier, onOrdered: () -> Unit = {}) {
    val context = LocalContext.current
    val searches = remember(med) { med.orderSearches() }
    if (searches.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Order ${med.title}" }) {
            Text("Order")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            searches.forEachIndexed { index, search ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.medColors.foil)
                Text(
                    "${search.label}: ${search.term}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.medColors.inkMuted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).widthIn(max = 280.dp).semantics { heading() },
                )
                Pharmacy.entries.forEach { pharmacy ->
                    DropdownMenuItem(
                        text = { Text(pharmacy.label) },
                        leadingIcon = {
                            Image(painterResource(pharmacy.logo), contentDescription = null, modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)))
                        },
                        modifier = Modifier.semantics { contentDescription = "${pharmacy.label}, search ${search.term}" },
                        onClick = {
                            open = false
                            onOrdered()
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pharmacy.searchUrl(search.term))))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "No browser to open ${pharmacy.label}", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }
            }
        }
    }
}

/** The pharmacy's own app icon, shown beside its name. */
private val Pharmacy.logo: Int
    get() = when (this) {
        Pharmacy.TATA_1MG -> R.drawable.logo_tata1mg
        Pharmacy.PHARMEASY -> R.drawable.logo_pharmeasy
        Pharmacy.NETMEDS -> R.drawable.logo_netmeds
    }
