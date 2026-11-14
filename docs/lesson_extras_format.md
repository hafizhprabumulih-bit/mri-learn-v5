# Format Berkas `lesson_extras.json`

Berkas `app/src/main/assets/lesson_extras.json` menyimpan konten opsional untuk **Mode Pelajaran Bertahap** tanpa mengubah kurikulum inti (`MriCurriculumData.kt`).

## Aturan Validasi Pakar (`reviewed`)
1. Setiap bagian `nanoHints` (`nanoHintsReviewed`), `recap` (`recapReviewed`), soal cerita (`stories[]`), dan rantai langkah interaktif (`interactive`) memiliki atribut `"reviewed": false` secara bawaan.
2. Konten dengan status `reviewed = false` **tidak akan ditampilkan ke pengguna umum** pada mode produksi kecuali sakelar pengembang pratinjau draf diaktifkan dalam build debug (`BuildConfig.DEBUG`).
3. Hanya pakar radiologi / fisikawan medis MRI yang berwenang mengubah status `reviewed` / `nanoHintsReviewed` / `recapReviewed` menjadi `true` beserta `"reviewedBy"` dan `"reviewedAt"`.

## Struktur JSON

```json
{
  "version": 1,
  "nodes": {
    "M01.T0": {
      "nanoHints": [
        "Komentar singkat Nano pada langkah penjelasan 1 (maks 90 karakter)."
      ],
      "nanoHintsReviewed": false,
      "recap": [
        "Butir rangkuman 1 untuk layar Selesai",
        "Butir rangkuman 2 untuk layar Selesai"
      ],
      "recapReviewed": false,
      "stories": [
        {
          "id": "story:M01.T0:1",
          "scenario": "Narasi kasus ruang konsol atau pasien.",
          "question": "Pertanyaan pemahaman kasus?",
          "options": [
            "Pilihan A",
            "Pilihan B",
            "Pilihan C",
            "Pilihan D"
          ],
          "correctIndex": 0,
          "explanation": "Penjelasan ilmiah mengapa jawaban tersebut tepat.",
          "followUps": [
            "Pertanyaan lanjutan opsional"
          ],
          "reviewed": false,
          "reviewedBy": null,
          "reviewedAt": null
        }
      ],
      "interactive": {
        "type": "REVEAL_CHAIN",
        "title": "Judul Lab Mini",
        "intro": "Kalimat pengantar singkat.",
        "steps": [
          { "text": "Langkah 1 yang langsung terbuka." },
          {
            "text": "Langkah 2 yang terbuka setelah ditebak.",
            "prediction": {
              "question": "Pertanyaan prediksi sebelum langkah 2 dibuka?",
              "options": ["Opsi 1", "Opsi 2", "Opsi 3"],
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
