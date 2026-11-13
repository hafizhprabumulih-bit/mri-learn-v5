package com.example.ui.character

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CharacterCatalog
import com.example.data.CharacterCategory
import com.example.data.CharacterCustomizationState
import com.example.data.MriRepository
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharRoomBg
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ItemUnlockState
import com.example.util.UnlockProgress
import com.example.viewmodel.MriUiState
import com.example.viewmodel.MriViewModel
import kotlinx.coroutines.delay

@Composable
fun CharacterRoomScreen(
    state: MriUiState,
    viewModel: MriViewModel
) {
    // Gunakan rememberSaveable per field primitif agar tahan rotasi layar tanpa membebani Bundle
    val saved = state.characterCustomization
    val sanitizedSavedHair = if (saved.equippedHair == "hair_cap") "hair_default" else saved.equippedHair
    var avatarId by rememberSaveable(saved.avatarId) { mutableStateOf(saved.avatarId) }
    var equippedHair by rememberSaveable(sanitizedSavedHair) { mutableStateOf(sanitizedSavedHair) }
    var equippedUniform by rememberSaveable(saved.equippedUniform) { mutableStateOf(saved.equippedUniform) }
    var equippedHeadset by rememberSaveable(saved.equippedHeadset) { mutableStateOf(saved.equippedHeadset) }
    var equippedAccessory by rememberSaveable(saved.equippedAccessory) { mutableStateOf(saved.equippedAccessory) }
    var backgroundId by rememberSaveable(saved.backgroundId) { mutableStateOf(saved.backgroundId) }

    val workingState = remember(
        avatarId,
        equippedHair,
        equippedUniform,
        equippedHeadset,
        equippedAccessory,
        backgroundId
    ) {
        CharacterCustomizationState(
            avatarId = avatarId,
            equippedHair = equippedHair,
            equippedUniform = equippedUniform,
            equippedHeadset = equippedHeadset,
            equippedAccessory = equippedAccessory,
            backgroundId = backgroundId
        )
    }

    val hasUnsavedChanges = workingState != state.characterCustomization

    var selectedCategory by rememberSaveable { mutableStateOf(CharacterCategory.RAMBUT) }
    var interactionCount by rememberSaveable { mutableIntStateOf(0) }
    var greetingIndex by rememberSaveable { mutableIntStateOf(0) }
    var customBubbleText by rememberSaveable { mutableStateOf<String?>(null) }
    var isJumping by remember { mutableStateOf(false) }
    var isWaveActive by rememberSaveable { mutableStateOf(false) }

    // State Interaksi (Body Zones, Rub, Double-Tap Larmor Spin, Shift Card, Studio)
    var forcedExpression by remember { mutableStateOf<Expression?>(null) }
    var larmorSpinTrigger by remember { mutableIntStateOf(0) }
    var heartBurstTrigger by remember { mutableIntStateOf(0) }
    var shiftMood by rememberSaveable { mutableIntStateOf(3) }
    var showShiftDialog by rememberSaveable { mutableStateOf(false) }
    var showSnapshotStudio by rememberSaveable { mutableStateOf(false) }

    val avatars = MriRepository.avatars
    val currentAvatarIndex = avatars.indexOfFirst { it.id == workingState.avatarId }.coerceAtLeast(0)
    val currentAvatar = avatars[currentAvatarIndex]
    val specialistProfile = CharacterCatalog.getSpecialistProfile(workingState.avatarId)
    val accentPalette = CharacterCatalog.getAccentPalette("cyan")
    val pathHolder = com.example.ui.path.rememberModulePathStateHolder()
    val todayKey = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }
    val dailyCounts by pathHolder.observeDailyShiftCounts(todayKey).collectAsState(initial = Triple(0, 0, 0))
    val dailyShift = remember(dailyCounts) {
        com.example.util.DailyShiftMission.evaluate(dailyCounts.first, dailyCounts.second, dailyCounts.third)
    }
    val arcadeStats by pathHolder.arcadeStatsFlow.collectAsState(initial = com.example.data.ArcadeStats())

    LaunchedEffect(isJumping) {
        if (isJumping) {
            delay(360)
            isJumping = false
        }
    }

    LaunchedEffect(forcedExpression, interactionCount) {
        if (forcedExpression != null) {
            delay(2200)
            forcedExpression = null
        }
    }

    val haptic = LocalHapticFeedback.current

    fun triggerAvatarTap(
        customMessage: String? = null,
        hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
        expression: Expression? = null
    ) {
        haptic.performHapticFeedback(hapticType)
        interactionCount++
        isJumping = true
        forcedExpression = expression
        if (customMessage != null) {
            customBubbleText = customMessage
        } else {
            customBubbleText = null
            greetingIndex = (greetingIndex + 1) % specialistProfile.greetings.size
        }
    }

    fun applyRoleWithDefaultLoadout(newAvatarId: String) {
        val roleLoadout = AvatarLayerRegistry.defaultLoadoutForRole(
            avatarId = newAvatarId,
            currentBackgroundId = backgroundId
        )
        avatarId = roleLoadout.avatarId
        equippedHair = roleLoadout.equippedHair
        equippedUniform = roleLoadout.equippedUniform
        equippedHeadset = roleLoadout.equippedHeadset
        equippedAccessory = roleLoadout.equippedAccessory
    }

    val displayedBubbleText = customBubbleText
        ?: if (interactionCount == 0 && state.studyStreakDays == 0) {
            "Streak belajar belum aktif hari ini! Yuk selesaikan 1 simpul agar koherensi belajar terjaga."
        } else {
            specialistProfile.greetings.getOrElse(greetingIndex % specialistProfile.greetings.size) {
                specialistProfile.greetings.first()
            }
        }
    val resetMsg = stringResource(R.string.char_room_reset_msg)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharRoomBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // 1. Panggung Interaktif Ruang Karakter
            InteractiveCharacterStage(
                workingState = workingState,
                currentAvatar = currentAvatar,
                specialistRole = specialistProfile.roleTitle,
                favoriteSequence = specialistProfile.favoriteSequence,
                userName = state.userName.ifBlank { currentAvatar.label },
                accentPalette = accentPalette,
                bubbleText = displayedBubbleText,
                isJumping = isJumping,
                isWaveActive = isWaveActive,
                interactionCount = interactionCount,
                hasUnsavedChanges = hasUnsavedChanges,
                studyStreakDays = state.studyStreakDays,
                shiftMood = shiftMood,
                forcedExpression = forcedExpression,
                larmorSpinTrigger = larmorSpinTrigger,
                heartBurstTrigger = heartBurstTrigger,
                onOpenSnapshotStudio = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showSnapshotStudio = true
                },
                onToggleWave = {
                    isWaveActive = !isWaveActive
                    val waveComment = when (workingState.backgroundId) {
                        "bg_led_stage" -> if (isWaveActive) "Gelombang RF menyala! Lintasan spiral & radial K-space sedang dipindai!" else null
                        "bg_lecture_hall" -> if (isWaveActive) "Demonstrasi gelombang pulsa 90° di papan kuliah siap diamati!" else null
                        "bg_simulation_center" -> if (isWaveActive) "Simulasi rekonstruksi sinyal RF & gradien pusat riset aktif!" else null
                        else -> if (isWaveActive) "Pulsa RF 90° aktif — vektor Mxy beresonansi pada frekuensi Larmor!" else null
                    }
                    if (waveComment != null) {
                        triggerAvatarTap(waveComment, expression = Expression.BERPIKIR)
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                onReset = {
                    val def = CharacterCustomizationState()
                    avatarId = def.avatarId
                    equippedHair = def.equippedHair
                    equippedUniform = def.equippedUniform
                    equippedHeadset = def.equippedHeadset
                    equippedAccessory = def.equippedAccessory
                    backgroundId = def.backgroundId
                    viewModel.resetCharacterCustomization()
                    triggerAvatarTap(resetMsg, HapticFeedbackType.LongPress, Expression.SEDIH)
                },
                onSave = {
                    viewModel.updateCharacterCustomization(workingState)
                    triggerAvatarTap(
                        "Kustomisasi karakter berhasil disimpan!",
                        HapticFeedbackType.LongPress,
                        Expression.BANGGA
                    )
                },
                onAvatarTap = { triggerAvatarTap() },
                onBodyZoneTap = { zone ->
                    when (zone) {
                        NanoBodyZone.HEAD -> {
                            val headItem = CharacterCatalog.getItem(workingState.equippedHair)
                            triggerAvatarTap(
                                customMessage = headItem?.let { "${it.name}: ${it.clinicalBenefit}." }
                                    ?: "Visor K-Space Nano dalam kondisi optimal!",
                                expression = if (interactionCount % 2 == 0) Expression.SENANG else Expression.BERPIKIR
                            )
                        }
                        NanoBodyZone.TOOL -> {
                            val acc = CharacterCatalog.getItem(workingState.equippedAccessory)
                            val toolMsg = when (workingState.equippedAccessory) {
                                "acc_magnet_detector" -> "Detektor Feromagnetik aktif: pembacaan medan tepi < 5 Gauss aman!"
                                "acc_clipboard" -> "Data konsol cepat: T1w (TR 500ms, TE 15ms) | T2w (TR 3500ms, TE 100ms)."
                                "acc_phantom" -> "Kalibrasi SNR fantom ACR selesai: homogenitas medan B0 terjaga!"
                                "acc_laser_pointer" -> "Pointer presentasi siap menyorot kurva relaksasi & spektrum K-Space!"
                                "acc_handbook_mri" -> "Buku Saku MRI: γ(¹H) = 42,58 MHz/T · Aturan ACR Zona I–IV selalu siap."
                                else -> acc?.let { "${it.name}: ${it.clinicalBenefit}." } ?: "Alat klinis siap digunakan!"
                            }
                            triggerAvatarTap(toolMsg, HapticFeedbackType.LongPress, Expression.BANGGA)
                        }
                        NanoBodyZone.TORSO -> {
                            val uni = CharacterCatalog.getItem(workingState.equippedUniform)
                            val zoneSuitability = when {
                                workingState.backgroundId == "bg_chiller_room" && workingState.equippedUniform != "uniform_armor" ->
                                    "Peringatan M05/M07B: Di Ruang Chiller 4,2K butuh Suit Kryogenik untuk bahaya helium cair!"
                                workingState.backgroundId == "bg_scanner_suite" && workingState.equippedHeadset == "headset_none" ->
                                    "Peringatan M07C: Di Ruang 3T Zona IV wajib memakai pelindung pendengaran terhadap bising gradien!"
                                else -> uni?.let { "${it.name}: ${it.clinicalBenefit}." } ?: "Seragam bebas komponen feromagnetik!"
                            }
                            triggerAvatarTap(
                                customMessage = zoneSuitability,
                                expression = if (zoneSuitability.startsWith("Peringatan")) Expression.WASPADA else Expression.SENANG
                            )
                        }
                    }
                },
                onDoubleTapLarmorSpin = {
                    larmorSpinTrigger++
                    triggerAvatarTap(
                        customMessage = "Presesi Larmor 63,87 MHz aktif! Spin fase 360° selaras!",
                        hapticType = HapticFeedbackType.LongPress,
                        expression = Expression.BANGGA
                    )
                },
                onLongPressShiftCard = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showShiftDialog = true
                },
                onRubHead = {
                    heartBurstTrigger++
                    shiftMood++
                    triggerAvatarTap(
                        customMessage = "Nyaman sekali! Semangat kerja shift naik ♥ (Mood Shift: $shiftMood)",
                        expression = Expression.SENANG
                    )
                },
                onPrevAvatar = {
                    val prevIndex = if (currentAvatarIndex - 1 < 0) avatars.lastIndex else currentAvatarIndex - 1
                    val newAv = avatars[prevIndex]
                    applyRoleWithDefaultLoadout(newAv.id)
                    val prof = CharacterCatalog.getSpecialistProfile(newAv.id)
                    triggerAvatarTap("${newAv.label} siap bertugas: ${prof.zoneSpecialty}!")
                },
                onNextAvatar = {
                    val nextIndex = (currentAvatarIndex + 1) % avatars.size
                    val newAv = avatars[nextIndex]
                    applyRoleWithDefaultLoadout(newAv.id)
                    val prof = CharacterCatalog.getSpecialistProfile(newAv.id)
                    triggerAvatarTap("${newAv.label} siap bertugas: ${prof.zoneSpecialty}!")
                }
            )

            Spacer(modifier = Modifier.height(7.dp))

            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = com.example.ui.theme.CharCardSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (dailyShift.isAllComplete) com.example.ui.theme.CharAccentMint else com.example.ui.theme.CharBorderSlate
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            androidx.compose.material3.Text(
                                text = "Shift Nano Hari Ini (${dailyShift.completedCount}/${dailyShift.totalCount})",
                                color = com.example.ui.theme.CharTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            androidx.compose.material3.Text(
                                text = "Pangkat Arcade: ${arcadeStats.highestRank}",
                                color = com.example.ui.theme.CharTextDim,
                                fontSize = 10.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (dailyShift.isAllComplete) {
                                com.example.ui.theme.CharAccentMint.copy(alpha = 0.2f)
                            } else {
                                com.example.ui.theme.CharAccentCyan.copy(alpha = 0.16f)
                            }
                        ) {
                            androidx.compose.material3.Text(
                                text = "+${dailyShift.bonusXpEarned} XP",
                                color = if (dailyShift.isAllComplete) com.example.ui.theme.CharAccentMint else com.example.ui.theme.CharAccentCyan,
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("📘 Materi", "${dailyCounts.first.coerceAtMost(1)}/1", dailyCounts.first >= 1),
                            Triple("⚡ Kuis", "${dailyCounts.second.coerceAtMost(5)}/5", dailyCounts.second >= 5),
                            Triple("🎛️ Lab/Game", "${dailyCounts.third.coerceAtMost(1)}/1", dailyCounts.third >= 1)
                        ).forEach { (label, prog, isDone) ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDone) com.example.ui.theme.CharCardSelected else com.example.ui.theme.CharRoomBg.copy(alpha = 0.65f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDone) com.example.ui.theme.CharAccentMint.copy(alpha = 0.7f) else com.example.ui.theme.CharBorderSlate
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDone) com.example.ui.theme.CharTextPrimary else com.example.ui.theme.CharTextDim,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (isDone) "✓" else prog,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDone) com.example.ui.theme.CharAccentMint else com.example.ui.theme.CharAccentCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Navigasi Tab Segmen (Karakter | Tampilan | Latar) + Sub-Tab
            CharacterCategoryTabs(
                selectedCategory = selectedCategory,
                accentPalette = accentPalette,
                onSelectCategory = { category ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedCategory = category
                }
            )

            Spacer(modifier = Modifier.height(7.dp))

            // 3. Pilihan Item yang Sliding ke Samping (LazyRow Horizontal) dalam Column agar tidak tumpang-tindih
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 144.dp)
            ) {
                if (selectedCategory == CharacterCategory.KARAKTER) {
                    SpecialistAvatarGrid(
                        avatars = avatars,
                        selectedAvatarId = workingState.avatarId,
                        accentPalette = accentPalette,
                        onSelectAvatar = { avatar ->
                            applyRoleWithDefaultLoadout(avatar.id)
                            val prof = CharacterCatalog.getSpecialistProfile(avatar.id)
                            triggerAvatarTap("Halo! Saya ${prof.roleTitle}. Sekuens favorit: ${prof.favoriteSequence}.")
                        }
                    )
                } else {
                    val currentItems = when (selectedCategory) {
                        CharacterCategory.KARAKTER -> emptyList()
                        CharacterCategory.RAMBUT -> CharacterCatalog.hairItems
                        CharacterCategory.SERAGAM -> CharacterCatalog.uniformItems
                        CharacterCategory.HEADSET -> CharacterCatalog.headsetItems
                        CharacterCategory.AKSESORIS -> CharacterCatalog.accessoryItems
                        CharacterCategory.LATAR -> CharacterCatalog.backgroundItems
                    }

                    val equippedIds = remember(workingState, saved) {
                        setOf(
                            workingState.equippedHair,
                            workingState.equippedUniform,
                            workingState.equippedHeadset,
                            workingState.equippedAccessory,
                            workingState.backgroundId,
                            saved.equippedHair,
                            saved.equippedUniform,
                            saved.equippedHeadset,
                            saved.equippedAccessory,
                            saved.backgroundId
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(currentItems, key = { it.id }) { item ->
                            val isSelected = when (selectedCategory) {
                                CharacterCategory.KARAKTER -> false
                                CharacterCategory.RAMBUT -> workingState.equippedHair == item.id
                                CharacterCategory.SERAGAM -> workingState.equippedUniform == item.id
                                CharacterCategory.HEADSET -> workingState.equippedHeadset == item.id
                                CharacterCategory.AKSESORIS -> workingState.equippedAccessory == item.id
                                CharacterCategory.LATAR -> workingState.backgroundId == item.id
                            }
                            val unlockState = UnlockProgress.evaluate(
                                item = item,
                                modules = state.modulesList,
                                completedNodeCodes = state.completedNodeCodes,
                                equippedItemIds = equippedIds
                            )
                            val isUnlocked = unlockState is ItemUnlockState.Unlocked
                            val unlockProgress = if (unlockState is ItemUnlockState.Locked) unlockState.progress else 1f
                            val rewardHint = (unlockState as? ItemUnlockState.Unlocked)?.rewardHint
                            val requirementLabel = (unlockState as? ItemUnlockState.Locked)?.requirementLabel

                            CharacterItemCard(
                                item = item,
                                isSelected = isSelected,
                                isUnlocked = isUnlocked,
                                accentPalette = accentPalette,
                                unlockProgress = unlockProgress,
                                rewardHint = rewardHint,
                                requirementLabel = requirementLabel,
                                onClick = {
                                    if (unlockState is ItemUnlockState.Locked) {
                                        triggerAvatarTap("${item.name} terkunci. ${unlockState.requirementLabel} untuk membukanya.")
                                    } else {
                                        when (selectedCategory) {
                                            CharacterCategory.KARAKTER -> Unit
                                            CharacterCategory.RAMBUT -> equippedHair = item.id
                                            CharacterCategory.SERAGAM -> equippedUniform = item.id
                                            CharacterCategory.HEADSET -> equippedHeadset = item.id
                                            CharacterCategory.AKSESORIS -> equippedAccessory = item.id
                                            CharacterCategory.LATAR -> backgroundId = item.id
                                        }
                                        triggerAvatarTap("${item.name} dipasang! ${item.clinicalBenefit}")
                                    }
                                }
                            )
                        }
                    }

                    val closestTip = remember(
                        state.modulesList,
                        state.completedNodeCodes,
                        equippedIds
                    ) {
                        UnlockProgress.closestLockedItem(
                            items = CharacterCatalog.allItems,
                            modules = state.modulesList,
                            completedNodeCodes = state.completedNodeCodes,
                            equippedItemIds = equippedIds
                        )
                    }
                    if (closestTip != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val locked = closestTip.state
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = AmberWarning.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${closestTip.item.name} terkunci",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${locked.moduleId}: ${locked.completedNodes}/${locked.requiredNodes} materi selesai",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                                val targetModule = state.modulesList.find {
                                    it.id.equals(locked.moduleId, ignoreCase = true) ||
                                        it.code.equals(locked.moduleId, ignoreCase = true)
                                }
                                if (targetModule != null) {
                                    Button(
                                        onClick = { viewModel.openModuleDetail(targetModule) },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                                    ) {
                                        Text(
                                            text = "Ke ${locked.moduleId}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepObsidian
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Toggle Khusus Galeri Lencana Modul (Horizontal Lazy Slide saat dibuka)
            Spacer(modifier = Modifier.height(10.dp))
            var isBadgeGalleryOpen by remember { mutableStateOf(false) }
            val normDoneCodes = remember(state.completedNodeCodes) {
                com.example.data.LegacyNodeCodes.normalizeAll(state.completedNodeCodes)
            }
            val completedModuleIds = remember(state.modulesList, normDoneCodes) {
                state.modulesList.filter { mod ->
                    mod.nodes.isNotEmpty() && mod.nodes.all {
                        com.example.data.LegacyNodeCodes.normalize(it.code) in normDoneCodes
                    }
                }.map { it.code }.toSet()
            }
            val badgeShowcase = remember(state.modulesList) {
                if (state.modulesList.isNotEmpty()) {
                    state.modulesList.map { mod ->
                        val icon = when {
                            mod.code.startsWith("M01") -> "🧲"
                            mod.code.startsWith("M02") -> "📡"
                            mod.code.startsWith("M03") -> "⚡"
                            mod.code.startsWith("M04") -> "🖼️"
                            mod.code.startsWith("M05") -> "❄️"
                            mod.code.startsWith("M06") -> "🎛️"
                            mod.code.startsWith("M07") -> "🛡️"
                            mod.code.startsWith("M08") -> "🩻"
                            else -> "👑"
                        }
                        val shortLabel = when (mod.code) {
                            "M01" -> "Fisika Spin"
                            "M02" -> "Relaksasi"
                            "M03A" -> "SE & GRE"
                            "M03B" -> "IR & EPI"
                            "M03C" -> "Kontras"
                            "M04A" -> "Spasial"
                            "M04B" -> "K-space"
                            "M04C" -> "Akuisisi"
                            "M05" -> "Magnet"
                            "M06A" -> "Gradien"
                            "M06B" -> "Koil RF"
                            "M07A" -> "Zona & B0"
                            "M07B" -> "SAR & B1"
                            "M07C" -> "dB/dt"
                            "M08A" -> "Artefak 1"
                            "M08B" -> "Artefak 2"
                            "M08C" -> "QA/QC"
                            "M09" -> "Konsol"
                            else -> mod.title.take(10)
                        }
                        Triple(mod.code, "$icon $shortLabel", mod)
                    }
                } else {
                    listOf(
                        Triple("M01", "🧲 Fisika", null),
                        Triple("M02", "📡 Relaksasi", null),
                        Triple("M03A", "⚡ Sekuens", null),
                        Triple("M04A", "🖼️ K-space", null),
                        Triple("M05", "❄️ Kriogenik", null),
                        Triple("M06A", "🎛️ Gradien", null),
                        Triple("M07A", "🛡️ Safety", null),
                        Triple("M08A", "🩻 Artefak", null),
                        Triple("M09", "👑 Konsol", null)
                    )
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = com.example.ui.theme.CharCardSurface,
                border = BorderStroke(
                    1.2.dp,
                    if (isBadgeGalleryOpen) com.example.ui.theme.CharAccentCyan.copy(alpha = 0.7f) else com.example.ui.theme.CharBorderSlate
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Toggle khusus pembuka/penutup Galeri Lencana Modul
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isBadgeGalleryOpen = !isBadgeGalleryOpen
                        },
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🏅 Galeri Lencana Modul",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = com.example.ui.theme.CharTextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = com.example.ui.theme.CharAccentCyan.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, com.example.ui.theme.CharAccentCyan.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "${completedModuleIds.size}/${state.modulesList.size}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = com.example.ui.theme.CharAccentCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isBadgeGalleryOpen) {
                                    com.example.ui.theme.CharAccentCyan.copy(alpha = 0.2f)
                                } else {
                                    com.example.ui.theme.CharRoomBg.copy(alpha = 0.75f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isBadgeGalleryOpen) com.example.ui.theme.CharAccentCyan else com.example.ui.theme.CharBorderSlate
                                )
                            ) {
                                Text(
                                    text = if (isBadgeGalleryOpen) "Tutup ▲" else "Buka ▼",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBadgeGalleryOpen) com.example.ui.theme.CharAccentCyan else com.example.ui.theme.CharTextDim,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(visible = isBadgeGalleryOpen) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(badgeShowcase, key = { it.first }) { (code, title, modObj) ->
                                val unlocked = code in completedModuleIds
                                val doneNodes = modObj?.nodes?.count {
                                    com.example.data.LegacyNodeCodes.normalize(it.code) in normDoneCodes
                                } ?: 0
                                val totalNodes = modObj?.nodes?.size ?: 0
                                Surface(
                                    onClick = {
                                        if (unlocked) {
                                            triggerAvatarTap(
                                                "Lencana $code ($title) sudah terbuka penuh! Kerja bagus!",
                                                expression = Expression.BANGGA
                                            )
                                        } else {
                                            triggerAvatarTap(
                                                "Lencana $code ($doneNodes/$totalNodes materi). Selesaikan seluruh materi $code untuk membuka lencana ini!",
                                                expression = Expression.BERPIKIR
                                            )
                                        }
                                    },
                                    modifier = Modifier.defaultMinSize(minWidth = 108.dp, minHeight = 58.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (unlocked) {
                                        com.example.ui.theme.CharCardSelected
                                    } else {
                                        com.example.ui.theme.CharRoomBg.copy(alpha = 0.65f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (unlocked) com.example.ui.theme.CharAccentMint else com.example.ui.theme.CharBorderSlate
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (unlocked) com.example.ui.theme.CharTextPrimary else com.example.ui.theme.CharTextDim,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (unlocked) {
                                                "$code ✓ Lulus"
                                            } else if (totalNodes > 0) {
                                                "$code · $doneNodes/$totalNodes"
                                            } else {
                                                code
                                            },
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (unlocked) com.example.ui.theme.CharAccentMint else com.example.ui.theme.CharAccentCyan.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    if (showShiftDialog) {
        NanoShiftStatusDialog(
            avatarLabel = currentAvatar.label,
            specialistRole = specialistProfile.roleTitle,
            userName = state.userName,
            completedCount = state.completedCount,
            totalNodesCount = state.totalNodesCount,
            studyStreakDays = state.studyStreakDays,
            shiftMood = shiftMood,
            favoriteSequence = specialistProfile.favoriteSequence,
            activeExpression = forcedExpression ?: Expression.NETRAL,
            onSelectExpression = { exp ->
                forcedExpression = exp
                showShiftDialog = false
                triggerAvatarTap(
                    customMessage = "Ekspresi Nano diatur ke ${exp.name.lowercase().replaceFirstChar { it.uppercase() }}!",
                    expression = exp
                )
            },
            onDismiss = { showShiftDialog = false }
        )
    }

    if (showSnapshotStudio) {
        NanoSnapshotStudioDialog(
            workingState = workingState,
            currentAvatar = currentAvatar,
            specialistRole = specialistProfile.roleTitle,
            favoriteSequence = specialistProfile.favoriteSequence,
            userName = state.userName,
            accentPalette = accentPalette,
            completedCount = state.completedCount,
            totalNodesCount = state.totalNodesCount,
            onDismiss = { showSnapshotStudio = false },
            onExported = { msg ->
                triggerAvatarTap(msg, HapticFeedbackType.LongPress, Expression.BANGGA)
            }
        )
    }
}
