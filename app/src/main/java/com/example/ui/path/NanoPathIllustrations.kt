package com.example.ui.path

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CharacterCustomizationState
import com.example.ui.character.AvatarView
import com.example.ui.character.Expression
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CharAccentCyan
import com.example.ui.theme.CharAccentMint
import com.example.ui.theme.CharAccentTeal
import com.example.ui.theme.CharActionOrange
import com.example.ui.theme.CharActionYellow
import com.example.ui.theme.CharBorderSlate
import com.example.ui.theme.CharCardSurface
import com.example.ui.theme.CharSpeechBubbleBg
import com.example.ui.theme.CharSpeechBubbleText
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySlate
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class NanoIllustrationType(
    val label: String,
    val badgeColor: Color,
    val responses: List<String>
) {
    MRI_SCANNER(
        label = "Alat MRI Nano",
        badgeColor = CyanBright,
        responses = listOf(
            "Gantry superkonduktor ini menjaga medan magnet B0 tetap stabil sepanjang pemindaian!",
            "Di dalam terowongan bore, koil gradien dan RF bekerja serempak membaca sinyal proton tubuh.",
            "Tip Nano: Lepas semua benda feromagnetik sebelum pasien masuk ke ruang magnet!"
        )
    ),
    MRI_BRAIN_PHOTO(
        label = "Citra MRI Otak",
        badgeColor = CharAccentTeal,
        responses = listOf(
            "Foto MRI otak gaya Nano! Cairan CSF tampak gelap di T1W dan terang di T2W.",
            "Perbedaan kontras jaringan lunak tanpa radiasi pengion adalah kekuatan utama MRI.",
            "Cek simetri hemisfer dan batas korteks-medula untuk menilai kualitas irisan aksial!"
        )
    ),
    HYDROGEN_ATOM(
        label = "Atom Hidrogen ¹H",
        badgeColor = CharAccentMint,
        responses = listOf(
            "Inti hidrogen (¹H) berputar seperti gasing mini dan berpresesi pada frekuensi Larmor!",
            "Tubuh manusia kaya akan air & lemak, sehingga sinyal ¹H sangat melimpah untuk MRI.",
            "Pada 1,5 Tesla, frekuensi presesi Larmor proton hidrogen sekitar 63,87 MHz."
        )
    ),
    RF_PULSE(
        label = "Pulsa RF 90°/180°",
        badgeColor = CharActionOrange,
        responses = listOf(
            "Pulsa RF 90° merebahkan magnetisasi longitudinal Mz ke bidang transversal Mxy!",
            "Pulsa 180° membalik fase spin agar defasing akibat inhomogenitas B0 kembali bertemu.",
            "Semakin tinggi SAR, semakin hati-hati kita memantau batas pemanasan jaringan pasien."
        )
    ),
    T1_T2_RELAX(
        label = "Relaksasi T1 & T2",
        badgeColor = AmberAccent,
        responses = listOf(
            "T1 adalah pemulihan magnetisasi longitudinal (spin-lattice), sedangkan T2 peluruhan transversal.",
            "Lemak punya T1 pendek sehingga tampak terang di T1W; cairan punya T2 panjang dan terang di T2W.",
            "Atur TR dan TE dengan cermat: TR pendek menonjolkan T1, TE panjang menonjolkan T2!"
        )
    ),
    SPIN_ECHO(
        label = "Sekuens Spin Echo",
        badgeColor = CyanPrimary,
        responses = listOf(
            "Spin Echo memakai pulsa 90° diikuti 180° untuk menghasilkan gema yang tahan inhomogenitas B0.",
            "Pada Fast Spin Echo (FSE/TSE), banyak echo dikumpulkan per TR sehingga waktu scan jauh lebih singkat!",
            "FLAIR menekan sinyal cairan CSF dengan TI khusus agar lesi periventrikel terlihat jelas."
        )
    ),
    K_SPACE(
        label = "Matriks K-space",
        badgeColor = CharAccentCyan,
        responses = listOf(
            "Pusat K-space menyimpan informasi kontras citra, sedangkan tepinya menyimpan detail & tepi tajam!",
            "Setiap baris K-space diisi dengan langkah phase-encoding yang berbeda tiap TR.",
            "Zero-filling memperhalus tampilan piksel, tapi resolusi spasial sejati tetap ditentukan batas K-space."
        )
    ),
    GRADIENT_COIL(
        label = "Koil Gradien X/Y/Z",
        badgeColor = EmeraldSuccess,
        responses = listOf(
            "Gradien Z memilih irisan (slice selection), Y mengatur fase, dan X membaca frekuensi!",
            "Suara ketukan keras saat scan berasal dari gaya Lorentz pada koil gradien.",
            "Slew rate tinggi memungkinkan TE dan TR sangat singkat pada sekuens cepat seperti EPI."
        )
    ),
    CRYO_HELIUM(
        label = "Helium Kriogenik",
        badgeColor = CharAccentTeal,
        responses = listOf(
            "Magnet superkonduktor direndam helium cair bersuhu sekitar 4,2 Kelvin (-269 °C).",
            "Pada suhu kriogenik, hambatan listrik kawat koil menjadi nol sehingga B0 bertahan terus!",
            "Jika terjadi quench darurat, pipa ventilasi menyalurkan gas helium keluar gedung dengan aman."
        )
    ),
    SAFETY_ZONES(
        label = "Zona Keselamatan I–IV",
        badgeColor = RoseDanger,
        responses = listOf(
            "Zona I terbuka untuk umum, sedangkan Zona IV adalah ruang magnet dengan garis 5 Gauss!",
            "Skrining ketat di Zona II wajib dilakukan sebelum pasien melangkah ke Zona III.",
            "Jangan pernah membawa tabung oksigen biasa atau kursi roda besi masuk ke Zona IV!"
        )
    ),
    ARTEFAK_ALIAS(
        label = "Artefak & Koreksi",
        badgeColor = CharActionYellow,
        responses = listOf(
            "Aliasing (wrap-around) muncul saat anatomi melampaui batas FOV pada arah phase-encoding.",
            "Solusi cepat: aktifkan oversampling fase, perbesar FOV, atau tukar arah fase dan frekuensi.",
            "Artefak gerak dapat dikurangi dengan respiratory gating, PROPELLER, atau instruksi napas."
        )
    ),
    QA_PHANTOM(
        label = "Phantom QA ACR",
        badgeColor = CharAccentMint,
        responses = listOf(
            "Phantom QA ACR memeriksa keseragaman intensitas, akurasi ketebalan irisan, dan resolusi tinggi.",
            "Pengukuran SNR rutin memastikan koil penerima dan sistem RF bekerja sesuai standar klinis.",
            "Distorsi geometris pada phantom menandakan perlunya kalibrasi shimming atau gradien."
        )
    ),
    CLINICAL_CONSOLE(
        label = "Konsol Operator",
        badgeColor = CyanBright,
        responses = listOf(
            "Di konsol operator, parameter Geometry, Contrast, dan Sequence diselaraskan untuk tiap pasien.",
            "Selalu periksa perkiraan waktu scan dan nilai SAR sebelum menekan tombol Start Scan!",
            "Gunakan lokalisator 3-bidang untuk merencanakan sudut irisan anatomi secara presisi."
        )
    )
}

/**
 * Ilustrasi vektor gaya Nano yang menyatu langsung dengan latar peta (tanpa bingkai kartu kaku).
 * Pesan edukatif hanya muncul sebagai balon mengambang (Popup) ketika ikon ditekan,
 * sehingga sama sekali tidak mengubah posisi atau susunan simpul peta belajar.
 */
@Composable
fun InteractiveNanoIllustration(
    type: NanoIllustrationType,
    modifier: Modifier = Modifier,
    alignRight: Boolean = false
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var tapCount by remember(type) { mutableIntStateOf(0) }
    var showBubble by remember(type) { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "nano_ill_${type.name}")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val bounceScale by animateFloatAsState(
        targetValue = if (showBubble) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce"
    )

    val activeResponse = type.responses[tapCount % type.responses.size]

    Box(
        modifier = modifier,
        contentAlignment = if (alignRight) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .width(92.dp)
                .semantics {
                    contentDescription = "${type.label}, ketuk untuk respons edukasi Nano"
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button
                ) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    if (!showBubble) {
                        showBubble = true
                    } else {
                        tapCount++
                        if (tapCount % type.responses.size == 0) {
                            showBubble = false
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(74.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pijakan 3D elips berlapis di bawah mini item agar tampak proporsional & menyatu dengan peta
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawOval(
                        color = type.badgeColor.copy(alpha = if (showBubble) 0.30f else 0.16f),
                        topLeft = Offset(size.width * 0.10f, size.height * 0.70f),
                        size = Size(size.width * 0.80f, size.height * 0.24f)
                    )
                    drawOval(
                        color = type.badgeColor.copy(alpha = if (showBubble) 0.55f else 0.28f),
                        topLeft = Offset(size.width * 0.14f, size.height * 0.72f),
                        size = Size(size.width * 0.72f, size.height * 0.20f),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                    drawOval(
                        color = Color.Black.copy(alpha = 0.42f),
                        topLeft = Offset(size.width * 0.20f, size.height * 0.76f),
                        size = Size(size.width * 0.60f, size.height * 0.14f)
                    )
                }
                NanoVectorCanvas(
                    type = type,
                    floatOffset = floatOffset,
                    pulsePhase = pulsePhase,
                    scaleFactor = bounceScale,
                    modifier = Modifier.size(66.dp)
                )
            }
            Text(
                text = type.label,
                color = if (showBubble) type.badgeColor else TextPrimary.copy(alpha = 0.86f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        if (showBubble) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val yOffsetPx = with(density) { (-118).dp.roundToPx() }
            androidx.compose.ui.window.Popup(
                alignment = if (alignRight) Alignment.TopEnd else Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(x = 0, y = yOffsetPx),
                onDismissRequest = { showBubble = false }
            ) {
                IllustrationSpeechBubble(
                    title = type.label,
                    text = activeResponse,
                    stepIndex = (tapCount % type.responses.size) + 1,
                    totalSteps = type.responses.size,
                    accent = type.badgeColor,
                    tailOnRight = alignRight,
                    tailPointingDown = true,
                    onAdvance = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        tapCount++
                    },
                    onClose = { showBubble = false }
                )
            }
        }
    }
}

@Composable
private fun IllustrationSpeechBubble(
    title: String,
    text: String,
    stepIndex: Int,
    totalSteps: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    tailOnRight: Boolean = false,
    tailPointingDown: Boolean = true,
    onAdvance: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .widthIn(min = 152.dp, max = 192.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onAdvance?.invoke()
            },
        horizontalAlignment = if (tailOnRight) Alignment.End else Alignment.Start
    ) {
        // Ekor gelembung awan di bagian atas bila balon muncul di bawah Nano
        if (!tailPointingDown) {
            CloudBubbleTailDots(
                accent = accent,
                tailOnRight = tailOnRight,
                pointingDown = false
            )
        }

        Box {
            // Dekorasi lengkung awan lembut di bagian atas kartu
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                val w = size.width
                drawCircle(
                    color = accent.copy(alpha = 0.20f),
                    radius = 18.dp.toPx(),
                    center = Offset(w * 0.25f, 4.dp.toPx())
                )
                drawCircle(
                    color = accent.copy(alpha = 0.24f),
                    radius = 22.dp.toPx(),
                    center = Offset(w * 0.55f, 2.dp.toPx())
                )
                drawCircle(
                    color = accent.copy(alpha = 0.18f),
                    radius = 16.dp.toPx(),
                    center = Offset(w * 0.80f, 5.dp.toPx())
                )
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomStart = if (tailOnRight) 22.dp else 10.dp,
                    bottomEnd = if (tailOnRight) 10.dp else 22.dp
                ),
                color = Color(0xF50D2235),
                border = BorderStroke(1.8.dp, accent),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "☁ $title",
                            color = accent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "$stepIndex/$totalSteps",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (onClose != null) {
                                Text(
                                    text = "✕",
                                    color = TextPrimary.copy(alpha = 0.75f),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onClose() }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = text,
                        color = TextPrimary,
                        fontSize = 10.2.sp,
                        lineHeight = 13.8.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Ketuk awan → interaksi berikutnya",
                        color = accent.copy(alpha = 0.8f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Ekor gelembung awan (2 bulatan kecil) mengarah ke ikon/Nano di bawahnya
        if (tailPointingDown) {
            CloudBubbleTailDots(
                accent = accent,
                tailOnRight = tailOnRight,
                pointingDown = true
            )
        }
    }
}

@Composable
private fun CloudBubbleTailDots(
    accent: Color,
    tailOnRight: Boolean,
    pointingDown: Boolean
) {
    Canvas(
        modifier = Modifier
            .width(46.dp)
            .height(14.dp)
            .padding(horizontal = 8.dp)
    ) {
        val firstX = if (tailOnRight) size.width * 0.62f else size.width * 0.38f
        val secondX = if (tailOnRight) size.width * 0.78f else size.width * 0.22f
        val firstY = if (pointingDown) size.height * 0.35f else size.height * 0.65f
        val secondY = if (pointingDown) size.height * 0.80f else size.height * 0.20f

        drawCircle(
            color = Color(0xF50D2235),
            radius = 4.2.dp.toPx(),
            center = Offset(firstX, firstY)
        )
        drawCircle(
            color = accent,
            radius = 4.2.dp.toPx(),
            center = Offset(firstX, firstY),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = Color(0xF50D2235),
            radius = 2.6.dp.toPx(),
            center = Offset(secondX, secondY)
        )
        drawCircle(
            color = accent,
            radius = 2.6.dp.toPx(),
            center = Offset(secondX, secondY),
            style = Stroke(width = 1.3.dp.toPx())
        )
    }
}

/**
 * Maskot Nano mengambang (Floating) di atas layar peta belajar.
 * Tidak berada di dalam aliran LazyColumn sehingga tidak menggeser posisi peta,
 * dan balon awan pesannya hanya muncul di atas/samping Nano saat Nano ditekan
 * (tidak pernah menutupi tab navigasi bawah).
 */
@Composable
fun FloatingNanoCompanion(
    characterCustomization: CharacterCustomizationState,
    hints: List<String>,
    modifier: Modifier = Modifier,
    openBubbleUpward: Boolean = true,
    alignStart: Boolean = false
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var tapIndex by remember { mutableIntStateOf(0) }
    var showBubble by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "nano_floating_companion")
    val bobDp by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    val expression = when {
        !showBubble -> Expression.SENANG
        tapIndex % 4 == 1 -> Expression.BANGGA
        tapIndex % 4 == 2 -> Expression.BERPIKIR
        tapIndex % 4 == 3 -> Expression.WASPADA
        else -> Expression.SENANG
    }
    val moodTitles = listOf("Nano Pemandu", "Fakta Fisika Nano", "Tips Klinis Nano", "Tantangan Nano")
    val activeTitle = moodTitles[tapIndex % moodTitles.size]
    val activeHint = hints.getOrElse(tapIndex % hints.size.coerceAtLeast(1)) {
        "Halo! Aku Nano. Ketuk kotak materi atau ilustrasi di peta untuk belajar fisika MRI!"
    }

    Box(
        modifier = modifier,
        contentAlignment = if (alignStart) Alignment.BottomStart else Alignment.TopEnd
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button
                ) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    if (!showBubble) {
                        showBubble = true
                    } else {
                        tapIndex++
                        if (tapIndex % hints.size.coerceAtLeast(1) == 0) {
                            showBubble = false
                        }
                    }
                }
                .semantics { contentDescription = "Nano pendamping mengambang, ketuk untuk petunjuk" }
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CyanBright.copy(alpha = if (showBubble) 0.36f else 0.20f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.48f
                    )
                    drawOval(
                        color = CyanBright.copy(alpha = 0.30f),
                        topLeft = Offset(size.width * 0.18f, size.height * 0.76f),
                        size = Size(size.width * 0.64f, size.height * 0.16f)
                    )
                }
                AvatarView(
                    role = characterCustomization.avatarId,
                    equipped = characterCustomization,
                    expression = expression,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(top = (bobDp.coerceAtLeast(0f)).dp)
                )
            }
        }

        if (showBubble) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val popupOffset = with(density) {
                if (openBubbleUpward) {
                    val xOff = if (alignStart) 6.dp.roundToPx() else (-6).dp.roundToPx()
                    androidx.compose.ui.unit.IntOffset(x = xOff, y = (-126).dp.roundToPx())
                } else {
                    val xOff = if (alignStart) 48.dp.roundToPx() else (-58).dp.roundToPx()
                    androidx.compose.ui.unit.IntOffset(x = xOff, y = 52.dp.roundToPx())
                }
            }
            androidx.compose.ui.window.Popup(
                alignment = if (alignStart) Alignment.TopStart else Alignment.TopEnd,
                offset = popupOffset,
                onDismissRequest = { showBubble = false }
            ) {
                IllustrationSpeechBubble(
                    title = activeTitle,
                    text = activeHint,
                    stepIndex = (tapIndex % hints.size.coerceAtLeast(1)) + 1,
                    totalSteps = hints.size.coerceAtLeast(1),
                    accent = CyanBright,
                    tailOnRight = !alignStart,
                    tailPointingDown = openBubbleUpward,
                    onAdvance = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        tapIndex++
                    },
                    onClose = { showBubble = false }
                )
            }
        }
    }
}

/**
 * Komponen kompatibilitas pemandu Nano (balon hanya muncul saat diketuk).
 */
@Composable
fun InteractiveNanoMapGuide(
    characterCustomization: CharacterCustomizationState,
    hints: List<String>,
    modifier: Modifier = Modifier,
    alignRight: Boolean = true
) {
    FloatingNanoCompanion(
        characterCustomization = characterCustomization,
        hints = hints,
        modifier = modifier,
        alignStart = !alignRight
    )
}

@Composable
fun NanoVectorCanvas(
    type: NanoIllustrationType,
    floatOffset: Float,
    pulsePhase: Float,
    scaleFactor: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + floatOffset
        val r = minOf(w, h) * 0.43f * scaleFactor
        val pulseGlow = 0.5f + 0.5f * sin(pulsePhase * 2f * PI).toFloat()

        // Halo berlapis di belakang setiap mini item agar lebih hidup, 3D, dan kontras
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    type.badgeColor.copy(alpha = 0.24f + 0.12f * pulseGlow),
                    type.badgeColor.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = r * 1.32f
            ),
            radius = r * 1.32f,
            center = Offset(cx, cy)
        )

        when (type) {
            NanoIllustrationType.MRI_SCANNER -> {
                // Dudukan 3D bawah gantry
                drawRoundRect(
                    color = Color(0xFF081B30),
                    topLeft = Offset(cx - r * 1.08f, cy - r * 0.80f),
                    size = Size(r * 2.16f, r * 1.92f),
                    cornerRadius = CornerRadius(r * 0.48f, r * 0.48f)
                )
                // Bodi utama gantry superkonduktor 3D
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF286496), Color(0xFF102A45))
                    ),
                    topLeft = Offset(cx - r * 1.05f, cy - r * 0.90f),
                    size = Size(r * 2.10f, r * 1.84f),
                    cornerRadius = CornerRadius(r * 0.45f, r * 0.45f)
                )
                drawRoundRect(
                    color = CyanBright,
                    topLeft = Offset(cx - r * 1.05f, cy - r * 0.90f),
                    size = Size(r * 2.10f, r * 1.84f),
                    cornerRadius = CornerRadius(r * 0.45f, r * 0.45f),
                    style = Stroke(width = 3.5f)
                )
                // Cincin magnet dalam & terowongan bore dengan garis medan B0 dinamis
                drawCircle(
                    color = Color(0xFF051120),
                    radius = r * 0.60f,
                    center = Offset(cx, cy - r * 0.05f)
                )
                drawCircle(
                    color = CyanGlow.copy(alpha = 0.65f + 0.35f * pulseGlow),
                    radius = r * 0.60f,
                    center = Offset(cx, cy - r * 0.05f),
                    style = Stroke(width = 3.4f)
                )
                drawArc(
                    color = AmberAccent.copy(alpha = 0.85f),
                    startAngle = pulsePhase * 360f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.44f, cy - r * 0.49f),
                    size = Size(r * 0.88f, r * 0.88f),
                    style = Stroke(width = 2.4f, cap = StrokeCap.Round)
                )
                // Meja pasien isocenter 3D
                drawRoundRect(
                    color = CharAccentTeal,
                    topLeft = Offset(cx - r * 0.48f, cy + r * 0.22f),
                    size = Size(r * 0.96f, r * 0.30f),
                    cornerRadius = CornerRadius(7f, 7f)
                )
                // Indikator LED panel gantry
                drawCircle(EmeraldSuccess, radius = 3.2f, center = Offset(cx - r * 0.68f, cy - r * 0.56f))
                drawCircle(CyanBright, radius = 3.2f, center = Offset(cx - r * 0.48f, cy - r * 0.56f))
                drawCircle(AmberAccent, radius = 2.8f, center = Offset(cx + r * 0.62f, cy - r * 0.56f))
            }

            NanoIllustrationType.MRI_BRAIN_PHOTO -> {
                drawRoundRect(
                    color = Color(0xFF071320),
                    topLeft = Offset(cx - r * 0.96f, cy - r * 0.96f),
                    size = Size(r * 1.92f, r * 1.92f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = CharAccentTeal,
                    topLeft = Offset(cx - r * 0.96f, cy - r * 0.96f),
                    size = Size(r * 1.92f, r * 1.92f),
                    cornerRadius = CornerRadius(14f, 14f),
                    style = Stroke(width = 3f)
                )
                drawOval(
                    color = Color(0xFF1D4B73),
                    topLeft = Offset(cx - r * 0.64f, cy - r * 0.74f),
                    size = Size(r * 1.28f, r * 1.48f)
                )
                drawOval(
                    color = CharAccentTeal.copy(alpha = 0.92f),
                    topLeft = Offset(cx - r * 0.54f, cy - r * 0.62f),
                    size = Size(r * 1.08f, r * 1.24f),
                    style = Stroke(width = 2.6f)
                )
                drawLine(
                    color = Color(0xFF7ADCF2),
                    start = Offset(cx, cy - r * 0.58f),
                    end = Offset(cx, cy + r * 0.58f),
                    strokeWidth = 2.4f
                )
                drawOval(
                    color = CyanGlow.copy(alpha = 0.82f),
                    topLeft = Offset(cx - r * 0.26f, cy - r * 0.18f),
                    size = Size(r * 0.21f, r * 0.36f)
                )
                drawOval(
                    color = CyanGlow.copy(alpha = 0.82f),
                    topLeft = Offset(cx + r * 0.05f, cy - r * 0.18f),
                    size = Size(r * 0.21f, r * 0.36f)
                )
            }

            NanoIllustrationType.HYDROGEN_ATOM -> {
                val angle = pulsePhase * 2f * PI.toFloat()
                drawOval(
                    color = CharAccentMint.copy(alpha = 0.85f),
                    topLeft = Offset(cx - r * 0.96f, cy - r * 0.40f),
                    size = Size(r * 1.92f, r * 0.80f),
                    style = Stroke(width = 2.6f)
                )
                drawOval(
                    color = CyanBright.copy(alpha = 0.78f),
                    topLeft = Offset(cx - r * 0.40f, cy - r * 0.96f),
                    size = Size(r * 0.80f, r * 1.92f),
                    style = Stroke(width = 2.6f)
                )
                // Kerucut presesi Larmor
                drawOval(
                    color = AmberAccent.copy(alpha = 0.65f),
                    topLeft = Offset(cx - r * 0.44f, cy - r * 0.82f),
                    size = Size(r * 0.88f, r * 0.28f),
                    style = Stroke(width = 2f)
                )
                val tipX = cx + cos(angle) * r * 0.40f
                val tipY = cy - r * 0.68f + sin(angle) * r * 0.12f
                drawLine(
                    color = AmberAccent,
                    start = Offset(cx, cy + r * 0.62f),
                    end = Offset(tipX, tipY),
                    strokeWidth = 3.4f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = CharAccentMint, radius = r * 0.38f, center = Offset(cx, cy))
                drawCircle(color = Color.White.copy(alpha = 0.72f), radius = r * 0.13f, center = Offset(cx - r * 0.10f, cy - r * 0.10f))
                val ex = cx + cos(angle) * r * 0.92f
                val ey = cy + sin(angle) * r * 0.36f
                drawCircle(color = CyanGlow, radius = 5.4f, center = Offset(ex, ey))
            }

            NanoIllustrationType.RF_PULSE -> {
                drawRoundRect(
                    color = Color(0xFF10243A),
                    topLeft = Offset(cx - r * 0.98f, cy - r * 0.80f),
                    size = Size(r * 1.96f, r * 1.60f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = CharActionOrange.copy(alpha = 0.65f),
                    topLeft = Offset(cx - r * 0.98f, cy - r * 0.80f),
                    size = Size(r * 1.96f, r * 1.60f),
                    cornerRadius = CornerRadius(14f, 14f),
                    style = Stroke(width = 2.4f)
                )
                val path = Path()
                val steps = 40
                for (i in 0..steps) {
                    val t = i / steps.toFloat()
                    val x = (cx - r * 0.86f) + t * r * 1.72f
                    val env = 1f - kotlin.math.abs(t - 0.5f) * 1.75f
                    val y = cy - sin((t * 5f + pulsePhase) * 2f * PI).toFloat() * r * 0.62f * env.coerceAtLeast(0.15f)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = CharActionOrange, style = Stroke(width = 3.8f, cap = StrokeCap.Round))
                drawCircle(color = CharActionYellow, radius = 4.8f, center = Offset(cx, cy - r * 0.60f))
            }

            NanoIllustrationType.T1_T2_RELAX -> {
                drawRoundRect(
                    color = Color(0xFF0C2034),
                    topLeft = Offset(cx - r * 0.98f, cy - r * 0.86f),
                    size = Size(r * 1.96f, r * 1.72f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = AmberAccent.copy(alpha = 0.55f),
                    topLeft = Offset(cx - r * 0.98f, cy - r * 0.86f),
                    size = Size(r * 1.96f, r * 1.72f),
                    cornerRadius = CornerRadius(14f, 14f),
                    style = Stroke(width = 2.4f)
                )
                drawLine(TextSecondary, Offset(cx - r * 0.80f, cy + r * 0.64f), Offset(cx + r * 0.84f, cy + r * 0.64f), 2.4f)
                drawLine(TextSecondary, Offset(cx - r * 0.80f, cy + r * 0.64f), Offset(cx - r * 0.80f, cy - r * 0.68f), 2.4f)
                val t1 = Path().apply {
                    moveTo(cx - r * 0.78f, cy + r * 0.60f)
                    cubicTo(cx - r * 0.35f, cy - r * 0.25f, cx + r * 0.15f, cy - r * 0.55f, cx + r * 0.80f, cy - r * 0.60f)
                }
                drawPath(t1, color = AmberAccent, style = Stroke(width = 3.8f, cap = StrokeCap.Round))
                val t2 = Path().apply {
                    moveTo(cx - r * 0.78f, cy - r * 0.56f)
                    cubicTo(cx - r * 0.30f, cy - r * 0.30f, cx + r * 0.10f, cy + r * 0.44f, cx + r * 0.80f, cy + r * 0.56f)
                }
                drawPath(t2, color = CyanBright, style = Stroke(width = 3.8f, cap = StrokeCap.Round))
                // Titik penanda bergerak di sepanjang kurva T1 & T2
                val dotX = (cx - r * 0.78f) + pulsePhase * (r * 1.58f)
                val t1Y = cy + r * 0.60f - (1f - (1f - pulsePhase) * (1f - pulsePhase)) * (r * 1.18f)
                val t2Y = cy - r * 0.56f + (1f - (1f - pulsePhase) * (1f - pulsePhase)) * (r * 1.12f)
                drawCircle(AmberAccent, radius = 4.5f, center = Offset(dotX, t1Y))
                drawCircle(CyanBright, radius = 4.5f, center = Offset(dotX, t2Y))
            }

            NanoIllustrationType.SPIN_ECHO -> {
                val base = cy + r * 0.55f
                drawRoundRect(
                    color = Color(0xFF0C2034),
                    topLeft = Offset(cx - r * 0.98f, cy - r * 0.84f),
                    size = Size(r * 1.96f, r * 1.64f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = CyanPrimary.copy(alpha = 0.60f),
                    topLeft = Offset(cx - r * 0.98f, cy - r * 0.84f),
                    size = Size(r * 1.96f, r * 1.64f),
                    cornerRadius = CornerRadius(14f, 14f),
                    style = Stroke(width = 2.4f)
                )
                drawLine(TextSecondary, Offset(cx - r * 0.86f, base), Offset(cx + r * 0.86f, base), 2.4f)
                drawRoundRect(CyanBright, Offset(cx - r * 0.74f, base - r * 0.78f), Size(r * 0.26f, r * 0.78f), CornerRadius(5f, 5f))
                drawRoundRect(CharActionOrange, Offset(cx - r * 0.18f, base - r * 1.16f), Size(r * 0.30f, r * 1.16f), CornerRadius(5f, 5f))
                val echo = Path().apply {
                    moveTo(cx + r * 0.22f, base)
                    quadraticTo(cx + r * 0.54f, base - r * (0.95f + 0.25f * pulseGlow), cx + r * 0.84f, base)
                }
                drawPath(echo, color = EmeraldSuccess, style = Stroke(width = 4.0f, cap = StrokeCap.Round))
            }

            NanoIllustrationType.K_SPACE -> {
                val cell = r * 0.43f
                val startX = cx - cell * 2f
                val startY = cy - cell * 2f
                drawRoundRect(
                    color = Color(0xFF08182B),
                    topLeft = Offset(startX - 5f, startY - 5f),
                    size = Size(cell * 4f + 10f, cell * 4f + 10f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                val activeRow = ((pulsePhase * 4f).toInt()).coerceIn(0, 3)
                for (row in 0..3) {
                    for (col in 0..3) {
                        val isCenter = (row in 1..2) && (col in 1..2)
                        val isScanned = row == activeRow
                        drawRoundRect(
                            color = when {
                                isCenter -> CyanBright
                                isScanned -> CharAccentMint.copy(alpha = 0.75f)
                                else -> Color(0xFF194163)
                            },
                            topLeft = Offset(startX + col * cell + 2f, startY + row * cell + 2f),
                            size = Size(cell - 4f, cell - 4f),
                            cornerRadius = CornerRadius(4.5f, 4.5f)
                        )
                    }
                }
                drawRoundRect(
                    color = CharAccentMint,
                    topLeft = Offset(startX + cell, startY + cell),
                    size = Size(cell * 2f, cell * 2f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 2.5f)
                )
            }

            NanoIllustrationType.GRADIENT_COIL -> {
                drawCircle(Color(0xFF0D2638), radius = r * 0.90f, center = Offset(cx, cy))
                drawCircle(EmeraldSuccess, radius = r * 0.90f, center = Offset(cx, cy), style = Stroke(width = 3.2f))
                drawLine(CyanBright, Offset(cx - r * 0.70f, cy), Offset(cx + r * 0.70f, cy), 3.6f, cap = StrokeCap.Round)
                drawLine(AmberAccent, Offset(cx, cy - r * 0.70f), Offset(cx, cy + r * 0.70f), 3.6f, cap = StrokeCap.Round)
                drawLine(CharActionOrange, Offset(cx - r * 0.50f, cy + r * 0.50f), Offset(cx + r * 0.50f, cy - r * 0.50f), 3.6f, cap = StrokeCap.Round)
                drawCircle(Color.White, radius = 4f, center = Offset(cx, cy))
            }

            NanoIllustrationType.CRYO_HELIUM -> {
                drawRoundRect(Color(0xFF164266), Offset(cx - r * 0.64f, cy - r * 0.88f), Size(r * 1.28f, r * 1.76f), CornerRadius(14f, 14f))
                drawRoundRect(CharAccentTeal, Offset(cx - r * 0.64f, cy - r * 0.88f), Size(r * 1.28f, r * 1.76f), CornerRadius(14f, 14f), style = Stroke(3.2f))
                for (i in 0..2) {
                    val ang = i * (PI / 3).toFloat()
                    drawLine(
                        Color(0xFFA7F3D0),
                        Offset(cx - cos(ang) * r * 0.40f, cy - sin(ang) * r * 0.40f),
                        Offset(cx + cos(ang) * r * 0.40f, cy + sin(ang) * r * 0.40f),
                        3.2f,
                        StrokeCap.Round
                    )
                }
            }

            NanoIllustrationType.SAFETY_ZONES -> {
                drawCircle(RoseDanger.copy(alpha = 0.26f), r * 0.95f, Offset(cx, cy))
                drawCircle(RoseDanger, r * 0.95f, Offset(cx, cy), style = Stroke(3f))
                drawCircle(AmberWarning, r * 0.65f, Offset(cx, cy), style = Stroke(3f))
                drawCircle(CyanBright, r * 0.36f, Offset(cx, cy), style = Stroke(3f))
                drawCircle(RoseDanger, r * (0.14f + 0.05f * pulseGlow), Offset(cx, cy))
            }

            NanoIllustrationType.ARTEFAK_ALIAS -> {
                drawRoundRect(Color(0xFF142E48), Offset(cx - r * 0.90f, cy - r * 0.74f), Size(r * 1.80f, r * 1.48f), CornerRadius(12f, 12f))
                drawRoundRect(CharActionYellow.copy(alpha = 0.65f), Offset(cx - r * 0.90f, cy - r * 0.74f), Size(r * 1.80f, r * 1.48f), CornerRadius(12f, 12f), style = Stroke(2.4f))
                drawOval(CharActionYellow.copy(alpha = 0.92f), Offset(cx - r * 0.36f, cy - r * 0.46f), Size(r * 0.72f, r * 0.92f), style = Stroke(3.2f))
                drawArc(RoseDanger, -90f, 180f, false, Offset(cx - r * 1.05f, cy - r * 0.35f), Size(r * 0.50f, r * 0.70f), style = Stroke(3.4f))
                drawArc(RoseDanger, 90f, 180f, false, Offset(cx + r * 0.55f, cy - r * 0.35f), Size(r * 0.50f, r * 0.70f), style = Stroke(3.4f))
            }

            NanoIllustrationType.QA_PHANTOM -> {
                drawCircle(Color(0xFF143A59), r * 0.90f, Offset(cx, cy))
                drawCircle(CharAccentMint, r * 0.90f, Offset(cx, cy), style = Stroke(3.4f))
                drawRect(CyanBright.copy(alpha = 0.70f), Offset(cx - r * 0.42f, cy - r * 0.42f), Size(r * 0.84f, r * 0.84f), style = Stroke(2.5f))
                drawCircle(AmberAccent, r * 0.14f, Offset(cx - r * 0.22f, cy - r * 0.22f))
                drawCircle(AmberAccent, r * 0.10f, Offset(cx + r * 0.22f, cy - r * 0.22f))
                drawCircle(EmeraldSuccess, r * 0.12f, Offset(cx, cy + r * 0.22f))
            }

            NanoIllustrationType.CLINICAL_CONSOLE -> {
                drawRoundRect(Color(0xFF102A44), Offset(cx - r * 0.96f, cy - r * 0.76f), Size(r * 1.92f, r * 1.28f), CornerRadius(12f, 12f))
                drawRoundRect(CyanBright, Offset(cx - r * 0.96f, cy - r * 0.76f), Size(r * 1.92f, r * 1.28f), CornerRadius(12f, 12f), style = Stroke(3f))
                drawLine(EmeraldSuccess, Offset(cx - r * 0.70f, cy - r * 0.35f), Offset(cx + r * 0.24f, cy - r * 0.35f), 4f, StrokeCap.Round)
                drawLine(AmberAccent, Offset(cx - r * 0.70f, cy - r * 0.05f), Offset(cx + r * 0.58f, cy - r * 0.05f), 4f, StrokeCap.Round)
                drawLine(CyanGlow, Offset(cx - r * 0.70f, cy + r * 0.22f), Offset(cx + r * 0.10f, cy + r * 0.22f), 4f, StrokeCap.Round)
                drawLine(TextSecondary, Offset(cx, cy + r * 0.54f), Offset(cx, cy + r * 0.84f), 4f, StrokeCap.Round)
                drawLine(TextSecondary, Offset(cx - r * 0.46f, cy + r * 0.84f), Offset(cx + r * 0.46f, cy + r * 0.84f), 4f, StrokeCap.Round)
            }
        }
    }
}
