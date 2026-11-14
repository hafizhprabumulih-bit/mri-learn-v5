# Panduan Lengkap Debug, Push, & Pengambilan APK di GitHub (MRI EDU)

Dokumen ini adalah panduan teknis resmi bagi pengembang dan kontributor proyek **MRI EDU v4.0.2** untuk melakukan sinkronisasi kode ke GitHub, memantau pipeline otomatisasi GitHub Actions CI, mengunduh file `app-debug.apk` langsung dari server GitHub, serta melakukan debugging lokal dan tata letak UI.

---

## 1. Persiapan & Push Kode ke GitHub

Jalankan serangkaian perintah git berikut dari root repositori:

```bash
# 1. Periksa status file yang berubah
git status

# 2. Tambahkan seluruh berkas perubahan ke staging
git add .

# 3. Lakukan commit dengan pesan terstruktur
git commit -m "feat: perbaiki layout larmor responsif, tombol tembak pulsa terjamin terlihat, dan upgrade dokumen debug"

# 4. Kirimkan perubahan ke cabang utama di GitHub
git push origin main
```

> **Catatan Pengaturan Remote Awal:** Jika remote GitHub belum dikonfigurasi pada direktori lokal Anda:
> ```bash
> git remote add origin https://github.com/<username_anda>/<nama_repo>.git
> git branch -M main
> git push -u origin main
> ```

---

## 2. Cara Mengambil APK Hasil Build di GitHub Actions

Setiap kali Anda melakukan `git push` ke cabang `main` atau membuka Pull Request, alur kerja **GitHub Actions (`.github/workflows/android-ci.yml`)** otomatis terpicu:

1. **Buka Web GitHub:**
   Akses repositori Anda di browser: `https://github.com/<username_anda>/<nama_repo>`.
2. **Masuk ke Tab Actions:**
   Klik tab **"Actions"** di bilah menu atas repositori.
3. **Pilih Workflow Run Terbaru:**
   Klik alur kerja paling atas yang baru saja berjalan (bernama **"Android CI"** dengan pesan commit Anda).
4. **Periksa Status Pipeline:**
   - Bila bertanda centang hijau **✅ Build Debug APK & Run Unit Tests**, seluruh 33 unit test lulus dan file APK berhasil dikompilasi!
   - Di halaman ringkasan (**Summary**), terdapat laporan run otomatis berformat markdown yang mencantumkan status, branch, commit SHA, dan panduan unduhan.
5. **Unduh Artefak APK:**
   - Gulir ke bagian paling bawah ke tabel **Artifacts**.
   - Klik **`mri-edu-debug-apk`** untuk mengunduh arsip zip.
   - Ekstrak berkas zip tersebut untuk mendapatkan file **`app-debug.apk`**.
   - Pasang file `app-debug.apk` ke ponsel Android Anda (atau jalankan di emulator).
6. **Memicu Build Manual (Workflow Dispatch):**
   Jika Anda ingin memicu build ulang kapan saja tanpa push commit baru:
   Masuk ke tab **Actions** → pilih **Android CI** di daftar sebelah kiri → klik tombol **"Run workflow"** → pilih cabang `main` → klik **Run workflow**.

---

## 3. Matriks Diagnostik Masalah Build & CI

| Gejala / Pesan Galat | Kemungkinan Penyebab | Tindakan Solusi |
|---|---|---|
| **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`** | Aplikasi lama di ponsel ditandatangani dengan keystore berbeda dari build CI baru. | Jalankan `adb uninstall com.example` sebelum memasang APK baru, atau hapus aplikasi manual dari layar ponsel terlebih dahulu. |
| **`java.lang.NullPointerException: Cannot invoke ... Application.getService` pada KSP** | Konflik runtime cache atau Gradle daemon thread pada KSP. | Jalankan `gradle --stop` lalu ulangi kompilasi dengan `gradle assembleDebug --stacktrace`. |
| **SDK Licenses not accepted** | Mesin CI belum menyetujui lisensi SDK Android Google. | File `.github/workflows/android-ci.yml` telah dipersenjatai skrip auto-accept lisensi pada `android-sdk-license` dan `android-sdk-preview-license`. |
| **`signingConfig` null / Missing debug.keystore** | File `debug.keystore` belum terbentuk di lingkungan fresh CI. | Alur CI memiliki langkah `Generate CI debug keystore if missing` yang otomatis menghasilkan keystore debug RSA 2048-bit. |
| **Unit Test FAILED di GitHub Actions** | Terdapat regresi logika pada perhitungan poin, Larmor, kurikulum, atau database. | Unduh artefak **`unit-test-reports`** pada tab Artifacts, buka `index.html` di browser untuk melihat baris pengujian yang gagal. |

---

## 4. Pengujian & Kompilasi Lokal di Komputer Anda

Sebelum melakukan push ke GitHub, Anda dapat memverifikasi aplikasi secara mandiri di komputer lokal:

### A. Menjalankan Seluruh Unit Test (JVM & Robolectric)
```bash
gradle testDebugUnitTest
```
Laporan HTML pengujian unit akan otomatis tersedia di:
`app/build/reports/tests/testDebugUnitTest/index.html`

### B. Membangun APK Debug Secara Lokal
```bash
gradle assembleDebug
```
File APK debug siap pakai akan dihasilkan di:
`app/build/outputs/apk/debug/app-debug.apk`

### C. Memasang Langsung ke Perangkat Fisik via Kabel USB (ADB)
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Ringkasan Optimalisasi Desain & Antarmuka Terbaru

1. **Layar Minigame Tembak Frekuensi Larmor (Solusi Tangkap Layar Pengguna):**
   - **Tombol Tembak 100% Terlihat:** Tombol `"⚡ TEMBAK PULSA RF 90°!"` dipindahkan ke tingkat kontainer luar di bawah kartu stage (menggunakan `Modifier.fillMaxWidth().height(52.dp)` dengan elevasi M3). Ini menjamin tombol selalu terlihat penuh dan tidak pernah terdorong keluar oleh navigation bar.
   - **Tata Letak Header 2 Baris Bebas Truncation:**
     - Baris 1: `Medan B₀: [Kekuatan Medan Lengkap]` di sisi kiri dan pill frekuensi resonansi `ω₀ = [Target] MHz` di sisi kanan. Tidak ada lagi teks terpotong `Medan B₀: 3,0...`.
     - Baris 2: `Formula: ω₀ = γ · B₀ (γ = 42,58 MHz/T untuk ¹H)` di sisi kiri dan lencana `Tingkat [X]` di sisi kanan.
   - **Konsol RF Komputer MRI Kompak:**
     - Mengubah label nilai medan menjadi ringkas (`3,0 T`, `1,5 T`) agar chip metrik tidak meluap (`3,0 T +...`).
     - Memperbaiki bobot bar perbandingan trade-off (`Sapuan & Presisi Waktu` dan `Margin SNR (FID)`) agar label teks tampil utuh tanpa tanda titik-titik.
2. **Opening Screen `mri_icon_bersih`:**
   - Ikon mesin MRI melingkar bersih menjadi aset pembuka aplikasi, bertransisi halus dengan durasi 2,4s dan zoom-out instan ke Beranda.
3. **Presisi Cincin Sorotan Tutorial Beranda (Map Guide):**
   - Menggunakan koordinat relatif terhadap kontainer overlay (`positionInRoot`) sehingga spotlight mengitari bilah aksi target secara pixel-perfect tanpa terpengaruh padding status bar.
