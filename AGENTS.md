# Aturan Proyek MRI-learn-v4 (AGENTS.md)

Panduan dan batasan teknis untuk agen & kontributor yang bekerja di repositori ini:

1. **Jangan ubah logika `MriViewModel.kt`**: kecuali diminta eksplisit; logika pemetaan Peta Belajar, XP, reward, kuis tingkat, dan label tampilan diletakkan di fungsi/kelas murni pada `com.example.util` atau state holder UI (`com.example.ui.path`).
2. **Keamanan Database Room**:
   - DILARANG keras menggunakan `fallbackToDestructiveMigration()` pada kode produksi (`MriDatabase.kt`). Tabel data pengguna (`node_progress`, `study_notes`, `quiz_attempts`, `study_days`) berada di file DB yang sama.
   - Setiap perubahan skema wajib menaikkan `DB_VERSION`, mengekspor schema Room, dan menulis `Migration` nyata di `Migrations.kt`.
3. **Kompatibilitas Kode Simpul**:
   - Selalu pertahankan `LegacyNodeCodes.normalize(...)` dan `LegacyNodeCodes.normalizeAll(...)` saat membandingkan atau menyimpan kode simpul.
   - Jangan mengubah kode simpul di basis data atau data kurikulum yang ada (`MriCurriculumData.kt`), dan pertahankan ID/nodeCode unik serta minimal 1 kuis per simpul.
4. **Navigasi & Pemulihan Scroll**:
   - Pertahankan alur `NavSession`, `BackHandler` alami (Pembaca -> Peta Modul -> Beranda), dan pemulihan scroll (`RememberSavedScrollState`).
5. **Tema & Aksesibilitas**:
   - Gunakan token tema aplikasi (`Color.kt` / `MaterialTheme.colorScheme`) dan komponen yang ada; jangan menyalin warna mentah dari mockup.
   - Pastikan target sentuh minimal `48.dp` untuk tombol interaktif dan `contentDescription` deskriptif.
6. **Pengujian & Verifikasi**:
   - Ekstrak logika baru ke fungsi murni di `com.example.util` disertai unit test JVM (`app/src/test/java/com/example/`).
   - Jangan jalankan build Gradle/emulator secara lokal jika pengguna meminta verifikasi lewat GitHub Actions / AI Studio.
