package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Satu-satunya sumber kebenaran versi skema [MriDatabase].
 * Naikkan nilai ini HANYA bersamaan dengan menulis Migration yang nyata (lihat prosedur di bawah).
 */
const val DB_VERSION = 14

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE study_days ADD COLUMN hasFlashcardActivity INTEGER NOT NULL DEFAULT 0"
        )
        // Baris lama yang sudah bertanda flashcard ikut ditandai.
        db.execSQL(
            "UPDATE study_days SET hasFlashcardActivity = 1 WHERE lastNodeCode = 'FLASHCARD_REVIEW'"
        )
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `mri_nodes`")
        db.execSQL("DROP TABLE IF EXISTS `quiz_questions`")
    }
}

/**
 * Daftar migrasi yang benar-benar ada.
 *
 * Prosedur saat skema berubah (mis. 12 -> 13):
 * 1. Ubah entity, naikkan DB_VERSION menjadi 13.
 * 2. Build sekali agar Room mengekspor schemas/.../13.json, lalu commit file itu.
 * 3. Tulis `val MIGRATION_12_13 = object : Migration(12, 13) { ... }` dengan SQL nyata yang
 *    menyamai 13.json, dan JANGAN menyentuh tabel data pengguna (node_progress, study_notes,
 *    quiz_attempts, study_days) kecuali dengan ALTER TABLE atau tabel sementara yang menyalin data.
 * 4. Daftarkan di ALL_MIGRATIONS DI SINI, dan baru pada langkah ini.
 * 5. Jalankan MigrationChainTest (JVM) dan MriMigrationTest (perangkat) sampai hijau.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Sumber penyelesaian simpul ('READ' vs 'SKIP_TEST') untuk fitur Lompat dengan tes singkat.
        db.execSQL("ALTER TABLE node_progress ADD COLUMN source TEXT NOT NULL DEFAULT 'READ'")
    }
}

private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
    db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
        val nameIdx = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIdx >= 0 && cursor.getString(nameIdx).equals(columnName, ignoreCase = true)) {
                return true
            }
        }
    }
    return false
}

private fun ensureBaselineV11Schema(db: SupportSQLiteDatabase) {
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `node_progress` (`nodeCode` TEXT NOT NULL, `completedAt` INTEGER NOT NULL, `moduleId` TEXT NOT NULL, PRIMARY KEY(`nodeCode`))"
    )
    if (!hasColumn(db, "node_progress", "moduleId")) {
        db.execSQL("ALTER TABLE `node_progress` ADD COLUMN `moduleId` TEXT NOT NULL DEFAULT ''")
    }
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_node_progress_nodeCode` ON `node_progress` (`nodeCode`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_node_progress_moduleId` ON `node_progress` (`moduleId`)")

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `study_notes` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `category` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `nodeCode` TEXT, `moduleId` TEXT, PRIMARY KEY(`id`))"
    )
    if (!hasColumn(db, "study_notes", "nodeCode")) {
        db.execSQL("ALTER TABLE `study_notes` ADD COLUMN `nodeCode` TEXT")
    }
    if (!hasColumn(db, "study_notes", "moduleId")) {
        db.execSQL("ALTER TABLE `study_notes` ADD COLUMN `moduleId` TEXT")
    }
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_notes_category` ON `study_notes` (`category`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_notes_moduleId` ON `study_notes` (`moduleId`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_notes_nodeCode` ON `study_notes` (`nodeCode`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_notes_updatedAt` ON `study_notes` (`updatedAt`)")

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `quiz_attempts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `quizId` TEXT NOT NULL, `nodeCode` TEXT NOT NULL, `moduleId` TEXT NOT NULL, `selectedIndex` INTEGER NOT NULL, `isCorrect` INTEGER NOT NULL, `answeredAt` INTEGER NOT NULL)"
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_quizId` ON `quiz_attempts` (`quizId`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_nodeCode` ON `quiz_attempts` (`nodeCode`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_moduleId` ON `quiz_attempts` (`moduleId`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_answeredAt` ON `quiz_attempts` (`answeredAt`)")

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `study_days` (`dateIso` TEXT NOT NULL, `nodesCompletedCount` INTEGER NOT NULL, `lastNodeCode` TEXT, `lastModuleId` TEXT, PRIMARY KEY(`dateIso`))"
    )
    if (!hasColumn(db, "study_days", "lastNodeCode")) {
        db.execSQL("ALTER TABLE `study_days` ADD COLUMN `lastNodeCode` TEXT")
    }
    if (!hasColumn(db, "study_days", "lastModuleId")) {
        db.execSQL("ALTER TABLE `study_days` ADD COLUMN `lastModuleId` TEXT")
    }
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_days_dateIso` ON `study_days` (`dateIso`)")

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `mri_modules` (`id` TEXT NOT NULL, `code` TEXT NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `description` TEXT NOT NULL, `levelBadge` TEXT NOT NULL, `iconName` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_mri_modules_id` ON `mri_modules` (`id`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_mri_modules_sortOrder` ON `mri_modules` (`sortOrder`)")

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `curriculum_nodes` (`code` TEXT NOT NULL, `moduleId` TEXT NOT NULL, `level` INTEGER NOT NULL, `title` TEXT NOT NULL, `summaryShort` TEXT NOT NULL, `detailFull` TEXT NOT NULL, `analogi` TEXT NOT NULL, `prerequisite` TEXT, `learningGoal` TEXT, `crossLinks` TEXT, `labType` TEXT, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`code`))"
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_nodes_code` ON `curriculum_nodes` (`code`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_nodes_moduleId` ON `curriculum_nodes` (`moduleId`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_nodes_level` ON `curriculum_nodes` (`level`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_nodes_sortOrder` ON `curriculum_nodes` (`sortOrder`)")

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `curriculum_quizzes` (`id` TEXT NOT NULL, `moduleId` TEXT NOT NULL, `nodeCode` TEXT NOT NULL, `level` INTEGER NOT NULL, `question` TEXT NOT NULL, `optionsJson` TEXT NOT NULL, `correctIndex` INTEGER NOT NULL, `explanation` TEXT NOT NULL, PRIMARY KEY(`id`))"
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_quizzes_id` ON `curriculum_quizzes` (`id`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_quizzes_moduleId` ON `curriculum_quizzes` (`moduleId`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_quizzes_nodeCode` ON `curriculum_quizzes` (`nodeCode`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_curriculum_quizzes_level` ON `curriculum_quizzes` (`level`)")
}

private val LEGACY_PRE_11_MIGRATIONS: Array<Migration> = (1..10).map { fromVer ->
    object : Migration(fromVer, fromVer + 1) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (fromVer + 1 == 11) {
                ensureBaselineV11Schema(db)
            }
        }
    }
}.toTypedArray()

val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    *LEGACY_PRE_11_MIGRATIONS,
    MIGRATION_11_12,
    MIGRATION_12_13,
    MIGRATION_13_14
)

