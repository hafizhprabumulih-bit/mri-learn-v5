package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharAccentMint
import com.example.ui.theme.CharActionOrange
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger

/**
 * Menggambar ikon vektor datar (flat vector) berbingkai navy gelap (#0B1B32)
 * selaras dengan gaya visual `mri_icon_bersih.png` dan `bg_simulation_center.png`.
 */
internal fun DrawScope.drawMriObjectVector(
    assetKey: MriGlobalAssetKey,
    accent: Color
) {
    val w = size.width
    val h = size.height
    val outline = Color(0xFF0B1B32)
    val strokeW = (minOf(w, h) * 0.06f).coerceIn(2f, 5.5f)
    val steelLight = Color(0xFF93C5FD)
    val steelSlate = Color(0xFFCBD5E1)

    when (assetKey) {
        MriGlobalAssetKey.TABUNG_O2 -> {
            drawRoundRect(RoseDanger, Offset(w * 0.33f, h * 0.24f), Size(w * 0.34f, h * 0.64f), CornerRadius(w * 0.14f, w * 0.14f))
            drawRoundRect(outline, Offset(w * 0.33f, h * 0.24f), Size(w * 0.34f, h * 0.64f), CornerRadius(w * 0.14f, w * 0.14f), style = Stroke(strokeW))
            drawRoundRect(steelSlate, Offset(w * 0.42f, h * 0.11f), Size(w * 0.16f, h * 0.15f), CornerRadius(w * 0.04f, w * 0.04f))
            drawRoundRect(outline, Offset(w * 0.42f, h * 0.11f), Size(w * 0.16f, h * 0.15f), CornerRadius(w * 0.04f, w * 0.04f), style = Stroke(strokeW * 0.85f))
            drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.42f, h * 0.38f), Offset(w * 0.42f, h * 0.74f), strokeW * 0.8f, StrokeCap.Round)
        }
        MriGlobalAssetKey.GUNTING_BAJA -> {
            drawLine(steelSlate, Offset(w * 0.34f, h * 0.64f), Offset(w * 0.76f, h * 0.18f), strokeW * 1.4f, StrokeCap.Round)
            drawLine(steelLight, Offset(w * 0.66f, h * 0.64f), Offset(w * 0.24f, h * 0.18f), strokeW * 1.4f, StrokeCap.Round)
            drawCircle(RoseDanger, radius = w * 0.13f, center = Offset(w * 0.30f, h * 0.73f), style = Stroke(strokeW * 1.1f))
            drawCircle(RoseDanger, radius = w * 0.13f, center = Offset(w * 0.70f, h * 0.73f), style = Stroke(strokeW * 1.1f))
            drawCircle(outline, radius = w * 0.055f, center = Offset(w * 0.50f, h * 0.46f))
        }
        MriGlobalAssetKey.KLIP_LOGAM -> {
            val clipPath = Path().apply {
                moveTo(w * 0.34f, h * 0.72f)
                lineTo(w * 0.34f, h * 0.26f)
                lineTo(w * 0.66f, h * 0.26f)
                lineTo(w * 0.66f, h * 0.76f)
                lineTo(w * 0.44f, h * 0.76f)
                lineTo(w * 0.44f, h * 0.40f)
                lineTo(w * 0.56f, h * 0.40f)
            }
            drawPath(clipPath, color = steelSlate, style = Stroke(strokeW * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(RoseDanger, radius = w * 0.07f, center = Offset(w * 0.56f, h * 0.40f))
        }
        MriGlobalAssetKey.PACEMAKER -> {
            drawRoundRect(steelSlate, Offset(w * 0.22f, h * 0.28f), Size(w * 0.56f, h * 0.52f), CornerRadius(w * 0.20f, w * 0.20f))
            drawRoundRect(outline, Offset(w * 0.22f, h * 0.28f), Size(w * 0.56f, h * 0.52f), CornerRadius(w * 0.20f, w * 0.20f), style = Stroke(strokeW))
            drawLine(RoseDanger, Offset(w * 0.50f, h * 0.28f), Offset(w * 0.64f, h * 0.14f), strokeW, StrokeCap.Round)
            val ecg = Path().apply {
                moveTo(w * 0.30f, h * 0.54f)
                lineTo(w * 0.41f, h * 0.54f)
                lineTo(w * 0.48f, h * 0.39f)
                lineTo(w * 0.54f, h * 0.66f)
                lineTo(w * 0.60f, h * 0.54f)
                lineTo(w * 0.70f, h * 0.54f)
            }
            drawPath(ecg, color = RoseDanger, style = Stroke(strokeW * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        MriGlobalAssetKey.KUNCI_PAS -> {
            drawLine(steelSlate, Offset(w * 0.24f, h * 0.76f), Offset(w * 0.72f, h * 0.28f), strokeW * 2.0f, StrokeCap.Round)
            drawCircle(steelLight, radius = w * 0.16f, center = Offset(w * 0.72f, h * 0.28f))
            drawCircle(outline, radius = w * 0.07f, center = Offset(w * 0.76f, h * 0.24f))
            drawCircle(RoseDanger, radius = w * 0.08f, center = Offset(w * 0.26f, h * 0.74f))
        }
        MriGlobalAssetKey.JAM_MEKANIK -> {
            drawRoundRect(CharActionOrange, Offset(w * 0.38f, h * 0.10f), Size(w * 0.24f, h * 0.80f), CornerRadius(w * 0.08f, w * 0.08f))
            drawRoundRect(outline, Offset(w * 0.38f, h * 0.10f), Size(w * 0.24f, h * 0.80f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(strokeW * 0.8f))
            drawCircle(steelSlate, radius = w * 0.26f, center = Offset(w * 0.50f, h * 0.50f))
            drawCircle(outline, radius = w * 0.26f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(strokeW))
            drawLine(outline, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.50f, h * 0.34f), strokeW * 0.8f, StrokeCap.Round)
            drawLine(RoseDanger, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.62f, h * 0.56f), strokeW * 0.8f, StrokeCap.Round)
        }
        MriGlobalAssetKey.KOIN_KUNCI -> {
            drawCircle(AmberWarning, radius = w * 0.17f, center = Offset(w * 0.34f, h * 0.38f))
            drawCircle(outline, radius = w * 0.17f, center = Offset(w * 0.34f, h * 0.38f), style = Stroke(strokeW * 0.85f))
            drawLine(steelSlate, Offset(w * 0.44f, h * 0.48f), Offset(w * 0.78f, h * 0.80f), strokeW * 1.4f, StrokeCap.Round)
            drawLine(steelSlate, Offset(w * 0.64f, h * 0.66f), Offset(w * 0.74f, h * 0.56f), strokeW * 1.1f, StrokeCap.Round)
        }
        MriGlobalAssetKey.KARTU_ATM -> {
            drawRoundRect(CyanPrimary, Offset(w * 0.14f, h * 0.26f), Size(w * 0.72f, h * 0.48f), CornerRadius(w * 0.10f, w * 0.10f))
            drawRoundRect(outline, Offset(w * 0.14f, h * 0.26f), Size(w * 0.72f, h * 0.48f), CornerRadius(w * 0.10f, w * 0.10f), style = Stroke(strokeW))
            drawRect(outline, Offset(w * 0.16f, h * 0.38f), Size(w * 0.68f, h * 0.11f))
            drawRoundRect(AmberWarning, Offset(w * 0.24f, h * 0.56f), Size(w * 0.16f, h * 0.10f), CornerRadius(3f, 3f))
        }
        MriGlobalAssetKey.KURSI_RODA_BESI, MriGlobalAssetKey.KURSI_RODA_MR -> {
            val frameCol = if (assetKey == MriGlobalAssetKey.KURSI_RODA_MR) EmeraldSuccess else RoseDanger
            drawLine(frameCol, Offset(w * 0.34f, h * 0.22f), Offset(w * 0.34f, h * 0.56f), strokeW * 1.2f, StrokeCap.Round)
            drawLine(frameCol, Offset(w * 0.34f, h * 0.54f), Offset(w * 0.68f, h * 0.54f), strokeW * 1.2f, StrokeCap.Round)
            drawLine(frameCol, Offset(w * 0.68f, h * 0.54f), Offset(w * 0.74f, h * 0.76f), strokeW * 1.2f, StrokeCap.Round)
            drawCircle(frameCol, radius = w * 0.20f, center = Offset(w * 0.40f, h * 0.68f), style = Stroke(strokeW * 1.2f))
            drawCircle(steelSlate, radius = w * 0.08f, center = Offset(w * 0.74f, h * 0.78f))
        }
        MriGlobalAssetKey.PINSET -> {
            val path = Path().apply {
                moveTo(w * 0.24f, h * 0.78f)
                lineTo(w * 0.74f, h * 0.22f)
                lineTo(w * 0.46f, h * 0.84f)
            }
            drawPath(path, color = steelSlate, style = Stroke(strokeW * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(RoseDanger, radius = w * 0.08f, center = Offset(w * 0.74f, h * 0.22f))
        }
        MriGlobalAssetKey.STETOSKOP, MriGlobalAssetKey.STENT_COND -> {
            val col = if (assetKey == MriGlobalAssetKey.STENT_COND) EmeraldSuccess else AmberWarning
            drawArc(col, 0f, 180f, false, Offset(w * 0.24f, h * 0.16f), Size(w * 0.52f, h * 0.46f), style = Stroke(strokeW * 1.1f, cap = StrokeCap.Round))
            drawLine(col, Offset(w * 0.50f, h * 0.62f), Offset(w * 0.50f, h * 0.78f), strokeW * 1.1f, StrokeCap.Round)
            drawCircle(steelSlate, radius = w * 0.11f, center = Offset(w * 0.66f, h * 0.74f))
            drawCircle(outline, radius = w * 0.11f, center = Offset(w * 0.66f, h * 0.74f), style = Stroke(strokeW * 0.8f))
        }
        MriGlobalAssetKey.BANTAL_BUSA -> {
            drawRoundRect(CharAccentMint, Offset(w * 0.14f, h * 0.30f), Size(w * 0.72f, h * 0.42f), CornerRadius(w * 0.16f, w * 0.16f))
            drawRoundRect(outline, Offset(w * 0.14f, h * 0.30f), Size(w * 0.72f, h * 0.42f), CornerRadius(w * 0.16f, w * 0.16f), style = Stroke(strokeW))
            drawLine(Color.White.copy(alpha = 0.65f), Offset(w * 0.26f, h * 0.43f), Offset(w * 0.68f, h * 0.43f), strokeW * 0.8f, StrokeCap.Round)
        }
        MriGlobalAssetKey.EARPLUG -> {
            drawRoundRect(AmberWarning, Offset(w * 0.24f, h * 0.36f), Size(w * 0.16f, h * 0.38f), CornerRadius(w * 0.08f, w * 0.08f))
            drawRoundRect(AmberWarning, Offset(w * 0.60f, h * 0.36f), Size(w * 0.16f, h * 0.38f), CornerRadius(w * 0.08f, w * 0.08f))
            drawArc(CyanBright, 180f, 180f, false, Offset(w * 0.28f, h * 0.18f), Size(w * 0.44f, h * 0.44f), style = Stroke(strokeW, cap = StrokeCap.Round))
        }
        MriGlobalAssetKey.GAUN_KATUN -> {
            val gown = Path().apply {
                moveTo(w * 0.34f, h * 0.18f)
                lineTo(w * 0.66f, h * 0.18f)
                lineTo(w * 0.84f, h * 0.36f)
                lineTo(w * 0.74f, h * 0.48f)
                lineTo(w * 0.66f, h * 0.42f)
                lineTo(w * 0.68f, h * 0.82f)
                lineTo(w * 0.32f, h * 0.82f)
                lineTo(w * 0.34f, h * 0.42f)
                lineTo(w * 0.26f, h * 0.48f)
                lineTo(w * 0.16f, h * 0.36f)
                close()
            }
            drawPath(gown, color = EmeraldSuccess)
            drawPath(gown, color = outline, style = Stroke(strokeW * 0.9f, join = StrokeJoin.Round))
        }
        MriGlobalAssetKey.VIAL_KONTRAS -> {
            drawRoundRect(CyanBright, Offset(w * 0.34f, h * 0.32f), Size(w * 0.32f, h * 0.54f), CornerRadius(w * 0.08f, w * 0.08f))
            drawRoundRect(outline, Offset(w * 0.34f, h * 0.32f), Size(w * 0.32f, h * 0.54f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(strokeW))
            drawRoundRect(steelSlate, Offset(w * 0.40f, h * 0.16f), Size(w * 0.20f, h * 0.16f), CornerRadius(w * 0.04f, w * 0.04f))
            drawRect(EmeraldSuccess.copy(alpha = 0.75f), Offset(w * 0.38f, h * 0.56f), Size(w * 0.24f, h * 0.24f))
        }
        MriGlobalAssetKey.PHANTOM_ACR -> {
            drawCircle(CyanPrimary, radius = w * 0.34f, center = Offset(w * 0.50f, h * 0.50f))
            drawCircle(outline, radius = w * 0.34f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(strokeW))
            drawRect(Color.White.copy(alpha = 0.7f), Offset(w * 0.34f, h * 0.34f), Size(w * 0.32f, h * 0.32f), style = Stroke(strokeW * 0.7f))
            drawCircle(AmberWarning, radius = w * 0.06f, center = Offset(w * 0.42f, h * 0.42f))
            drawCircle(EmeraldSuccess, radius = w * 0.06f, center = Offset(w * 0.58f, h * 0.58f))
        }
        else -> {
            drawCircle(accent.copy(alpha = 0.25f), radius = w * 0.38f, center = Offset(w * 0.50f, h * 0.50f))
            drawCircle(accent, radius = w * 0.24f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(strokeW))
        }
    }
}
