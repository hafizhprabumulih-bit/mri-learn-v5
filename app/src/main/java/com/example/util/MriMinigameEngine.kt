package com.example.util

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.random.Random

enum class MinigameKind(val key: String, val title: String, val subtitle: String, val durationSec: Int) {
    ZONE_SCREENING(
        key = "ZONE_SCREENING",
        title = "Skrining Zona II–IV",
        subtitle = "Loloskan benda MR Safe atau tolak benda feromagnetik",
        durationSec = 35
    ),
    LARMOR_TAP(
        key = "LARMOR_TAP",
        title = "Tembak Frekuensi Larmor",
        subtitle = "Ketuk tepat saat jarum menyentuh resonansi ¹H",
        durationSec = 35
    ),
    CONTRAST_ARTIFACT_FIX(
        key = "CONTRAST_ARTIFACT_FIX",
        title = "Pembobotan & Artefak",
        subtitle = "Identifikasi kontras citra & perbaiki artefak cepat",
        durationSec = 35
    )
}

enum class SafetyClass { MR_SAFE, MR_CONDITIONAL, MR_UNSAFE }

data class ScreeningItem(
    val id: String,
    val name: String,
    val emoji: String,
    val safetyClass: SafetyClass,
    val allowIntoZone4: Boolean,
    val reason: String,
    val assetKey: String = ""
)

enum class TapAccuracy(val label: String, val basePoints: Int) {
    PERFECT("Sempurna!", 120),
    GOOD("Mendekati", 70),
    MISS("Meleset", 0)
}

data class LarmorChallenge(
    val id: String,
    val fieldStrengthTesla: String,
    val targetMhz: String,
    val targetNormalized: Float,
    val speedMultiplier: Float
)

data class ContrastArtifactQuestion(
    val id: String,
    val prompt: String,
    val badge: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

/**
 * Engine murni (teroptimasi) untuk 3 minigame edukatif MRI berdurasi singkat ("Zona Cepat Nano").
 */
object MriMinigameEngine {

    val screeningBank: List<ScreeningItem> = listOf(
        ScreeningItem("scr_o2", "Tabung O₂ Baja", "O2", SafetyClass.MR_UNSAFE, false, "Baja feromagnetik berisiko menjadi proyektil di Zona IV.", "TABUNG_O2"),
        ScreeningItem("scr_clip", "Klip Aneurisma Feromagnetik Lama", "KLIP", SafetyClass.MR_UNSAFE, false, "Klip lama berisiko mengalami torsi & pergeseran akibat medan B0.", "KLIP_LOGAM"),
        ScreeningItem("scr_pacer_old", "Pacu Jantung Non-MR", "PACER", SafetyClass.MR_UNSAFE, false, "Implan non-MR berisiko malafungsi dan arus induksi RF.", "PACEMAKER"),
        ScreeningItem("scr_scissors", "Gunting Bedah Baja", "GUNTING", SafetyClass.MR_UNSAFE, false, "Benda tajam feromagnetik sangat berbahaya tertarik ke bore magnet.", "GUNTING_BAJA"),
        ScreeningItem("scr_watch", "Jam Tangan Mekanik", "JAM", SafetyClass.MR_UNSAFE, false, "Komponen logam tertarik B0 dan mekanismenya bisa rusak.", "JAM_MEKANIK"),
        ScreeningItem("scr_coin", "Koin & Kunci Saku", "KUNCI", SafetyClass.MR_UNSAFE, false, "Seluruh benda logam saku wajib dilepas di Zona II.", "KOIN_KUNCI"),
        ScreeningItem("scr_card", "Kartu ATM Strip Magnetik", "ATM", SafetyClass.MR_UNSAFE, false, "Medan magnet B0 langsung menghapus data pita magnetik kartu.", "KARTU_ATM"),
        ScreeningItem("scr_chair_fe", "Kursi Roda Besi Biasa", "KURSI", SafetyClass.MR_UNSAFE, false, "Harus diganti kursi roda khusus MR Safe non-feromagnetik.", "KURSI_RODA_BESI"),
        ScreeningItem("scr_pillow", "Bantal Busa & Selimut Polos", "BANTAL", SafetyClass.MR_SAFE, true, "Bahan non-konduktif dan non-magnetik aman masuk Zona IV.", "BANTAL_BUSA"),
        ScreeningItem("scr_chair_mr", "Kursi Roda Aluminium MR Safe", "KURSI_MR", SafetyClass.MR_SAFE, true, "Berlabel MR Safe, bebas feromagnetik sehingga aman di Zona IV.", "KURSI_RODA_MR"),
        ScreeningItem("scr_earplug", "Penyumbat Telinga Busa", "EARPLUG", SafetyClass.MR_SAFE, true, "Wajib & aman untuk melindungi pendengaran dari bising gradien.", "EARPLUG"),
        ScreeningItem("scr_gown", "Gaun Pasien Katun Tanpa Logam", "GAUN", SafetyClass.MR_SAFE, true, "Kain bebas kancing/benang logam aman untuk pemindaian MRI.", "GAUN_KATUN"),
        ScreeningItem("scr_gd", "Vial Kontras Gadolinium Kelasi", "VIAL", SafetyClass.MR_SAFE, true, "Cairan kontras non-feromagnetik aman disiapkan sesuai protokol.", "VIAL_KONTRAS"),
        ScreeningItem("scr_phantom", "Phantom QA ACR Plastik", "PHANTOM", SafetyClass.MR_SAFE, true, "Wadah akrilik berisi larutan uji standar aman untuk kalibrasi bore.", "PHANTOM_ACR"),
        ScreeningItem("scr_pacer_cond", "Implan MR Conditional (Syarat Terpenuhi)", "COND", SafetyClass.MR_CONDITIONAL, true, "Aman masuk hanya bila seluruh syarat B0 & SAR telah diverifikasi.", "STETOSKOP"),
        ScreeningItem("scr_stent_cond", "Stent Koroner MR Conditional (Terverifikasi)", "STENT", SafetyClass.MR_CONDITIONAL, true, "Diizinkan setelah verifikasi tipe implan dan batas kekuatan magnet.", "STENT_COND"),
        ScreeningItem("scr_phone", "Ponsel & Smartwatch Pribadi", "PONSEL", SafetyClass.MR_UNSAFE, false, "Mengandung komponen feromagnetik & baterai yang tertarik B0 serta rusak oleh RF.", "PONSEL_LOGAM"),
        ScreeningItem("scr_apar", "Tabung Pemadam Api (APAR) Baja", "APAR", SafetyClass.MR_UNSAFE, false, "Tabung baja bertekanan sangat berbahaya jika tertarik ke bore magnet 1,5T–3T.", "TABUNG_PEMADAM"),
        ScreeningItem("scr_glasses", "Kacamata Bingkai Logam", "KACAMATA", SafetyClass.MR_UNSAFE, false, "Bingkai logam bisa tertarik B0 dan memicu artefak atau panas lokal.", "KACA_MATA_LOGAM"),
        ScreeningItem("scr_infusion", "Pompa Infus Standar Non-MR", "POMPA", SafetyClass.MR_UNSAFE, false, "Harus ditinggalkan di Zona III; hanya pompa khusus MR Conditional yang boleh masuk.", "POMPA_INFUS_BIASA"),
        ScreeningItem("scr_bottle", "Botol Termos Baja Tahan Karat", "BOTOL", SafetyClass.MR_UNSAFE, false, "Botol logam berisiko memicu gaya tarik magnetik dan distorsi citra.", "BOTOL_MINUM_LOGAM"),
        ScreeningItem("scr_sandbag", "Bantalan Pasir Imobilisasi Non-Magnetik", "SANDBAG", SafetyClass.MR_SAFE, true, "Pasir silika terbungkus vinil bebas logam aman membantu imobilisasi pasien.", "SANDBAG_NONMAG"),
        ScreeningItem("scr_blanket", "Selimut Katun Polos Rumah Sakit", "SELIMUT", SafetyClass.MR_SAFE, true, "Katun polos tanpa kancing logam aman menjaga kenyamanan pasien di bore.", "SELIMUT_KATUN"),
        ScreeningItem("scr_o2_alu", "Tabung O₂ Aluminium MR Conditional", "O2_ALU", SafetyClass.MR_CONDITIONAL, true, "Tabung aluminium khusus berlabel MR Conditional aman dengan jarak batas sesuai protokol.", "TABUNG_O2_ALU"),
        ScreeningItem("scr_ti_plate", "Pelat & Sekrup Ortopedi Titanium", "TITANIUM", SafetyClass.MR_CONDITIONAL, true, "Implan titanium non-feromagnetik MR Conditional aman bila syarat SAR & B0 dipenuhi.", "IMPLAN_TITANIUM"),
        ScreeningItem("scr_pneumatic", "Headset Pneumatik Akustik Non-Logam", "HEADSET", SafetyClass.MR_SAFE, true, "Tabung udara plastik tanpa kumparan logam aman dan meredam bising gradien.", "HEADSET_PNEUMATIK"),
        ScreeningItem("scr_o2_mask", "Masker & Selang Oksigen Polimer", "MASKER", SafetyClass.MR_SAFE, true, "Masker plastik murni bebas kawat hidung logam aman untuk pasien di Zona IV.", "MASKER_OKSIGEN_PLASTIK"),
        ScreeningItem("scr_scalpel", "Skalpel Bedah Baja Karbon", "SKALPEL", SafetyClass.MR_UNSAFE, false, "Bilah baja feromagnetik sangat tajam berisiko menjadi proyektil fatal di bore B0.", "PISAU_BEDAH_BAJA"),
        ScreeningItem("scr_hammer", "Palu Refleks Batang Besi", "PALU", SafetyClass.MR_UNSAFE, false, "Batang besi feromagnetik akan tertarik keras ke isosenter magnet.", "PALU_REFLEKS_BESI"),
        ScreeningItem("scr_ring", "Cincin & Perhiasan Feromagnetik", "CINCIN", SafetyClass.MR_UNSAFE, false, "Perhiasan logam berisiko memicu tarikan magnetik, artefak, dan luka bakar RF.", "CINCIN_FEROMAGNETIK"),
        ScreeningItem("scr_lanyard", "Lanyard Klip Besi & Kartu Magnetik", "LANYARD", SafetyClass.MR_UNSAFE, false, "Klip penjepit besi bisa terlempar dan strip magnetik kartu langsung terhapus.", "KARTU_AKSES_MAGNET")
    )

    val larmorChallenges: List<LarmorChallenge> = listOf(
        LarmorChallenge("lar_02", "0,2 Tesla (Open MRI)", "8,52 MHz", 0.22f, 0.60f),
        LarmorChallenge("lar_05", "0,5 Tesla (Low-Field)", "21,29 MHz", 0.35f, 0.72f),
        LarmorChallenge("lar_15", "1,5 Tesla (Klinis Standar)", "63,87 MHz", 0.52f, 0.85f),
        LarmorChallenge("lar_15b", "1,5 T + Gradien +Gz", "64,12 MHz", 0.44f, 0.95f),
        LarmorChallenge("lar_30", "3,0 Tesla (High-Field)", "127,74 MHz", 0.72f, 1.10f),
        LarmorChallenge("lar_30b", "3,0 T + Gradien −Gz", "127,30 MHz", 0.62f, 1.20f),
        LarmorChallenge("lar_70", "7,0 Tesla (Ultra-High)", "298,06 MHz", 0.84f, 1.35f)
    )

    val contrastArtifactBank: List<ContrastArtifactQuestion> = listOf(
        ContrastArtifactQuestion(
            id = "ca_1",
            prompt = "Citra otak menunjukkan CSF gelap dan lemak subkutan terang. Sekuens apakah ini?",
            badge = "Pembobotan Kontras",
            options = listOf("T1-weighted (T1w)", "T2-weighted (T2w)", "DWI b=1000"),
            correctIndex = 0,
            explanation = "Lemak memiliki T1 pendek (terang) sedangkan cairan CSF memiliki T1 panjang (gelap di T1w)."
        ),
        ContrastArtifactQuestion(
            id = "ca_2",
            prompt = "Cairan CSF tampak gelap pada sekuens T2 sehingga lesi periventrikel menonjol. Ini adalah ...",
            badge = "Sekuens Khusus",
            options = listOf("FLAIR", "STIR", "PDw"),
            correctIndex = 0,
            explanation = "FLAIR memakai pulsa inversi (TI panjang) untuk menekan sinyal cairan bebas CSF."
        ),
        ContrastArtifactQuestion(
            id = "ca_3",
            prompt = "Hidung pasien terlipat muncul di belakang kepala pada arah phase-encoding. Cara mengatasinya?",
            badge = "Artefak Aliasing",
            options = listOf("Aktifkan phase oversampling / perbesar FOV", "Kurangi TR ke 10 ms", "Matikan koil penerima"),
            correctIndex = 0,
            explanation = "Aliasing (wrap-around) terjadi saat anatomi melampaui batas FOV arah fase."
        ),
        ContrastArtifactQuestion(
            id = "ca_4",
            prompt = "Muncul garis putus-putus lurus di tengah citra akibat kebocoran sinyal radio luar. Penyebabnya?",
            badge = "Artefak Zipper",
            options = listOf("Pintu kandang Faraday tidak rapat / RF bocor", "Nilai TE terlalu panjang", "Suhu helium terlalu dingin"),
            correctIndex = 0,
            explanation = "Artefak zipper muncul saat perisai RF ruang magnet terganggu sehingga gelombang luar masuk."
        ),
        ContrastArtifactQuestion(
            id = "ca_5",
            prompt = "Infark serebri akut tampak sangat terang di DWI dan gelap di peta ADC. Apa artinya?",
            badge = "DWI & ADC",
            options = listOf("Difusi molekul air terbatas (edema sitotoksik)", "Difusi air bebas seperti CSF", "Artefak logam berat"),
            correctIndex = 0,
            explanation = "Difusi terbatas menghasilkan nilai ADC rendah dan sinyal tinggi di DWI."
        ),
        ContrastArtifactQuestion(
            id = "ca_6",
            prompt = "Bayangan berulang (ghosting) muncul sepanjang arah phase-encoding karena pasien bernapas. Solusinya?",
            badge = "Artefak Gerak",
            options = listOf("Gunakan respiratory gating / instruksi tahan napas", "Naikkan b-value DWI", "Perkecil ketebalan irisan"),
            correctIndex = 0,
            explanation = "Gerakan periodik menghasilkan replika bayangan di arah fase; gating/breath-hold menguranginya."
        ),
        ContrastArtifactQuestion(
            id = "ca_7",
            prompt = "Untuk memperoleh pembobotan Proton Density (PDw), kombinasi parameter manakah yang dipilih?",
            badge = "Parameter TR/TE",
            options = listOf("TR panjang, TE pendek", "TR pendek, TE panjang", "TR pendek, TE pendek"),
            correctIndex = 0,
            explanation = "TR panjang menekan efek T1 dan TE pendek menekan efek T2 sehingga densitas proton dominan."
        ),
        ContrastArtifactQuestion(
            id = "ca_8",
            prompt = "Pergeseran batas lemak-air (chemical shift) terlihat terlalu tebal. Tindakan cepatnya?",
            badge = "Chemical Shift",
            options = listOf("Perlebar Receive Bandwidth (RBW)", "Persempit FOV setengahnya", "Matikan gradien baca"),
            correctIndex = 0,
            explanation = "Memperlebar RBW memperkecil selisih piksel antara frekuensi lemak dan air."
        ),
        ContrastArtifactQuestion(
            id = "ca_9",
            prompt = "Citra T2w klasik menampilkan cairan CSF dan edema jaringan sebagai warna ...",
            badge = "Kontras T2w",
            options = listOf("Hiperintens (terang)", "Hipointens (gelap pekat)", "Selalu tak terlihat"),
            correctIndex = 0,
            explanation = "Cairan bebas memiliki T2 panjang sehingga sinyalnya bertahan terang pada TE panjang."
        ),
        ContrastArtifactQuestion(
            id = "ca_10",
            prompt = "STIR dipilih saat pemeriksaan muskuloskeletal untuk menekan sinyal jaringan apa?",
            badge = "Fat Suppression",
            options = listOf("Lemak", "Cairan sendi", "Tulang kortikal"),
            correctIndex = 0,
            explanation = "STIR memakai TI pendek (~0,69 × T1 lemak) untuk meniadakan sinyal lemak."
        ),
        ContrastArtifactQuestion(
            id = "ca_11",
            prompt = "Pusat K-space terutama mengontrol aspek citra bagian ...",
            badge = "K-space",
            options = listOf("Kontras keseluruhan & kecerahan sinyal", "Ketajaman tepi mikro saja", "Suhu ruang kontrol"),
            correctIndex = 0,
            explanation = "Frekuensi spasial rendah di pusat K-space mendominasi kontras global citra."
        ),
        ContrastArtifactQuestion(
            id = "ca_12",
            prompt = "Penyekat logam pada pasien memicu distorsi & area hitam kosong (signal void) lokal. Ini artefak ...",
            badge = "Kerentanan Magnetik",
            options = listOf("Susceptibility artifact", "Partial volume", "Aliasing fase"),
            correctIndex = 0,
            explanation = "Perbedaan kerentanan magnetik logam mengubah medan lokal B0 secara drastis (lebih parah di GRE)."
        )
    )

    /**
     * Mengacak deck tantangan tanpa mengulang elemen terakhir pada awal siklus berikutnya.
     */
    fun <T> buildNonRepeatingDeck(
        items: List<T>,
        previousLast: T? = null,
        random: Random = Random.Default
    ): List<T> {
        if (items.size <= 1) return items
        val shuffled = items.shuffled(random).toMutableList()
        if (previousLast != null && shuffled.first() == previousLast) {
            val swapIndex = 1 + random.nextInt(shuffled.size - 1)
            val tmp = shuffled[0]
            shuffled[0] = shuffled[swapIndex]
            shuffled[swapIndex] = tmp
        }
        return shuffled
    }

    /**
     * Mengakumulasi fase osilasi jarum Larmor (0f..2f) secara kontinu per delta frame
     * agar perubahan kecepatan kombo tidak membuat jarum melompat.
     */
    fun advanceLarmorPhase(
        currentPhase: Float,
        deltaSec: Float,
        speedMultiplier: Float,
        combo: Int
    ): Float {
        if (deltaSec <= 0f) return currentPhase
        // Combo boost bertingkat halus: awal tenang, lalu bertahap lebih cepat saat combo bertambah
        val comboBoost = 1f + (combo.coerceIn(0, 8) * 0.10f)
        val halfCyclesPerSec = 0.44f * speedMultiplier.coerceAtLeast(0.35f) * comboBoost
        return (currentPhase + deltaSec.coerceAtMost(0.05f) * halfCyclesPerSec) % 2f
    }

    /**
     * Memetakan fase [0..2) ke posisi jarum ter-normalisasi [0.06..0.94] dengan kurva kosinus halus.
     */
    fun needleFromPhase(phase: Float): Float {
        val norm = ((phase % 2f) + 2f) % 2f
        val eased = (1f - cos(norm * PI.toFloat())) * 0.5f
        return (0.06f + eased * 0.88f).coerceIn(0.06f, 0.94f)
    }

    fun evaluateLarmorTap(needleNormalized: Float, targetNormalized: Float): TapAccuracy {
        val diff = abs(needleNormalized - targetNormalized)
        return when {
            diff <= 0.06f -> TapAccuracy.PERFECT
            diff <= 0.14f -> TapAccuracy.GOOD
            else -> TapAccuracy.MISS
        }
    }

    fun computePoints(basePoints: Int, comboStreak: Int): Int {
        if (basePoints <= 0) return 0
        val bonus = (comboStreak.coerceAtLeast(0) * 20).coerceAtMost(120)
        return basePoints + bonus
    }

    fun evaluateRank(score: Int, accuracyPercent: Int): String = when {
        score >= 900 && accuracyPercent >= 85 -> "Ahli Zona Kilat"
        score >= 500 && accuracyPercent >= 65 -> "Radiografer Sigap"
        else -> "Peserta Shift Nano"
    }
}
