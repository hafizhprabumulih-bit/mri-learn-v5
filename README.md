# Belajar MRI (Android · Jetpack Compose)

Aplikasi pembelajaran interaktif fisika dasar, pembentukan citra, sekuens, keselamatan, serta artefak Magnetic Resonance Imaging (MRI) untuk mahasiswa radiologi dan radiografer.

## Fitur Utama Peta Belajar
- **Peta Kurikulum 9 Bab (18 Modul) di Beranda**: Mengelompokkan `M01`–`M09` menjadi 9 bab berurutan pada jalur zig-zag dengan penanda modul aktif (`CURRENT`), capaian `selesai/total`, serta pengingat rekomendasi `>= 60%` tanpa memblokir akses ke modul lain.
- **Peta Modul per Simpul (`ModulePathScreen`)**: Menampilkan seluruh **186 simpul** materi dengan pembagian sub-bagian (*Pendahuluan & Tujuan*, *Tingkat 1–3*), konektor progres, penanda simpul spesial (*Tujuan*, *Ringkasan*, *Lab*, *Kuis Evaluasi*), dan sakelar `PATH_STRICT_LOCKS`.
- **Bottom Sheet Pratinjau & Pembaca Fokus**: Mengetuk simpul menampilkan ringkasan, estimasi waktu baca, jumlah soal kuis, peringatan prasyarat lembut (*non-blocking*), tombol *Mulai belajar*, serta pintasan lab simulasi dan latihan kuis.
- **Lompat dengan Tes Singkat (`SkipTestBuilder`)**: Simpul terkunci menawarkan tes singkat berisi 2–3 soal yang diambil langsung dari simpul prasyarat yang akan dilewati (dengan cakupan soal penuh pada seluruh 186 simpul / 271 soal kuis). Lulus `>= 2/3` menandai irisan sebagai `SKIP_TEST` (`NodeStatus.SKIPPED`); membaca simpul tersebut kemudian menaikkannya menjadi `READ` (`NodeStatus.DONE`) dengan perolehan XP.
- **Selebrasi Simpul Selesai & XP Derivatif (`XpCalculator`)**: Menampilkan layar selesai dengan perolehan XP yang dihitung murni dari progres DB (`10 XP/simpul dibaca`, `5 XP/kuis benar unik`, `50 XP/modul tuntas 100%`, `0 XP/simpul dilewati`), status lab terbuka, serta pratinjau progres hadiah karakter.

## Struktur Komponen Peta Belajar
- `app/src/main/java/com/example/util/PathStateResolver.kt` — Resolver status simpul/modul (`DONE`, `SKIPPED`, `CURRENT`, `OPEN`, `LOCKED`), prasyarat lembut, dan `PATH_STRICT_LOCKS`.
- `app/src/main/java/com/example/util/HomePathPlanner.kt` — Pengelompokan 18 modul menjadi 9 bab di Beranda.
- `app/src/main/java/com/example/util/XpCalculator.kt` — Perhitungan XP derivatif & pratinjau hadiah karakter.
- `app/src/main/java/com/example/util/SkipTestBuilder.kt` — Penyusun soal dan ambang kelulusan tes lompat simpul.
- `app/src/main/java/com/example/ui/path/` — Komponen jalur zig-zag (`PathComponents.kt`, `PathGeometry.kt`), `ModulePathScreen.kt`, `ModulePathStateHolder.kt`, `ModuleRouteHost.kt`, `SkipTestDialog.kt`, dan `NodeCompletedScreen.kt`.
- `app/src/main/java/com/example/data/local/` — Room Database versi `14` dengan migrasi non-destruktif `MIGRATION_13_14` (`ALTER TABLE node_progress ADD COLUMN source TEXT NOT NULL DEFAULT 'READ'`).

## Pengujian & Build CI
Proyek dilengkapi alur GitHub Actions di `.github/workflows/android-ci.yml` yang menjalankan tes unit JVM/Robolectric dan build APK debug:

```bash
./gradlew testDebugUnitTest assembleDebug --stacktrace
```
*(Bila Gradle wrapper tidak tersedia di mesin lokal, jalankan `gradle testDebugUnitTest assembleDebug --stacktrace` dengan Gradle 9.3.1 & JDK 17).*
