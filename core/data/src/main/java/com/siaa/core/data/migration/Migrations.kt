package com.siaa.core.data.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add columns to learner_kc_state
        db.execSQL("ALTER TABLE learner_kc_state ADD COLUMN exposureCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE learner_kc_state ADD COLUMN lastExposedAtEpochMs INTEGER DEFAULT NULL")

        // 2. Add columns to exercises
        db.execSQL("ALTER TABLE exercises ADD COLUMN misconceptionIdsCsv TEXT NOT NULL DEFAULT ''")

        // 3. Add columns to device_profiles
        db.execSQL("ALTER TABLE device_profiles ADD COLUMN primaryKeyCode INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE device_profiles ADD COLUMN secondaryKeyCode INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE device_profiles ADD COLUMN backKeyCode INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE device_profiles ADD COLUMN stopKeyCode INTEGER DEFAULT NULL")

        // 4. Recreate interactions table with turnId unique index, graded, kind
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS interactions_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                turnId INTEGER NOT NULL DEFAULT 0,
                exerciseId TEXT NOT NULL,
                timestampEpochMs INTEGER NOT NULL,
                response TEXT NOT NULL,
                correct INTEGER NOT NULL,
                graded INTEGER NOT NULL DEFAULT 1,
                kind TEXT NOT NULL DEFAULT 'GRADED_RESPONSE',
                confidence TEXT,
                latencyMs INTEGER,
                hintDepth INTEGER NOT NULL,
                plannerScore REAL,
                stateBeforeMastery REAL,
                stateAfterMastery REAL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO interactions_new (
                id, sessionId, turnId, exerciseId, timestampEpochMs, response, correct, graded, kind,
                confidence, latencyMs, hintDepth, plannerScore, stateBeforeMastery, stateAfterMastery
            )
            SELECT id, sessionId, id, exerciseId, timestampEpochMs, response, correct, 1, 'GRADED_RESPONSE',
                   confidence, latencyMs, hintDepth, plannerScore, stateBeforeMastery, stateAfterMastery
            FROM interactions
        """.trimIndent())

        db.execSQL("DROP TABLE interactions")
        db.execSQL("ALTER TABLE interactions_new RENAME TO interactions")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_interactions_sessionId_turnId ON interactions (sessionId, turnId)")

        // 5. Create runtime_events table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS runtime_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                turnId INTEGER NOT NULL,
                timestampEpochMs INTEGER NOT NULL,
                eventType TEXT NOT NULL,
                stateBefore TEXT NOT NULL,
                stateAfter TEXT NOT NULL,
                exerciseId TEXT,
                runtimeCommand TEXT,
                mediaKeyCode INTEGER,
                payload TEXT
            )
        """.trimIndent())
    }
}
