package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharAccentMint
import com.example.ui.theme.CharActionOrange
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class MriGlobalAssetKey(val contentDescriptionId: String) {
    // Aset raster bawaan game & fallback global
    GLOBAL_MRI_EMBLEM("Emblem MRI Global"),
    LARMOR_MONITOR("Monitor Osiloskop Frekuensi Larmor"),
    SCREENING_GATE("Portal Detektor Feromagnetik Zona IV"),
    MAGNET_ARENA("Arena Pertahanan Medan 5 Gauss"),
    RHYTHM_STAGE("Panggung Sekuens Pulsa Spin Echo"),
    MRI_GANTRY("Mesin Pemindai MRI Superkonduktor"),

    // Simbol status & lencana pengganti stiker emoji
    TROPHY_MEDAL("Lencana Rekor & Pangkat"),
    SHIELD_5G("Perisai Keselamatan 5 Gauss"),
    SPARK_BOLT("Energi Pulsa RF & Combo"),
    GAME_CONTROLLER("Mode Arcade & Minigame Nano"),
    PULSE_WAVE("Gelombang Frekuensi Larmor"),
    KSPACE_SLICE("Citra Irisan & K-space MRI"),
    FIRE_STREAK("Lencana Streak Harian"),
    CLINICAL_ID_BADGE("Lencana Identitas Radiografer"),
    STAR_BADGE("Bintang Penguasaan Simpul"),
    CHECK_VERIFIED("Tanda Terverifikasi Klinis"),
    RADIOGRAPHER_CREST("Emblem Spesialis Radiologi & MRI"),

    // Benda skrining & proyektil MR Unsafe / Safe / Conditional
    TABUNG_O2("Tabung Oksigen Feromagnetik"),
    GUNTING_BAJA("Gunting Bedah Baja"),
    KLIP_LOGAM("Klip Aneurisma Feromagnetik"),
    PACEMAKER("Pacu Jantung Non-MR"),
    KUNCI_PAS("Kunci Pas Logam"),
    JAM_MEKANIK("Jam Tangan Mekanik"),
    KOIN_KUNCI("Koin & Kunci Logam"),
    KARTU_ATM("Kartu ATM Pita Magnetik"),
    KURSI_RODA_BESI("Kursi Roda Besi Biasa"),
    PINSET("Pinset Feromagnetik"),
    STETOSKOP("Stetoskop Klinis / Implan Terverifikasi"),
    BANTAL_BUSA("Bantal Busa MR Safe"),
    KURSI_RODA_MR("Kursi Roda Aluminium MR Safe"),
    EARPLUG("Pelindung Telinga Busa"),
    GAUN_KATUN("Gaun Pasien Katun Tanpa Logam"),
    VIAL_KONTRAS("Vial Kontras Gadolinium"),
    PHANTOM_ACR("Phantom Kalibrasi QA ACR"),
    STENT_COND("Stent Koroner MR Conditional"),
    PONSEL_LOGAM("Ponsel & Perangkat Elektronik"),
    TABUNG_PEMADAM("Tabung Pemadam Api Baja"),
    KACA_MATA_LOGAM("Kacamata Bingkai Logam"),
    POMPA_INFUS_BIASA("Pompa Infus Standar Non-MR"),
    BOTOL_MINUM_LOGAM("Botol Termos Baja Tahan Karat"),
    SANDBAG_NONMAG("Bantalan Pasir Non-Magnetik"),
    SELIMUT_KATUN("Selimut Katun Polos MR Safe"),
    TABUNG_O2_ALU("Tabung Oksigen Aluminium MR Conditional"),
    IMPLAN_TITANIUM("Pelat & Sekrup Ortopedi Titanium"),
    HEADSET_PNEUMATIK("Headset Pneumatik Akustik MR Safe"),
    MASKER_OKSIGEN_PLASTIK("Masker & Selang Oksigen Polimer"),
    PISAU_BEDAH_BAJA("Skalpel Bedah Baja Karbon"),
    PALU_REFLEKS_BESI("Palu Refleks Batang Besi"),
    CINCIN_FEROMAGNETIK("Cincin & Perhiasan Feromagnetik"),
    KARTU_AKSES_MAGNET("Lanyard Klip Besi & Kartu Magnetik"),

    // Fallback global resmi
    FALLBACK_MRI("Ilustrasi Gambar MRI Global")
}

object MriGlobalAssetCatalog {
    fun resolveKey(rawKey: String?): MriGlobalAssetKey {
        if (rawKey.isNullOrBlank()) return MriGlobalAssetKey.FALLBACK_MRI
        val norm = rawKey.trim().uppercase()
        return MriGlobalAssetKey.entries.firstOrNull { it.name == norm }
            ?: when {
                "TABUNG" in norm || "O2" in norm -> MriGlobalAssetKey.TABUNG_O2
                "GUNTING" in norm -> MriGlobalAssetKey.GUNTING_BAJA
                "KLIP" in norm -> MriGlobalAssetKey.KLIP_LOGAM
                "PACER" in norm || "PACEMAKER" in norm -> MriGlobalAssetKey.PACEMAKER
                "KUNCI_PAS" in norm -> MriGlobalAssetKey.KUNCI_PAS
                "KOIN" in norm || "KUNCI" in norm -> MriGlobalAssetKey.KOIN_KUNCI
                "JAM" in norm -> MriGlobalAssetKey.JAM_MEKANIK
                "ATM" in norm || "KARTU" in norm -> MriGlobalAssetKey.KARTU_ATM
                "KURSI_RODA_MR" in norm || "MR_SAFE_CHAIR" in norm -> MriGlobalAssetKey.KURSI_RODA_MR
                "KURSI" in norm -> MriGlobalAssetKey.KURSI_RODA_BESI
                "PINSET" in norm -> MriGlobalAssetKey.PINSET
                "STETO" in norm || "COND" in norm -> MriGlobalAssetKey.STETOSKOP
                "BANTAL" in norm -> MriGlobalAssetKey.BANTAL_BUSA
                "EARPLUG" in norm -> MriGlobalAssetKey.EARPLUG
                "GAUN" in norm -> MriGlobalAssetKey.GAUN_KATUN
                "VIAL" in norm -> MriGlobalAssetKey.VIAL_KONTRAS
                "PHANTOM" in norm -> MriGlobalAssetKey.PHANTOM_ACR
                "STENT" in norm -> MriGlobalAssetKey.STENT_COND
                "PONSEL" in norm || "HP" in norm || "SMARTPHONE" in norm -> MriGlobalAssetKey.PONSEL_LOGAM
                "PEMADAM" in norm || "APAR" in norm -> MriGlobalAssetKey.TABUNG_PEMADAM
                "KACAMATA" in norm || "KACA_MATA" in norm -> MriGlobalAssetKey.KACA_MATA_LOGAM
                "POMPA" in norm || "INFUS" in norm -> MriGlobalAssetKey.POMPA_INFUS_BIASA
                "BOTOL" in norm || "TERMOS" in norm -> MriGlobalAssetKey.BOTOL_MINUM_LOGAM
                "SANDBAG" in norm || "PASIR" in norm -> MriGlobalAssetKey.SANDBAG_NONMAG
                "SELIMUT" in norm || "KATUN" in norm -> MriGlobalAssetKey.SELIMUT_KATUN
                "ALU" in norm || "ALUMINIUM" in norm -> MriGlobalAssetKey.TABUNG_O2_ALU
                "IMPLAN" in norm || "TITANIUM" in norm || "PELAT" in norm -> MriGlobalAssetKey.IMPLAN_TITANIUM
                "HEADSET" in norm || "PNEUMATIK" in norm -> MriGlobalAssetKey.HEADSET_PNEUMATIK
                "MASKER" in norm || "POLIMER" in norm -> MriGlobalAssetKey.MASKER_OKSIGEN_PLASTIK
                "SKALPEL" in norm || "PISAU" in norm -> MriGlobalAssetKey.PISAU_BEDAH_BAJA
                "PALU" in norm || "REFLEKS" in norm -> MriGlobalAssetKey.PALU_REFLEKS_BESI
                "CINCIN" in norm || "PERHIASAN" in norm -> MriGlobalAssetKey.CINCIN_FEROMAGNETIK
                "LANYARD" in norm || "AKSES" in norm -> MriGlobalAssetKey.KARTU_AKSES_MAGNET
                "MONITOR" in norm -> MriGlobalAssetKey.LARMOR_MONITOR
                "TROPHY" in norm || "REKOR" in norm -> MriGlobalAssetKey.TROPHY_MEDAL
                "SHIELD" in norm || "PERISAI" in norm -> MriGlobalAssetKey.SHIELD_5G
                "STREAK" in norm || "FIRE" in norm -> MriGlobalAssetKey.FIRE_STREAK
                "STAR" in norm || "BINTANG" in norm -> MriGlobalAssetKey.STAR_BADGE
                "CHECK" in norm || "VERIF" in norm -> MriGlobalAssetKey.CHECK_VERIFIED
                "CREST" in norm || "RADIO" in norm -> MriGlobalAssetKey.RADIOGRAPHER_CREST
                "ID_BADGE" in norm -> MriGlobalAssetKey.CLINICAL_ID_BADGE
                else -> MriGlobalAssetKey.FALLBACK_MRI
            }
    }

    @DrawableRes
    fun drawableResOrNull(key: MriGlobalAssetKey): Int? = when (key) {
        MriGlobalAssetKey.GLOBAL_MRI_EMBLEM,
        MriGlobalAssetKey.FALLBACK_MRI -> R.drawable.img_mri_global_placeholder
        MriGlobalAssetKey.LARMOR_MONITOR -> R.drawable.img_larmor_monitor
        MriGlobalAssetKey.SCREENING_GATE -> R.drawable.img_arcade_screening_gate
        MriGlobalAssetKey.MAGNET_ARENA -> R.drawable.img_arcade_magnet_arena
        MriGlobalAssetKey.RHYTHM_STAGE -> R.drawable.img_arcade_rhythm_stage
        MriGlobalAssetKey.MRI_GANTRY -> R.drawable.mri_icon_bersih
        else -> null
    }
}

/**
 * Komponen aset gambar global Belajar MRI.
 * Dapat digunakan di layar mana pun sebagai pengganti stiker emoji maupun placeholder gambar standar,
 * memadukan aset gambar bawaan (img_*) dan ilustrasi vektor flat dark-outline bergaya Nano.
 */
@Composable
fun MriGlobalAssetImage(
    assetKey: MriGlobalAssetKey,
    modifier: Modifier = Modifier,
    tintAccent: Color = CyanBright,
    showBadgeFrame: Boolean = false,
    contentDescription: String? = assetKey.contentDescriptionId
) {
    val drawableRes = MriGlobalAssetCatalog.drawableResOrNull(assetKey)
    val frameModifier = if (showBadgeFrame) {
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF071322))
            .border(1.3.dp, tintAccent.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
    } else {
        modifier
    }

    Box(
        modifier = frameModifier,
        contentAlignment = Alignment.Center
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            val pad = if (showBadgeFrame) 6.dp else 1.dp
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
            ) {
                val w = size.width
                val h = size.height
                val outline = Color(0xFF071322)
                when (assetKey) {
                    MriGlobalAssetKey.TROPHY_MEDAL -> {
                        drawCircle(AmberWarning.copy(alpha = 0.22f), radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.48f))
                        val cup = Path().apply {
                            moveTo(w * 0.28f, h * 0.20f)
                            lineTo(w * 0.72f, h * 0.20f)
                            lineTo(w * 0.65f, h * 0.56f)
                            lineTo(w * 0.50f, h * 0.66f)
                            lineTo(w * 0.35f, h * 0.56f)
                            close()
                        }
                        drawPath(cup, color = AmberWarning)
                        drawPath(cup, color = outline, style = Stroke(width = w * 0.06f, join = StrokeJoin.Round))
                        drawLine(AmberWarning, Offset(w * 0.50f, h * 0.65f), Offset(w * 0.50f, h * 0.80f), w * 0.11f, StrokeCap.Round)
                        drawRoundRect(AmberWarning, Offset(w * 0.30f, h * 0.78f), Size(w * 0.40f, h * 0.11f), CornerRadius(w * 0.04f, w * 0.04f))
                    }
                    MriGlobalAssetKey.SHIELD_5G -> {
                        val shield = Path().apply {
                            moveTo(w * 0.50f, h * 0.12f)
                            lineTo(w * 0.84f, h * 0.26f)
                            lineTo(w * 0.80f, h * 0.60f)
                            lineTo(w * 0.50f, h * 0.88f)
                            lineTo(w * 0.20f, h * 0.60f)
                            lineTo(w * 0.16f, h * 0.26f)
                            close()
                        }
                        drawPath(shield, brush = Brush.verticalGradient(listOf(tintAccent, tintAccent.copy(alpha = 0.6f))))
                        drawPath(shield, color = outline, style = Stroke(width = w * 0.06f, join = StrokeJoin.Round))
                        drawCircle(Color.White.copy(alpha = 0.85f), radius = w * 0.10f, center = Offset(w * 0.5f, h * 0.48f))
                    }
                    MriGlobalAssetKey.SPARK_BOLT -> {
                        val bolt = Path().apply {
                            moveTo(w * 0.58f, h * 0.10f)
                            lineTo(w * 0.24f, h * 0.54f)
                            lineTo(w * 0.48f, h * 0.54f)
                            lineTo(w * 0.40f, h * 0.90f)
                            lineTo(w * 0.76f, h * 0.44f)
                            lineTo(w * 0.52f, h * 0.44f)
                            close()
                        }
                        drawPath(bolt, color = tintAccent)
                        drawPath(bolt, color = outline, style = Stroke(width = w * 0.055f, join = StrokeJoin.Round))
                    }
                    MriGlobalAssetKey.GAME_CONTROLLER -> {
                        drawRoundRect(
                            color = CyanPrimary,
                            topLeft = Offset(w * 0.12f, h * 0.28f),
                            size = Size(w * 0.76f, h * 0.44f),
                            cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
                        )
                        drawRoundRect(
                            color = outline,
                            topLeft = Offset(w * 0.12f, h * 0.28f),
                            size = Size(w * 0.76f, h * 0.44f),
                            cornerRadius = CornerRadius(w * 0.22f, w * 0.22f),
                            style = Stroke(width = w * 0.055f)
                        )
                        drawLine(Color.White, Offset(w * 0.28f, h * 0.50f), Offset(w * 0.42f, h * 0.50f), w * 0.07f, StrokeCap.Round)
                        drawLine(Color.White, Offset(w * 0.35f, h * 0.43f), Offset(w * 0.35f, h * 0.57f), w * 0.07f, StrokeCap.Round)
                        drawCircle(AmberWarning, radius = w * 0.06f, center = Offset(w * 0.64f, h * 0.45f))
                        drawCircle(EmeraldSuccess, radius = w * 0.06f, center = Offset(w * 0.72f, h * 0.55f))
                    }
                    MriGlobalAssetKey.PULSE_WAVE -> {
                        val path = Path()
                        for (i in 0..24) {
                            val t = i / 24f
                            val x = w * 0.10f + t * w * 0.80f
                            val env = 1f - kotlin.math.abs(t - 0.5f) * 1.4f
                            val y = h * 0.50f - sin(t * 4f * PI).toFloat() * h * 0.30f * env.coerceAtLeast(0.2f)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = CharActionOrange, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))
                        drawCircle(CyanBright, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.24f))
                    }
                    MriGlobalAssetKey.KSPACE_SLICE -> {
                        drawRoundRect(Color(0xFF133352), Offset(w * 0.12f, h * 0.12f), Size(w * 0.76f, h * 0.76f), CornerRadius(w * 0.14f, w * 0.14f))
                        drawOval(CharAccentMint, Offset(w * 0.24f, h * 0.18f), Size(w * 0.52f, h * 0.64f), style = Stroke(width = w * 0.07f))
                        drawLine(CyanBright, Offset(w * 0.50f, h * 0.20f), Offset(w * 0.50f, h * 0.80f), w * 0.05f)
                        drawOval(CyanBright.copy(alpha = 0.75f), Offset(w * 0.34f, h * 0.38f), Size(w * 0.12f, h * 0.22f))
                        drawOval(CyanBright.copy(alpha = 0.75f), Offset(w * 0.54f, h * 0.38f), Size(w * 0.12f, h * 0.22f))
                    }
                    MriGlobalAssetKey.FIRE_STREAK -> {
                        val flame = Path().apply {
                            moveTo(w * 0.50f, h * 0.10f)
                            cubicTo(w * 0.78f, h * 0.32f, w * 0.84f, h * 0.62f, w * 0.68f, h * 0.82f)
                            cubicTo(w * 0.58f, h * 0.92f, w * 0.42f, h * 0.92f, w * 0.32f, h * 0.82f)
                            cubicTo(w * 0.16f, h * 0.62f, w * 0.24f, h * 0.34f, w * 0.50f, h * 0.10f)
                            close()
                        }
                        drawPath(flame, brush = Brush.verticalGradient(listOf(AmberWarning, CharActionOrange)))
                        drawPath(flame, color = outline, style = Stroke(width = w * 0.06f, join = StrokeJoin.Round))
                        drawCircle(Color(0xFFFFF3B0), radius = w * 0.14f, center = Offset(w * 0.50f, h * 0.66f))
                    }
                    MriGlobalAssetKey.CLINICAL_ID_BADGE -> {
                        drawRoundRect(
                            color = Color(0xFF102A43),
                            topLeft = Offset(w * 0.12f, h * 0.18f),
                            size = Size(w * 0.76f, h * 0.64f),
                            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
                        )
                        drawRoundRect(
                            color = tintAccent,
                            topLeft = Offset(w * 0.12f, h * 0.18f),
                            size = Size(w * 0.76f, h * 0.64f),
                            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                            style = Stroke(width = w * 0.06f)
                        )
                        drawCircle(CharAccentMint, radius = w * 0.12f, center = Offset(w * 0.34f, h * 0.50f))
                        drawLine(CyanBright, Offset(w * 0.52f, h * 0.40f), Offset(w * 0.76f, h * 0.40f), w * 0.07f, StrokeCap.Round)
                        drawLine(Color.White.copy(alpha = 0.75f), Offset(w * 0.52f, h * 0.58f), Offset(w * 0.72f, h * 0.58f), w * 0.06f, StrokeCap.Round)
                    }
                    MriGlobalAssetKey.STAR_BADGE -> {
                        val star = Path().apply {
                            val cx = w * 0.50f
                            val cy = h * 0.50f
                            val outerR = w * 0.40f
                            val innerR = w * 0.18f
                            for (i in 0 until 10) {
                                val angle = (-PI / 2.0) + i * (PI / 5.0)
                                val r = if (i % 2 == 0) outerR else innerR
                                val px = cx + (cos(angle) * r).toFloat()
                                val py = cy + (sin(angle) * r).toFloat()
                                if (i == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(star, color = AmberWarning)
                        drawPath(star, color = outline, style = Stroke(width = w * 0.055f, join = StrokeJoin.Round))
                    }
                    MriGlobalAssetKey.CHECK_VERIFIED -> {
                        drawCircle(EmeraldSuccess.copy(alpha = 0.22f), radius = w * 0.44f, center = Offset(w * 0.50f, h * 0.50f))
                        drawCircle(EmeraldSuccess, radius = w * 0.36f, center = Offset(w * 0.50f, h * 0.50f))
                        drawCircle(outline, radius = w * 0.36f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(width = w * 0.055f))
                        val check = Path().apply {
                            moveTo(w * 0.33f, h * 0.52f)
                            lineTo(w * 0.45f, h * 0.64f)
                            lineTo(w * 0.68f, h * 0.38f)
                        }
                        drawPath(check, color = Color(0xFF052E16), style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    MriGlobalAssetKey.RADIOGRAPHER_CREST -> {
                        drawCircle(tintAccent.copy(alpha = 0.20f), radius = w * 0.44f, center = Offset(w * 0.50f, h * 0.50f))
                        drawCircle(tintAccent, radius = w * 0.34f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(width = w * 0.07f))
                        drawCircle(CharAccentMint, radius = w * 0.18f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(width = w * 0.06f))
                        drawCircle(AmberWarning, radius = w * 0.07f, center = Offset(w * 0.50f, h * 0.50f))
                    }
                    else -> {
                        drawMriObjectVector(assetKey = assetKey, accent = tintAccent)
                    }
                }
            }
        }
    }
}

@Composable
fun MriGlobalFallbackImage(
    modifier: Modifier = Modifier,
    contentDescription: String = "Ilustrasi MRI Global"
) {
    MriGlobalAssetImage(
        assetKey = MriGlobalAssetKey.FALLBACK_MRI,
        modifier = modifier,
        contentDescription = contentDescription
    )
}
