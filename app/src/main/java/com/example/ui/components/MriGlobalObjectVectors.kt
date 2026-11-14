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
            // Bilah gunting bedah bersilang dengan tepi gelap & kilap baja
            val bladeA = Path().apply {
                moveTo(w * 0.36f, h * 0.62f)
                lineTo(w * 0.72f, h * 0.14f)
                lineTo(w * 0.80f, h * 0.21f)
                lineTo(w * 0.45f, h * 0.66f)
                close()
            }
            val bladeB = Path().apply {
                moveTo(w * 0.64f, h * 0.62f)
                lineTo(w * 0.28f, h * 0.14f)
                lineTo(w * 0.20f, h * 0.21f)
                lineTo(w * 0.55f, h * 0.66f)
                close()
            }
            drawPath(bladeA, color = steelSlate)
            drawPath(bladeA, color = outline, style = Stroke(strokeW * 0.75f, join = StrokeJoin.Round))
            drawPath(bladeB, color = steelLight)
            drawPath(bladeB, color = outline, style = Stroke(strokeW * 0.75f, join = StrokeJoin.Round))
            drawLine(Color.White.copy(alpha = 0.8f), Offset(w * 0.30f, h * 0.22f), Offset(w * 0.44f, h * 0.40f), strokeW * 0.45f, StrokeCap.Round)
            // Cincin gagang ergonomis ganda
            drawCircle(RoseDanger, radius = w * 0.135f, center = Offset(w * 0.29f, h * 0.75f), style = Stroke(strokeW * 1.15f))
            drawCircle(outline, radius = w * 0.165f, center = Offset(w * 0.29f, h * 0.75f), style = Stroke(strokeW * 0.45f))
            drawCircle(RoseDanger, radius = w * 0.135f, center = Offset(w * 0.71f, h * 0.75f), style = Stroke(strokeW * 1.15f))
            drawCircle(outline, radius = w * 0.165f, center = Offset(w * 0.71f, h * 0.75f), style = Stroke(strokeW * 0.45f))
            // Baut poros tengah
            drawCircle(steelSlate, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.47f))
            drawCircle(outline, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.47f), style = Stroke(strokeW * 0.6f))
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
            // Tali jam kulit/logam dengan lug & crown pemutar
            drawRoundRect(CharActionOrange, Offset(w * 0.37f, h * 0.08f), Size(w * 0.26f, h * 0.84f), CornerRadius(w * 0.08f, w * 0.08f))
            drawRoundRect(outline, Offset(w * 0.37f, h * 0.08f), Size(w * 0.26f, h * 0.84f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(strokeW * 0.8f))
            drawLine(Color.White.copy(alpha = 0.45f), Offset(w * 0.43f, h * 0.14f), Offset(w * 0.57f, h * 0.14f), strokeW * 0.5f, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.45f), Offset(w * 0.43f, h * 0.86f), Offset(w * 0.57f, h * 0.86f), strokeW * 0.5f, StrokeCap.Round)
            // Crown pengatur di kanan bezel
            drawRoundRect(steelLight, Offset(w * 0.73f, h * 0.45f), Size(w * 0.09f, h * 0.10f), CornerRadius(w * 0.02f, w * 0.02f))
            drawRoundRect(outline, Offset(w * 0.73f, h * 0.45f), Size(w * 0.09f, h * 0.10f), CornerRadius(w * 0.02f, w * 0.02f), style = Stroke(strokeW * 0.6f))
            // Casing & bezel arloji
            drawCircle(steelSlate, radius = w * 0.27f, center = Offset(w * 0.50f, h * 0.50f))
            drawCircle(Color(0xFFF1F5F9), radius = w * 0.21f, center = Offset(w * 0.50f, h * 0.50f))
            drawCircle(outline, radius = w * 0.27f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(strokeW))
            // Penanda jam 12, 3, 6, 9
            drawLine(outline, Offset(w * 0.50f, h * 0.31f), Offset(w * 0.50f, h * 0.34f), strokeW * 0.65f, StrokeCap.Round)
            drawLine(outline, Offset(w * 0.69f, h * 0.50f), Offset(w * 0.66f, h * 0.50f), strokeW * 0.65f, StrokeCap.Round)
            drawLine(outline, Offset(w * 0.50f, h * 0.69f), Offset(w * 0.50f, h * 0.66f), strokeW * 0.65f, StrokeCap.Round)
            drawLine(outline, Offset(w * 0.31f, h * 0.50f), Offset(w * 0.34f, h * 0.50f), strokeW * 0.65f, StrokeCap.Round)
            // Jarum jam, menit, detik
            drawLine(outline, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.50f, h * 0.35f), strokeW * 0.85f, StrokeCap.Round)
            drawLine(outline, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.61f, h * 0.44f), strokeW * 0.7f, StrokeCap.Round)
            drawLine(RoseDanger, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.62f, h * 0.59f), strokeW * 0.55f, StrokeCap.Round)
            drawCircle(outline, radius = w * 0.035f, center = Offset(w * 0.50f, h * 0.50f))
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
            val isSafe = assetKey == MriGlobalAssetKey.KURSI_RODA_MR
            val frameCol = if (isSafe) EmeraldSuccess else RoseDanger
            val seatCol = if (isSafe) CharAccentMint else steelLight
            // Pegangan dorong & sandaran belakang
            drawLine(frameCol, Offset(w * 0.20f, h * 0.18f), Offset(w * 0.31f, h * 0.18f), strokeW * 1.1f, StrokeCap.Round)
            drawLine(frameCol, Offset(w * 0.31f, h * 0.18f), Offset(w * 0.31f, h * 0.55f), strokeW * 1.25f, StrokeCap.Round)
            // Sandaran lengan & dudukan
            drawLine(seatCol, Offset(w * 0.31f, h * 0.38f), Offset(w * 0.59f, h * 0.38f), strokeW * 0.9f, StrokeCap.Round)
            drawRoundRect(seatCol, Offset(w * 0.30f, h * 0.49f), Size(w * 0.36f, h * 0.08f), CornerRadius(w * 0.03f, w * 0.03f))
            drawLine(frameCol, Offset(w * 0.31f, h * 0.55f), Offset(w * 0.68f, h * 0.55f), strokeW * 1.2f, StrokeCap.Round)
            // Penopang kaki & pijakan
            drawLine(frameCol, Offset(w * 0.66f, h * 0.55f), Offset(w * 0.74f, h * 0.75f), strokeW * 1.1f, StrokeCap.Round)
            drawLine(steelSlate, Offset(w * 0.72f, h * 0.76f), Offset(w * 0.85f, h * 0.76f), strokeW * 1.0f, StrokeCap.Round)
            // Roda belakang besar + jari-jari + hand-rim
            val wheelCenter = Offset(w * 0.38f, h * 0.67f)
            drawCircle(Color(0xFF0A1D33), radius = w * 0.20f, center = wheelCenter)
            drawCircle(frameCol, radius = w * 0.20f, center = wheelCenter, style = Stroke(strokeW * 1.15f))
            drawCircle(steelSlate, radius = w * 0.145f, center = wheelCenter, style = Stroke(strokeW * 0.55f))
            drawLine(steelSlate, Offset(w * 0.25f, h * 0.67f), Offset(w * 0.51f, h * 0.67f), strokeW * 0.45f)
            drawLine(steelSlate, Offset(w * 0.38f, h * 0.54f), Offset(w * 0.38f, h * 0.80f), strokeW * 0.45f)
            drawCircle(outline, radius = w * 0.045f, center = wheelCenter)
            // Roda depan kecil (caster)
            drawCircle(steelSlate, radius = w * 0.075f, center = Offset(w * 0.72f, h * 0.79f))
            drawCircle(outline, radius = w * 0.075f, center = Offset(w * 0.72f, h * 0.79f), style = Stroke(strokeW * 0.75f))
            if (isSafe) {
                drawCircle(EmeraldSuccess, radius = w * 0.09f, center = Offset(w * 0.74f, h * 0.24f))
                drawCircle(outline, radius = w * 0.09f, center = Offset(w * 0.74f, h * 0.24f), style = Stroke(strokeW * 0.55f))
            }
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
        MriGlobalAssetKey.PONSEL_LOGAM -> {
            drawRoundRect(steelSlate, Offset(w * 0.28f, h * 0.14f), Size(w * 0.44f, h * 0.72f), CornerRadius(w * 0.08f, w * 0.08f))
            drawRoundRect(outline, Offset(w * 0.28f, h * 0.14f), Size(w * 0.44f, h * 0.72f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(strokeW))
            drawRoundRect(Color(0xFF0A2442), Offset(w * 0.33f, h * 0.22f), Size(w * 0.34f, h * 0.52f), CornerRadius(w * 0.04f, w * 0.04f))
            drawCircle(CyanBright, radius = w * 0.05f, center = Offset(w * 0.50f, h * 0.44f))
            drawArc(RoseDanger, 210f, 120f, false, Offset(w * 0.38f, h * 0.32f), Size(w * 0.24f, h * 0.24f), style = Stroke(strokeW * 0.75f, cap = StrokeCap.Round))
            drawCircle(outline, radius = w * 0.03f, center = Offset(w * 0.50f, h * 0.80f))
        }
        MriGlobalAssetKey.TABUNG_PEMADAM -> {
            drawRoundRect(RoseDanger, Offset(w * 0.32f, h * 0.28f), Size(w * 0.36f, h * 0.58f), CornerRadius(w * 0.12f, w * 0.12f))
            drawRoundRect(outline, Offset(w * 0.32f, h * 0.28f), Size(w * 0.36f, h * 0.58f), CornerRadius(w * 0.12f, w * 0.12f), style = Stroke(strokeW))
            drawRoundRect(steelSlate, Offset(w * 0.42f, h * 0.16f), Size(w * 0.16f, h * 0.12f), CornerRadius(w * 0.03f, w * 0.03f))
            drawLine(outline, Offset(w * 0.36f, h * 0.16f), Offset(w * 0.64f, h * 0.14f), strokeW, StrokeCap.Round)
            drawLine(outline, Offset(w * 0.32f, h * 0.36f), Offset(w * 0.20f, h * 0.60f), strokeW * 1.1f, StrokeCap.Round)
            drawRect(AmberWarning, Offset(w * 0.38f, h * 0.48f), Size(w * 0.24f, h * 0.16f))
        }
        MriGlobalAssetKey.KACA_MATA_LOGAM -> {
            drawCircle(Color(0xFF15385A), radius = w * 0.15f, center = Offset(w * 0.30f, h * 0.54f))
            drawCircle(Color(0xFF15385A), radius = w * 0.15f, center = Offset(w * 0.70f, h * 0.54f))
            drawCircle(steelLight, radius = w * 0.15f, center = Offset(w * 0.30f, h * 0.54f), style = Stroke(strokeW * 0.95f))
            drawCircle(steelLight, radius = w * 0.15f, center = Offset(w * 0.70f, h * 0.54f), style = Stroke(strokeW * 0.95f))
            drawLine(steelLight, Offset(w * 0.45f, h * 0.52f), Offset(w * 0.55f, h * 0.52f), strokeW * 0.9f, StrokeCap.Round)
            drawLine(outline, Offset(w * 0.16f, h * 0.49f), Offset(w * 0.26f, h * 0.30f), strokeW * 0.85f, StrokeCap.Round)
            drawLine(outline, Offset(w * 0.84f, h * 0.49f), Offset(w * 0.74f, h * 0.30f), strokeW * 0.85f, StrokeCap.Round)
        }
        MriGlobalAssetKey.POMPA_INFUS_BIASA -> {
            drawRoundRect(steelSlate, Offset(w * 0.22f, h * 0.18f), Size(w * 0.56f, h * 0.64f), CornerRadius(w * 0.08f, w * 0.08f))
            drawRoundRect(outline, Offset(w * 0.22f, h * 0.18f), Size(w * 0.56f, h * 0.64f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(strokeW))
            drawRoundRect(Color(0xFF0A2442), Offset(w * 0.29f, h * 0.26f), Size(w * 0.42f, h * 0.24f), CornerRadius(w * 0.04f, w * 0.04f))
            drawLine(CyanBright, Offset(w * 0.33f, h * 0.38f), Offset(w * 0.65f, h * 0.38f), strokeW * 0.6f, StrokeCap.Round)
            drawCircle(RoseDanger, radius = w * 0.06f, center = Offset(w * 0.36f, h * 0.66f))
            drawCircle(AmberWarning, radius = w * 0.06f, center = Offset(w * 0.54f, h * 0.66f))
            drawLine(CyanPrimary, Offset(w * 0.50f, h * 0.82f), Offset(w * 0.50f, h * 0.92f), strokeW * 0.75f, StrokeCap.Round)
        }
        MriGlobalAssetKey.BOTOL_MINUM_LOGAM -> {
            drawRoundRect(steelLight, Offset(w * 0.35f, h * 0.26f), Size(w * 0.30f, h * 0.60f), CornerRadius(w * 0.09f, w * 0.09f))
            drawRoundRect(outline, Offset(w * 0.35f, h * 0.26f), Size(w * 0.30f, h * 0.60f), CornerRadius(w * 0.09f, w * 0.09f), style = Stroke(strokeW))
            drawRoundRect(outline, Offset(w * 0.40f, h * 0.14f), Size(w * 0.20f, h * 0.12f), CornerRadius(w * 0.03f, w * 0.03f))
            drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.42f, h * 0.35f), Offset(w * 0.42f, h * 0.76f), strokeW * 0.6f, StrokeCap.Round)
        }
        MriGlobalAssetKey.SANDBAG_NONMAG -> {
            drawRoundRect(EmeraldSuccess, Offset(w * 0.16f, h * 0.32f), Size(w * 0.68f, h * 0.40f), CornerRadius(w * 0.14f, w * 0.14f))
            drawRoundRect(outline, Offset(w * 0.16f, h * 0.32f), Size(w * 0.68f, h * 0.40f), CornerRadius(w * 0.14f, w * 0.14f), style = Stroke(strokeW))
            drawLine(Color(0xFF064E3B), Offset(w * 0.30f, h * 0.34f), Offset(w * 0.30f, h * 0.70f), strokeW * 0.75f)
            drawLine(Color(0xFF064E3B), Offset(w * 0.70f, h * 0.34f), Offset(w * 0.70f, h * 0.70f), strokeW * 0.75f)
            drawCircle(CharAccentMint, radius = w * 0.07f, center = Offset(w * 0.50f, h * 0.52f))
        }
        MriGlobalAssetKey.SELIMUT_KATUN -> {
            drawRoundRect(CharAccentMint, Offset(w * 0.18f, h * 0.26f), Size(w * 0.64f, h * 0.48f), CornerRadius(w * 0.10f, w * 0.10f))
            drawRoundRect(outline, Offset(w * 0.18f, h * 0.26f), Size(w * 0.64f, h * 0.48f), CornerRadius(w * 0.10f, w * 0.10f), style = Stroke(strokeW))
            drawLine(EmeraldSuccess, Offset(w * 0.18f, h * 0.44f), Offset(w * 0.82f, h * 0.44f), strokeW * 0.9f)
            drawLine(Color.White.copy(alpha = 0.6f), Offset(w * 0.26f, h * 0.60f), Offset(w * 0.74f, h * 0.60f), strokeW * 0.65f, StrokeCap.Round)
        }
        MriGlobalAssetKey.TABUNG_O2_ALU -> {
            drawRoundRect(EmeraldSuccess, Offset(w * 0.33f, h * 0.24f), Size(w * 0.34f, h * 0.64f), CornerRadius(w * 0.14f, w * 0.14f))
            drawRoundRect(outline, Offset(w * 0.33f, h * 0.24f), Size(w * 0.34f, h * 0.64f), CornerRadius(w * 0.14f, w * 0.14f), style = Stroke(strokeW))
            drawRoundRect(CharAccentMint, Offset(w * 0.42f, h * 0.11f), Size(w * 0.16f, h * 0.15f), CornerRadius(w * 0.04f, w * 0.04f))
            drawRect(CyanBright, Offset(w * 0.35f, h * 0.46f), Size(w * 0.30f, h * 0.14f))
            drawLine(Color.White.copy(alpha = 0.8f), Offset(w * 0.41f, h * 0.30f), Offset(w * 0.41f, h * 0.78f), strokeW * 0.65f, StrokeCap.Round)
        }
        MriGlobalAssetKey.IMPLAN_TITANIUM -> {
            drawRoundRect(steelSlate, Offset(w * 0.36f, h * 0.16f), Size(w * 0.28f, h * 0.68f), CornerRadius(w * 0.12f, w * 0.12f))
            drawRoundRect(outline, Offset(w * 0.36f, h * 0.16f), Size(w * 0.28f, h * 0.68f), CornerRadius(w * 0.12f, w * 0.12f), style = Stroke(strokeW))
            listOf(0.28f, 0.44f, 0.60f, 0.74f).forEach { ry ->
                drawCircle(EmeraldSuccess, radius = w * 0.055f, center = Offset(w * 0.50f, h * ry))
                drawCircle(outline, radius = w * 0.055f, center = Offset(w * 0.50f, h * ry), style = Stroke(strokeW * 0.55f))
            }
        }
        MriGlobalAssetKey.HEADSET_PNEUMATIK -> {
            drawArc(CyanBright, 180f, 180f, false, Offset(w * 0.20f, h * 0.18f), Size(w * 0.60f, h * 0.56f), style = Stroke(strokeW * 1.25f, cap = StrokeCap.Round))
            drawRoundRect(CharAccentMint, Offset(w * 0.16f, h * 0.42f), Size(w * 0.16f, h * 0.32f), CornerRadius(w * 0.08f, w * 0.08f))
            drawRoundRect(CharAccentMint, Offset(w * 0.68f, h * 0.42f), Size(w * 0.16f, h * 0.32f), CornerRadius(w * 0.08f, w * 0.08f))
            drawRoundRect(outline, Offset(w * 0.16f, h * 0.42f), Size(w * 0.16f, h * 0.32f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(strokeW * 0.7f))
            drawRoundRect(outline, Offset(w * 0.68f, h * 0.42f), Size(w * 0.16f, h * 0.32f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(strokeW * 0.7f))
            drawArc(EmeraldSuccess, 20f, 140f, false, Offset(w * 0.26f, h * 0.56f), Size(w * 0.48f, h * 0.30f), style = Stroke(strokeW * 0.8f, cap = StrokeCap.Round))
        }
        MriGlobalAssetKey.MASKER_OKSIGEN_PLASTIK -> {
            val mask = Path().apply {
                moveTo(w * 0.50f, h * 0.20f)
                lineTo(w * 0.74f, h * 0.56f)
                lineTo(w * 0.62f, h * 0.74f)
                lineTo(w * 0.38f, h * 0.74f)
                lineTo(w * 0.26f, h * 0.56f)
                close()
            }
            drawPath(mask, color = CharAccentMint.copy(alpha = 0.75f))
            drawPath(mask, color = outline, style = Stroke(strokeW * 0.9f, join = StrokeJoin.Round))
            drawLine(CyanBright, Offset(w * 0.50f, h * 0.74f), Offset(w * 0.50f, h * 0.90f), strokeW, StrokeCap.Round)
            drawArc(EmeraldSuccess, 165f, 210f, false, Offset(w * 0.18f, h * 0.34f), Size(w * 0.64f, h * 0.34f), style = Stroke(strokeW * 0.65f, cap = StrokeCap.Round))
        }
        MriGlobalAssetKey.PISAU_BEDAH_BAJA -> {
            drawLine(steelSlate, Offset(w * 0.20f, h * 0.78f), Offset(w * 0.60f, h * 0.38f), strokeW * 1.4f, StrokeCap.Round)
            val tip = Path().apply {
                moveTo(w * 0.58f, h * 0.40f)
                lineTo(w * 0.82f, h * 0.16f)
                lineTo(w * 0.72f, h * 0.38f)
                close()
            }
            drawPath(tip, color = steelLight)
            drawPath(tip, color = outline, style = Stroke(strokeW * 0.7f, join = StrokeJoin.Round))
            drawCircle(RoseDanger, radius = w * 0.06f, center = Offset(w * 0.24f, h * 0.74f))
        }
        MriGlobalAssetKey.PALU_REFLEKS_BESI -> {
            drawLine(steelSlate, Offset(w * 0.28f, h * 0.82f), Offset(w * 0.62f, h * 0.30f), strokeW * 1.25f, StrokeCap.Round)
            val head = Path().apply {
                moveTo(w * 0.38f, h * 0.24f)
                lineTo(w * 0.80f, h * 0.22f)
                lineTo(w * 0.66f, h * 0.42f)
                close()
            }
            drawPath(head, color = RoseDanger)
            drawPath(head, color = outline, style = Stroke(strokeW * 0.85f, join = StrokeJoin.Round))
        }
        MriGlobalAssetKey.CINCIN_FEROMAGNETIK -> {
            drawCircle(AmberWarning, radius = w * 0.24f, center = Offset(w * 0.50f, h * 0.54f), style = Stroke(strokeW * 1.45f))
            drawCircle(outline, radius = w * 0.28f, center = Offset(w * 0.50f, h * 0.54f), style = Stroke(strokeW * 0.55f))
            val gem = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.62f, h * 0.28f)
                lineTo(w * 0.50f, h * 0.38f)
                lineTo(w * 0.38f, h * 0.28f)
                close()
            }
            drawPath(gem, color = CyanBright)
            drawPath(gem, color = outline, style = Stroke(strokeW * 0.65f, join = StrokeJoin.Round))
        }
        MriGlobalAssetKey.KARTU_AKSES_MAGNET -> {
            drawLine(RoseDanger, Offset(w * 0.36f, h * 0.12f), Offset(w * 0.50f, h * 0.34f), strokeW * 0.95f, StrokeCap.Round)
            drawLine(RoseDanger, Offset(w * 0.64f, h * 0.12f), Offset(w * 0.50f, h * 0.34f), strokeW * 0.95f, StrokeCap.Round)
            drawCircle(steelSlate, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.34f))
            drawRoundRect(AmberWarning, Offset(w * 0.24f, h * 0.40f), Size(w * 0.52f, h * 0.44f), CornerRadius(w * 0.08f, w * 0.08f))
            drawRoundRect(outline, Offset(w * 0.24f, h * 0.40f), Size(w * 0.52f, h * 0.44f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(strokeW * 0.85f))
            drawRect(outline, Offset(w * 0.26f, h * 0.52f), Size(w * 0.48f, h * 0.10f))
        }
        else -> {
            drawCircle(accent.copy(alpha = 0.25f), radius = w * 0.38f, center = Offset(w * 0.50f, h * 0.50f))
            drawCircle(accent, radius = w * 0.24f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(strokeW))
        }
    }
}
