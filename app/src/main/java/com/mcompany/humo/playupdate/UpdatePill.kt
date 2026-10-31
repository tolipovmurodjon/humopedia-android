package com.mcompany.humo.playupdate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcompany.humo.ui.glass.Glass

/** The quiet pill: shown only while Play has an update on offer, and it does nothing until it is tapped. */
@Composable
fun UpdatePill(model: PlayUpdateModel, modifier: Modifier = Modifier) {
    val text = model.pillText ?: return
    Surface(
        modifier = modifier.clickable(onClick = model::onPillTapped),
        shape = RoundedCornerShape(50),
        color = Glass.Primary,
    ) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
