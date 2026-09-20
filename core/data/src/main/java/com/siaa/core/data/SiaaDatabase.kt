package com.siaa.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        KnowledgeComponentEntity::class,
        KnowledgeEdgeEntity::class,
        LearnerKcStateEntity::class,
        ExerciseEntity::class,
        SessionEntity::class,
        InteractionEntity::class,
        DeviceProfileEntity::class,
        MisconceptionEntity::class,
        AppMetaEntity::class,
        RuntimeEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SiaaDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun learnerDao(): LearnerDao
    abstract fun sessionDao(): SessionDao
    abstract fun deviceDao(): DeviceDao
    abstract fun metaDao(): MetaDao

    companion object {
        fun build(context: Context): SiaaDatabase = Room.databaseBuilder(
            context.applicationContext,
            SiaaDatabase::class.java,
            "siaa.db"
        )
            .addMigrations(com.siaa.core.data.migration.MIGRATION_1_2)
            .build()
    }
}

