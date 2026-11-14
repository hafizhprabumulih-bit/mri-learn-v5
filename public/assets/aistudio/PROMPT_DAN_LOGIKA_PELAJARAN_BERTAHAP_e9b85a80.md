# Logika & Prompt Antigravity — Mode Pelajaran Bertahap (5 mockup terakhir)

Dokumen ini menjelaskan logika dan prompt untuk lima layar terakhir di kanvas mockup:

| Mockup | Tahap prompt di sini |
| :--- | :--- |
| Pelajaran 5: Logika alur dan aturan | **P1** (logika murni) |
| Pelajaran 1: Penjelasan bertahap | **P2** |
| Pelajaran 3: Soal cerita (+ cek ringkasan) | **P3** (pakai kuis yang ada) dan **P4** (soal cerita baru) |
| Pelajaran 2: Interaktif tebak langkah | **P5** |
| Pelajaran 4: Selesai otomatis | **P6** |

Mode ini menggantikan tombol "Tandai selesai & lanjut" pada reader fokus (Tahap 9). Aturan proyek dibaca dari `AGENTS.md`. Rumus Git, branch per tahap, dan larangan `git push` oleh agen mengikuti `PANDUAN_PETA_BELAJAR_LANGKAH_DAN_PROMPT.md`.

---

## 1. Fakta dari kode Anda yang menentukan rancangan

Saya memeriksa zip proyek. Hal berikut menentukan cara kerja logikanya.

| Fakta | Dampak |
| :--- | :--- |
| `MriNode` punya `title`, `summaryShort`, `detailFull`, `analogi`, `learningGoal` (opsional), `crossLinks` (opsional), `labType`, `level`, `sortOrder` | Pembuka, penjelasan, dan analogi bisa dibangun **tanpa konten baru** |
| `detailFull` memakai **baris kosong (`\n\n`) antarparagraf**; panjang tengah ±690 karakter, terpanjang 2.601, terpendek 25 (dari 186 simpul) | Pemecahan per paragraf lalu per kalimat; wajib ada **pengaman untuk teks sangat pendek dan sangat panjang** |
| Tiap paragraf sering diawali kalimat pendek berfungsi sebagai judul kecil ("MRI dan jaringan lunak.") | Kalimat pembuka itu dijadikan teks tebal (lead-in), jangan dipisah dari kalimat isinya |
| Teks memuat pola yang **tidak boleh dipecah**: kode simpul (`T1.7`), desimal (`63,87`), singkatan (`mis.`, `dll.`), kurung "(lihat T1.7)" | Pemecah kalimat harus teruji dengan teks nyata |
| `QuizQuestion` sudah punya `nodeCode`, `options`, `correctIndex`, `explanation`, `level` | Langkah "Cek ringkasan" memakai **kuis yang sudah ada**, termasuk penjelasannya |
| `QuizAttemptEntity` menyimpan `quizId`, `nodeCode`, `moduleId`, `selectedIndex`, `isCorrect`, `answeredAt` | Hasil jawaban bisa dicatat tanpa tabel baru (lihat keputusan di bagian 3) |
| Soal cerita dan langkah interaktif **belum ada** di kurikulum | Harus berupa konten baru, disimpan terpisah, dan divalidasi pakar |
| `MriCurriculumData.kt` sudah ±6.579 baris dan memengaruhi hash seed | Konten baru **tidak** ditaruh di sana |
| Enum `Expression` sudah punya 6 ekspresi untuk Nano | Maskot di pelajaran memakai ekspresi yang ada |

---

## 2. Logika pelajaran (spesifikasi)

### 2.1 Jenis langkah

| Langkah | Sumber data | Wajib? |
| :--- | :--- | :--- |
| `OPENING` | `title` + `learningGoal` (jika kosong, `summaryShort`) | Ya |
| `EXPLAIN` (n potongan) | `detailFull` dipecah | Ya (minimal 1; cadangan: `summaryShort`) |
| `ANALOGY` | `analogi` | Bila tidak kosong |
| `INTERACTIVE` | Berkas konten tambahan (P5) | Opsional, hanya simpul tertentu |
| `STORY` (maks. 2) | Berkas konten tambahan (P4), hanya yang `reviewed = true` | Opsional |
| `CHECK` | Kuis simpul yang ada (maks. 1) | Bila ada kuis dan tidak duplikat |
| `COMPLETE` | Dihitung | Ya |

Urutan: OPENING → EXPLAIN… → ANALOGY → INTERACTIVE → STORY… → CHECK → COMPLETE.

### 2.2 Pemecahan teks (`LessonChunker`)
1. Pecah `detailFull` pada baris kosong menjadi paragraf.
2. Pada tiap paragraf, bila kalimat pertama ≤ 60 karakter dan masih ada kalimat lain, jadikan **lead-in** (ditebalkan, tidak berdiri sendiri).
3. Pecah kalimat pada tanda akhir (`. ! ?`) yang diikuti spasi dan huruf kapital, angka, atau tanda kurung buka. **Jangan** memecah setelah singkatan (`mis.`, `dll.`, `dsb.`, `dst.`, `vs.`, `no.`), di dalam kode (`T1.7`), pada desimal (`63,87`), atau di dalam tanda kurung.
4. Kelompokkan jadi potongan: maksimal 2 kalimat dan ±220 karakter (boleh 3 kalimat bila sangat pendek); lead-in selalu menyatu dengan kalimat isinya.
5. Batasi jumlah potongan maksimal 8 (gabungkan potongan bertetangga bila lebih).
6. Bila `detailFull` kosong atau < 40 karakter, pakai `summaryShort` sebagai satu potongan.

### 2.3 Jumlah segmen progres
Segmen = jumlah langkah selain `COMPLETE`. Bila lebih dari 10, gabungkan potongan `EXPLAIN` hingga ≤ 10 segmen.

### 2.4 Perilaku tiap langkah
- **EXPLAIN:** potongan muncul satu per ketukan **Lanjut** (atau ketuk di mana saja). Potongan lama meredup ke 50%; potongan baru muncul dengan pudar-masuk (±250 ms). **Bukan efek mengetik** (efek mengetik memperlambat membaca). Bila pengguna mematikan animasi sistem, potongan langsung tampil.
- **Tombol kembali** (sistem): kembali ke potongan sebelumnya untuk membaca ulang; tidak bisa menjawab ulang soal yang sudah terjawab.
- **Tombol X:** dialog "Keluar? Kemajuan disimpan", lalu kembali ke jalur modul.
- **Bendera (laporkan):** membuka lembar berbagi (`ACTION_SEND`) berisi kode simpul dan nomor langkah. Tanpa jaringan dan tanpa server.

### 2.5 Soal (CHECK dan STORY)
- Percobaan 1 salah: opsi yang dipilih meredup, tampil penjelasan, tombol **Coba lagi**.
- Percobaan 2 salah: tampilkan jawaban benar dan penjelasan, lalu **lanjut** (tidak menahan pengguna).
- Setiap percobaan dicatat sebagai baris `quiz_attempts`.
- Warna salah memakai amber lembut dari tema, bukan merah keras. Ekspresi Nano: benar → `SENANG`, salah → `WASPADA`, selesai → `BANGGA`.

### 2.6 Penyelesaian simpul
- Simpul ditandai selesai **saat langkah `COMPLETE` tercapai**, memakai fungsi penyelesaian yang **sudah ada** (`markNodeCompletedIfNotYet` atau setara).
- Bila penyelesaian itu baru (belum pernah selesai): XP, layar selesai, simpul berikutnya terbuka.
- Bila simpul sudah pernah selesai: mode "Ulangi pelajaran", tanpa XP baru, tanpa mengubah progres.

### 2.7 Bintang penguasaan (dihitung, tidak disimpan)
`bintang = 1 + jumlah item penilaian yang dijawab benar pada percobaan pertama`, maksimum 3. Item penilaian = `STORY` dan `CHECK` yang tersedia pada simpul itu. Bila hanya satu item penilaian tersedia, maksimum 2 bintang. Dihitung dari `quiz_attempts` (percobaan paling awal per `quizId`).

### 2.8 Tanda "perlu diulang" (dihitung)
Simpul "perlu diulang" bila jawaban terakhir pada salah satu item penilaiannya salah. Fungsi ini disiapkan sekarang untuk dipakai fitur Ulas Hari Ini nanti.

### 2.9 Lanjut dari langkah terakhir
Posisi langkah disimpan di DataStore dengan kunci per simpul (kode dinormalkan lewat `LegacyNodeCodes.normalize`), dan dihapus saat simpul selesai. Tidak ada perubahan Room.

### 2.10 Dua cara belajar
- **Pelajaran** (baru, bawaan): alur di atas.
- **Baca cepat:** reader fokus yang sekarang. Menyelesaikannya butuh **menjawab satu soal kuis simpul**; bila simpul tak punya kuis, tombol "Tandai selesai" tetap ada dengan satu dialog konfirmasi.
- Sheet simpul di peta menampilkan dua tombol. Pilihan terakhir diingat di DataStore (`preferred_reader_mode`).

### 2.11 Sakelar pengaman
Konstanta `LESSON_FLOW_ENABLED` (default `true`). Bila `false`, aplikasi memakai reader fokus lama sepenuhnya. Ini memudahkan membatalkan bila ada masalah di perangkat pengguna.

---

## 3. Keputusan yang perlu Anda ambil

| # | Keputusan | Usulan saya |
| :--- | :--- | :--- |
| 1 | Mencatat jawaban soal cerita | Pakai `quiz_attempts` dengan `quizId` berawalan `story:` (tanpa migrasi). **Syarat:** semua kode yang membaca `quiz_attempts` (akurasi di layar Kuis, daftar soal salah, statistik) harus diperiksa agar tidak error atau salah hitung untuk id yang tak ada di katalog kuis. Bila ternyata berisiko, pakai tabel terpisah lewat migrasi. |
| 2 | Efek munculnya teks | Pudar-masuk, bukan mengetik |
| 3 | Salah dua kali | Lanjut, simpul diberi tanda "perlu diulang" |
| 4 | Soal cerita tampil di rilis | Hanya yang `reviewed = true`; versi debug menampilkan draf dengan label "DRAF" |
| 5 | Siapa yang memvalidasi isi | Anda atau dosen pembimbing; agen **tidak** boleh menandai `reviewed = true` |

---

## 4. Format berkas konten tambahan (`lesson_extras.json`)

Lokasi: `app/src/main/assets/lesson_extras.json`. Dibaca saat dibutuhkan, tidak dimasukkan ke Room dan tidak memengaruhi hash seed.

```json
{
  "version": 1,
  "nodes": {
    "M01.T0": {
      "nanoHints": ["Itu sebabnya MRI bagus untuk jaringan lunak."],
      "recap": [
        "Hidrogen di air dan lemak menjadi sumber sinyal MRI.",
        "Proton berpresesi pada frekuensi Larmor, f0 = γ · B0.",
        "Pulsa RF pada frekuensi itu mengeksitasi proton, lalu muncul sinyal FID."
      ],
      "stories": [
        {
          "id": "story:M01.T0:1",
          "scenario": "Pak Budi (58 tahun) dijadwalkan MRI lutut di pesawat 1,5 T. Rina, radiografer yang bertugas, menyiapkan konsol. Ia ingat bahwa pulsa RF harus dikirim tepat pada frekuensi Larmor proton hidrogen.",
          "question": "Kira-kira berapa frekuensi itu pada 1,5 T?",
          "options": ["21,3 MHz", "42,58 MHz", "63,87 MHz", "127,7 MHz"],
          "correctIndex": 2,
          "explanation": "f0 = γ · B0. Dengan γ ≈ 42,58 MHz/T, maka 42,58 × 1,5 ≈ 63,87 MHz.",
          "followUps": ["Bagaimana jika 3 T?"],
          "reviewed": false,
          "reviewedBy": null,
          "reviewedAt": null
        }
      ],
      "interactive": {
        "type": "REVEAL_CHAIN",
        "title": "Rantai Sinyal",
        "intro": "Kamu memprediksi apa yang terjadi selanjutnya. Terus ketuk Langkah berikutnya untuk melihat sinyal MRI terbentuk.",
        "steps": [
          { "text": "Proton hidrogen berada dalam medan magnet B0." },
          { "text": "Proton berpresesi pada frekuensi Larmor." },
          { "text": "Pulsa RF dikirim tepat pada frekuensi Larmor." },
          {
            "text": "Sinyal FID terdeteksi oleh koil.",
            "prediction": {
              "question": "Apa yang terjadi selanjutnya?",
              "options": ["Sinyal FID terdeteksi", "Gradien dinyalakan", "Magnet dimatikan"],
              "correctIndex": 0
            }
          }
        ],
        "reviewed": false
      }
    }
  }
}
```

Contoh di atas hanya ilustrasi struktur dan **belum divalidasi**. Fakta fisikanya (γ ≈ 42,58 MHz/T, f0 ≈ 63,87 MHz pada 1,5 T) sudah saya cocokkan, tetapi setiap isi baru tetap harus Anda tinjau.

---

## Tahap P1 — Logika murni (tanpa UI)

**Branch:** `feat/pelajaran-1-logika`
**Lampirkan:** `MriCurriculumData.kt` (hanya `MriNode`, `QuizQuestion`, dan contoh simpul `M01.T1.1`), `LegacyNodeCodes.kt`, `ResumeNodeResolver.kt` (sebagai contoh gaya), `PathStateResolver.kt`, screenshot mockup "Logika alur"

```text
TAHAP P1 — Logika Pelajaran Bertahap (util murni + tes, tanpa UI).

Tujuan: membangun otak mode pelajaran yang membagi materi simpul menjadi langkah-langkah.
JANGAN mengubah MriCurriculumData.kt, CurriculumSnapshot.SEED_TAG, skema Room, atau UI.

Baca: data/MriCurriculumData.kt (MriNode, QuizQuestion), data/LegacyNodeCodes.kt,
util/ResumeNodeResolver.kt dan util/PathStateResolver.kt (ikuti gaya kode),
data/local/Entities.kt (QuizAttemptEntity).

Tulis rencana (maks 10 baris) dan tunggu persetujuan saya. Lalu buat di package util/lesson/:

1. LessonChunker.kt
   fun chunk(detailFull: String, summaryShort: String): List<LessonChunk>
   data class LessonChunk(val leadIn: String?, val text: String)
   Aturan:
   - pecah detailFull pada baris kosong menjadi paragraf;
   - kalimat pertama paragraf <= 60 karakter dan masih ada kalimat lain -> leadIn (tidak berdiri sendiri);
   - pecah kalimat pada . ! ? yang diikuti spasi lalu huruf kapital, angka, atau "(". JANGAN pecah
     setelah singkatan (mis., dll., dsb., dst., vs., no.), di dalam kode seperti "T1.7", pada
     desimal seperti "63,87", dan di dalam tanda kurung;
   - kelompokkan jadi potongan maks. 2 kalimat dan ±220 karakter (3 kalimat bila sangat pendek);
   - maksimum 8 potongan (gabungkan yang bertetangga bila lebih);
   - detailFull kosong atau < 40 karakter -> satu potongan dari summaryShort.
2. LessonStep.kt
   sealed interface LessonStep dengan: Opening(title, goal), Explain(chunks: List<LessonChunk>
   atau satu chunk), Analogy(text), Interactive(contentId), Story(storyId), Check(quizId), Complete.
3. LessonPlanBuilder.kt
   fun build(node: MriNode, quizzes: List<QuizQuestion>, extras: LessonExtras?): LessonPlan
   data class LessonPlan(val nodeCode: String, val steps: List<LessonStep>) { val segmentCount: Int }
   Urutan: Opening, Explain..., Analogy (bila analogi tidak kosong), Interactive (bila ada),
   Story (maks. 2, hanya yang reviewed = true; parameter allowDrafts untuk build debug),
   Check (maks. 1 kuis simpul yang belum dipakai sebagai Story), Complete.
   segmentCount = jumlah langkah selain Complete; bila > 10, gabungkan Explain sampai <= 10.
   LessonExtras boleh berupa tipe kosong sementara (data class dengan peta kosong); parser JSON
   dikerjakan di tahap lain.
4. LessonReducer.kt (state machine murni)
   data class LessonState(val plan, val index, val attemptsByItem: Map<String, List<Int>>, val finished: Boolean)
   Event: Next, Back, Answer(itemId, optionIndex, correctIndex), Retry.
   Aturan: Back hanya mundur ke langkah non-soal yang sudah dilihat; jawaban benar -> boleh Next;
   salah pertama -> tahan di soal dengan status "boleh coba lagi"; salah kedua -> tampilkan jawaban benar
   dan izinkan Next; Next pada langkah terakhir sebelum Complete -> finished = true.
5. MasteryStars.kt
   fun stars(attempts: List<AttemptRecord>, assessmentIds: List<String>): Int
   bintang = 1 + jumlah item penilaian yang benar pada PERCOBAAN PERTAMA, maks. 3; bila hanya satu
   item penilaian tersedia, maks. 2.
   fun needsReview(attempts: List<AttemptRecord>, assessmentIds: List<String>): Boolean
   (jawaban TERAKHIR pada salah satu item salah).
   AttemptRecord(quizId, isCorrect, answeredAt) dibuat sebagai tipe murni di util/, bukan Room.

Tes (test/.../lesson/): wajib mencakup
 - teks nyata simpul M01.T1.1 (dua paragraf; "(lihat T1.7)" tidak terpecah; "M07A." tidak terpecah salah),
 - teks sangat pendek (25 karakter) dan sangat panjang (2.600 karakter) -> maksimal 8 potongan,
 - desimal "63,87" dan singkatan "mis." tidak memecah kalimat,
 - paragraf satu kalimat, tanpa lead-in,
 - plan tanpa analogi, tanpa kuis, dengan 2 soal cerita,
 - reducer: benar pertama, salah lalu benar, salah dua kali, Back tidak menjawab ulang,
 - bintang: tanpa item penilaian, 1 item, 2 item, urutan percobaan acak, needsReview.
Jalankan chunker pada SELURUH 186 simpul dalam satu tes dan pastikan: tidak ada yang melempar
exception, tiap simpul punya >= 1 potongan, tidak ada potongan kosong, tidak ada simpul > 8 potongan.
Cetak (println) 5 simpul dengan jumlah potongan terbanyak dan 5 simpul dengan potongan paling panjang.

Selesai bila: semua tes hijau dan saya mendapat cetakan dari tes seluruh 186 simpul.
```
**Commit:** `feat(lesson): logika pelajaran bertahap (chunker, plan, reducer, bintang)`
**Setelah ini:** lihat cetakan simpul bermasalah. Jika banyak simpul menghasilkan potongan janggal, perbaiki aturan di P1 sebelum lanjut.

---

## Tahap P2 — Layar Pelajaran bertahap (mockup 1)

**Branch:** `feat/pelajaran-2-layar-bertahap`
**Prasyarat:** P1 selesai, Tahap 9 (reader fokus) sudah ada.
**Lampirkan:** `pelajaran-bertahap.png`, screenshot reader fokus saat ini, file reader fokus dan `ModulePathScreen`, `AvatarLayers.kt`

```text
TAHAP P2 — Layar Pelajaran Bertahap.

Prasyarat: util/lesson/ dari P1 sudah ada dan tesnya hijau.

Baca: reader fokus dari Tahap 9 (FOCUS_NODE) dan bagaimana sheet simpul membukanya,
ui/path/, ui/character/AvatarLayers.kt (enum Expression dan komposable untuk menggambar Nano),
UserDataRepository (cara DataStore dipakai), util/lesson/ (P1), util/PathStateResolver (nextNodeOf).

Tulis rencana dan tunggu persetujuan saya. Lalu:

1. Buat ui/lesson/LessonScreen.kt: layar penuh tanpa bottom navigation.
   - Bar atas: ikon X, bilah progres bersegmen (segmentCount dari LessonPlan; segmen selesai =
     warna aksen, segmen berjalan = setengah), ikon bendera (laporkan).
   - Area isi: LazyColumn/Column yang menumpuk potongan Explain. Potongan sebelumnya meredup ke 50%,
     potongan terbaru muncul dengan pudar-masuk (~250 ms) dan, bila perlu, scroll otomatis agar tetap
     terlihat. BUKAN efek mengetik. Lead-in ditebalkan. Istilah kunci (bila ada penanda istilah di
     teks atau daftar istilah glosarium) boleh ditonjolkan, tetapi jangan membuat logika baru; lewati bila rumit.
   - Maskot Nano di kiri bawah (komposabel yang sudah ada; ekspresi NETRAL dan BERPIKIR), gelembung
     komentar HANYA bila ada nanoHint (P4); sebelum itu gelembung disembunyikan.
   - Tombol "Lanjut" di kanan bawah (>= 48dp) dan ketuk di area isi untuk melanjutkan.
   - Teks kecil "Ketuk di mana saja untuk melanjutkan" tampil beberapa kali pertama saja
     (simpan penghitung di DataStore).
2. Gunakan LessonReducer dari P1; JANGAN menaruh logika langkah di komposabel. State holder terpisah
   (ViewModel baru atau remember), bukan MriViewModel.
3. Langkah yang dikerjakan di tahap ini: Opening, Explain, Analogy, dan Complete (sementara Complete
   memanggil fungsi penyelesaian yang SUDAH ADA lalu kembali ke jalur modul). Langkah Story, Check,
   Interactive di-skip sementara.
4. Simpan posisi langkah di DataStore (kunci per simpul, kode dinormalkan dengan
   LegacyNodeCodes.normalize); hapus saat selesai; lanjutkan dari posisi tersimpan saat dibuka lagi.
5. Tombol X: dialog "Keluar? Kemajuan disimpan". Tombol Back sistem: mundur ke potongan sebelumnya;
   pada potongan pertama, tampilkan dialog yang sama.
6. Bendera: buka lembar berbagi (ACTION_SEND) berisi kode simpul dan nomor langkah. Tanpa jaringan.
7. Sheet simpul di peta: tambahkan dua tombol "Mulai pelajaran" (utama) dan "Baca cepat"
   (membuka reader fokus yang sekarang). Ingat pilihan terakhir di DataStore (preferred_reader_mode).
8. Tambahkan konstanta LESSON_FLOW_ENABLED = true. Bila false, perilaku lama (reader fokus) dipakai.
9. Aksesibilitas: potongan baru diumumkan TalkBack (liveRegion); hormati pengaturan "hapus animasi"
   (potongan tampil langsung); target sentuh >= 48dp; kontras >= 4.5:1.
10. Jangan mengubah reader fokus lama kecuali menambah titik masuk. Jangan mengubah data kurikulum.

Selesai bila: dari peta, "Mulai pelajaran" membuka alur bertahap untuk simpul apa pun, potongan muncul
satu per ketukan, keluar lalu masuk kembali melanjutkan dari posisi terakhir, penyelesaian menandai
simpul selesai sekali, dan semua tes hijau.
```
**Uji manual:** coba simpul pendek, panjang (cari yang terpanjang dari cetakan P1), dan yang tanpa analogi; putar layar; matikan animasi sistem.
**Commit:** `feat(lesson): layar pelajaran bertahap dengan penyimpanan posisi`

---

## Tahap P3 — Cek ringkasan dari kuis yang sudah ada (mockup 3, tanpa konten baru)

**Branch:** `feat/pelajaran-3-cek-ringkasan`
**Prasyarat:** P2 selesai.
**Lampirkan:** `soal-cerita.png`, `QuizScreen.kt`, `Entities.kt`, `UserDataRepository.kt` (fungsi simpan jawaban kuis), semua kode yang membaca `quiz_attempts`

```text
TAHAP P3 — Langkah Cek ringkasan memakai kuis simpul yang sudah ada.

Baca: ui/quiz/QuizScreen.kt (bagaimana jawaban disimpan dan umpan balik ditampilkan),
data/local/Entities.kt (QuizAttemptEntity), DAO dan repository yang menyimpan/membaca quiz_attempts,
util/lesson/ (LessonReducer, MasteryStars).

Tulis rencana dan tunggu persetujuan saya. Lalu:

1. Layar langkah Check di LessonScreen: kartu soal (pertanyaan, opsi berhuruf A-D), panel umpan
   balik di bawah, tombol "Periksa jawaban" -> "Lanjut". Tampilan mengikuti mockup 3, tetapi tanpa
   kartu "Kasus" (itu P4).
2. Aturan jawaban: salah pertama -> opsi yang dipilih meredup, tampil explanation dan tombol "Coba lagi";
   salah kedua -> tampilkan jawaban benar + explanation, lalu izinkan lanjut; benar -> panel hijau
   "Tepat!". Warna salah memakai amber lembut dari tema, bukan merah keras.
   Ekspresi Nano: benar = SENANG, salah = WASPADA.
3. Catat SETIAP percobaan ke quiz_attempts memakai fungsi yang sudah dipakai layar Kuis (quizId asli,
   nodeCode, moduleId, selectedIndex, isCorrect, answeredAt). JANGAN mengubah skema.
   Pastikan layar Kuis (akurasi %, daftar soal salah, statistik) menghitung percobaan dari pelajaran
   ini dengan benar dan tidak menghitung ganda dengan cara yang menyesatkan; tulis tes untuk itu.
4. Jika simpul tidak punya kuis, langkah Check dilewati dan Complete langsung tercapai.
5. Mode "Baca cepat": ubah reader fokus lama agar menyelesaikan simpul lewat menjawab satu soal kuis
   simpul; bila simpul tak punya kuis, tombol "Tandai selesai" tetap ada dengan satu dialog konfirmasi.
6. Hitung bintang memakai MasteryStars dari quiz_attempts; belum ditampilkan (itu P6).

Selesai bila: soal tampil dan tercatat, aturan dua percobaan berjalan, statistik layar Kuis tetap benar,
Baca cepat menyelesaikan simpul lewat satu soal, dan semua tes hijau.
```
**Commit:** `feat(lesson): langkah cek ringkasan dengan dua percobaan`

---

## Tahap P4 — Soal cerita (mockup 3, konten baru)

**Branch:** `feat/pelajaran-4-soal-cerita`
**Prasyarat:** P3 selesai. **Konten:** minimal beberapa soal yang sudah Anda tinjau (lihat bagian 5).
**Lampirkan:** `soal-cerita.png`, bagian 4 dokumen ini (format JSON), contoh JSON dengan beberapa soal Anda

```text
TAHAP P4 — Format, pemuat, dan layar Soal Cerita.

Konteks: soal cerita adalah konten BARU yang disimpan di assets/lesson_extras.json (format ada di
bagian 4 dokumen panduan; salin ke repo sebagai docs/lesson_extras_format.md). JANGAN menaruh konten di
MriCurriculumData.kt dan JANGAN mengubah CurriculumSnapshot.SEED_TAG.

Baca: util/lesson/LessonPlanBuilder (parameter extras dan allowDrafts), build.gradle.kts dan
gradle/libs.versions.toml (cek pustaka JSON yang sudah ada; PREFERENSI: org.json bawaan Android atau
pustaka yang sudah terpasang, jangan menambah dependensi baru tanpa persetujuan saya),
data/UserDataRepository.kt, semua kode yang membaca quiz_attempts.

Tulis rencana dan tunggu persetujuan saya. Lalu:

1. data/lesson/LessonExtras.kt + pemuat: parse assets/lesson_extras.json menjadi LessonExtras
   (nodeCode -> nanoHints, recap, stories, interactive). Pemuat tahan terhadap berkas hilang atau rusak
   (kembalikan LessonExtras kosong dan catat ke log; aplikasi tidak boleh crash). Muat sekali dan simpan
   di cache memori; jangan memblokir thread utama.
2. Tes penjaga isi test/LessonExtrasGuardTest.kt, untuk setiap simpul di berkas:
   - nodeCode ada di kurikulum (normalkan dengan LegacyNodeCodes);
   - setiap soal: tepat 4 opsi yang berbeda, correctIndex 0..3, explanation tidak kosong,
     scenario <= 400 karakter, question tidak kosong, id berawalan "story:<kode simpul>:" dan unik;
   - tidak ada tanda kutip atau karakter yang merusak render; tidak ada nama orang nyata yang dikenali
     (gunakan daftar kecil kata terlarang bila perlu) dan tidak ada angka dosis obat;
   - reviewed = true WAJIB disertai reviewedBy dan reviewedAt terisi.
   Tes harus lolos pada berkas kosong dan pada berkas contoh.
3. Layar langkah Story di LessonScreen mengikuti mockup 3: kartu "KASUS" berisi scenario, pertanyaan,
   4 opsi, panel umpan balik, tombol "Lanjut", dan chip followUps (ketuk -> kirim sebagai pertanyaan ke
   Tanya Nano HANYA bila fitur itu sudah ada; kalau belum, sembunyikan chip).
4. Aturan jawaban sama dengan P3. quizId = id soal cerita ("story:..."). Pastikan SEMUA kode yang membaca
   quiz_attempts (akurasi di layar Kuis, daftar soal salah, statistik) menangani id yang tidak ada di
   katalog kuis tanpa error atau salah hitung; tulis tes. Bila ternyata berisiko tinggi, berhenti dan
   beri tahu saya sebelum menambah tabel (itu butuh migrasi).
5. LessonPlanBuilder: soal cerita yang ditampilkan di rilis HANYA yang reviewed = true. Pada build debug,
   tampilkan draf dengan label "DRAF" (gunakan BuildConfig.DEBUG).
6. nanoHints: tampilkan sebagai gelembung komentar Nano pada potongan penjelasan bila ada.
7. JANGAN menandai reviewed = true pada konten apa pun. JANGAN menulis konten soal baru; hanya
   memakai berkas yang saya berikan.

Selesai bila: berkas contoh dimuat, soal tampil di simpul yang punya soal, hanya soal yang ditinjau
tampil di rilis, tes penjaga hijau, dan statistik Kuis tetap benar.
```
**Commit:** `feat(lesson): format soal cerita, pemuat, dan layar`

---

## Tahap P5 — Langkah interaktif "tebak langkah" (mockup 2)

**Branch:** `feat/pelajaran-5-interaktif`
**Prasyarat:** P4 selesai. **Konten:** minimal satu simpul dengan `interactive` yang sudah Anda tinjau.
**Lampirkan:** `pelajaran-interaktif.png`, contoh `interactive` dari JSON

```text
TAHAP P5 — Langkah Interaktif REVEAL_CHAIN.

Baca: data/lesson/LessonExtras (field interactive), ui/lesson/LessonScreen.kt, util/lesson/LessonReducer.

Tulis rencana dan tunggu persetujuan saya. Lalu:

1. Komposabel RevealChainCard sesuai mockup 2: teks pengantar di atas; kartu dengan judul
   (mis. "Lab Mini · Rantai Sinyal"), daftar langkah bernomor, langkah berikutnya sebagai placeholder
   putus-putus, tombol "Langkah berikutnya" dan ikon reset.
2. Perilaku: tiap ketukan memunculkan satu langkah (pudar-masuk). Bila sebuah langkah punya prediction,
   tampilkan "Tebak dulu" berisi opsi; pengguna boleh memilih lalu mengetuk "Langkah berikutnya"; tebakan
   TIDAK dinilai dan TIDAK memengaruhi bintang (hanya umpan balik ringan: "Betul!" atau "Hampir, lihat
   langkah berikutnya"). Reset mengembalikan ke awal. Langkah selesai saat semua langkah muncul, lalu
   tombol "Lanjut".
3. Ekspresi Nano: BERPIKIR saat menebak, SENANG saat tebakan benar.
4. Hanya tampilkan bila interactive.reviewed = true di rilis (draf berlabel di debug).
5. Tambahkan ke LessonPlanBuilder jenis langkah Interactive (sudah disiapkan di P1) dan tes baru:
   plan dengan dan tanpa interactive.
6. Tambahkan pemeriksaan di LessonExtrasGuardTest: type dikenal (hanya REVEAL_CHAIN), minimal 3 langkah,
   correctIndex valid pada prediction, jumlah opsi prediksi 2 sampai 4.
7. Aksesibilitas: setiap langkah baru diumumkan TalkBack; semua tombol >= 48dp; animasi dihormati
   pengaturan sistem.

Selesai bila: simpul dengan interactive menampilkan langkah ini setelah penjelasan, reset dan tebakan
berfungsi, dan semua tes hijau.
```
**Commit:** `feat(lesson): langkah interaktif tebak langkah`

---

## Tahap P6 — Layar selesai otomatis (mockup 4)

**Branch:** `feat/pelajaran-6-selesai`
**Prasyarat:** P3 selesai dan Tahap 6 (`XpCalculator`, `NodeCompletedScreen`) sudah ada.
**Lampirkan:** `pelajaran-selesai.png`, `NodeCompletedScreen.kt`, `XpCalculator.kt`, `MasteryStars.kt`

```text
TAHAP P6 — Layar Selesai otomatis dengan bintang penguasaan.

Baca: NodeCompletedScreen (Tahap 6), util/XpCalculator.kt, util/lesson/MasteryStars.kt,
util/PathStateResolver (nextNodeOf), data lesson_extras (recap).

Tulis rencana dan tunggu persetujuan saya. Lalu:

1. Perluas NodeCompletedScreen (jangan membuat layar duplikat) untuk alur pelajaran sesuai mockup 4:
   maskot Nano berekspresi BANGGA, judul "<kode> selesai", bintang penguasaan (MasteryStars) dengan
   keterangan singkat ("2 bintang: soal cerita benar di percobaan pertama"), kartu XP / Simpul / Streak,
   kartu "Yang kamu pelajari".
2. "Yang kamu pelajari": pakai recap dari lesson_extras bila ada dan reviewed = true; bila tidak,
   pakai learningGoal atau summaryShort satu baris. JANGAN menghasilkan rangkuman otomatis.
3. Tombol utama "Lanjut: <judul simpul berikutnya>" (nextNodeOf; bila ini simpul terakhir modul, arahkan
   ke layar Modul Tuntas bila sudah ada, kalau belum kembali ke peta) dan tombol teks "Ulangi pelajaran ini".
4. Hanya tampil untuk penyelesaian BARU. Untuk pengulangan, tampilkan versi ringkas tanpa XP.
5. Sakelar "Tambahkan ke Ulas Hari Ini": tampilkan HANYA bila fitur Ulas Hari Ini sudah ada;
   jika belum, jangan tampilkan apa pun dan jangan membuat penyimpanan baru.
6. Tes: bintang untuk berbagai kombinasi, teks keterangan bintang, perilaku pengulangan.

Selesai bila: menyelesaikan pelajaran menampilkan layar ini dengan nilai benar, pengulangan tidak
memberi XP, dan semua tes hijau.
```
**Commit:** `feat(lesson): layar selesai otomatis dengan bintang penguasaan`

---

## 5. Menyusun konten soal cerita dengan bantuan AI (di chat lain)

Gunakan prompt ini di chat AI mana pun untuk membuat **draf**. Setiap draf **wajib Anda atau dosen tinjau** sebelum `reviewed` diubah menjadi `true`.

```text
Kamu membantu menyusun DRAF soal cerita untuk aplikasi belajar MRI berbahasa Indonesia
untuk mahasiswa radiografer. Hasilmu akan ditinjau pakar sebelum dipakai.

Aturan:
1. Gunakan HANYA fakta yang ada pada materi simpul di bawah. Jangan menambah fakta klinis baru.
   Bila materi tidak cukup untuk soal yang baik, katakan "materi tidak cukup" dan jangan mengarang.
2. Tulis 1 kasus singkat (maks. 400 karakter) di ruang konsol atau ruang pemeriksaan dengan tokoh
   fiktif (nama umum Indonesia, tanpa data identitas nyata).
3. Satu pertanyaan, 4 opsi berbeda, tepat 1 benar. Pengecoh harus masuk akal, misalnya nilai atau
   konsep yang sering tertukar dengan jawaban benar, bukan jawaban asal-asalan.
4. Acak posisi jawaban benar; jangan selalu di opsi yang sama.
5. Penjelasan (maks. 3 kalimat) menyebut alasan dari materi, bukan sekadar mengulang jawaban.
6. Jangan memuat dosis obat, instruksi pengobatan, atau diagnosis pasien.
7. Tandai angka atau istilah yang kamu tidak yakin dengan "[PERIKSA]".
8. Keluaran: JSON saja, mengikuti format di bawah, dengan reviewed = false.

Format keluaran: [ { "id": "story:<kodeSimpul>:<nomor>", "scenario": "...", "question": "...",
"options": ["...","...","...","..."], "correctIndex": 0, "explanation": "...",
"followUps": ["..."], "reviewed": false, "reviewedBy": null, "reviewedAt": null } ]

Materi simpul:
Kode: <tempel kode simpul>
Judul: <tempel judul>
Tujuan: <tempel learningGoal atau summaryShort>
Isi: <tempel detailFull>
Analogi: <tempel analogi>
Jumlah soal yang diminta: <1 atau 2>
```

**Daftar periksa tinjauan (untuk Anda atau dosen):**
- Fakta dan angka benar dan sesuai materi simpul.
- Hanya satu jawaban yang benar; pengecoh tidak ambigu.
- Kasus realistis dan tidak mengarah pada praktik yang salah.
- Bahasa jelas; tidak ada nama orang nyata.
- Setelah setuju: isi `reviewed`, `reviewedBy` (nama Anda), dan `reviewedAt` (tanggal).

**Urutan konten yang disarankan:** mulai dari M01 (23 simpul), lalu M02 (5 simpul), karena keduanya paling dulu dilihat pengguna. Jangan menunggu seluruh 186 simpul; aplikasi tetap berfungsi dengan soal hanya di sebagian simpul.

---

## 6. Risiko dan mitigasi

| Risiko | Mitigasi |
| :--- | :--- |
| Potongan teks janggal (singkatan, kode, desimal terpecah) | Tes pada seluruh 186 simpul di P1 dan cetakan simpul bermasalah |
| Simpul sangat panjang membuat pelajaran terasa lama | Batas 8 potongan dan 10 segmen; pengguna berpengalaman memakai Baca cepat |
| Jawaban soal cerita merusak statistik layar Kuis | Periksa semua pembaca `quiz_attempts`; tes; bila berisiko, berhenti dan putuskan migrasi bersama |
| Konten medis salah | `reviewed` wajib; agen tidak boleh menandainya; tes penjaga; label DRAF di debug |
| Pengguna lama kehilangan akses cepat ke materi | Mode Baca cepat dan sakelar `LESSON_FLOW_ENABLED` |
| Reader fokus (Tahap 9) dan pelajaran bercabang terlalu jauh | Satu titik masuk dari sheet simpul; satu fungsi penyelesaian yang sama |
| Animasi mengganggu aksesibilitas | Hormati pengaturan sistem; TalkBack mengumumkan potongan baru |

## 7. Urutan yang disarankan

P1 → P2 → P3 (sudah bernilai tanpa konten baru) → siapkan 5–10 soal cerita M01 → P4 → P6 → P5 (paling akhir, kontennya paling sulit).

Perbarui tabel status di `SERAH_TERIMA_PROYEK_PETA_BELAJAR.md` setelah tiap tahap selesai.
