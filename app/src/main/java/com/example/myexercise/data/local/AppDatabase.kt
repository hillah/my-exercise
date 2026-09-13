package com.example.myexercise.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myexercise.data.local.entity.DailySummaryEntity
import com.example.myexercise.data.local.entity.ExerciseLogEntity
import com.example.myexercise.data.local.entity.ExerciseTypeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExerciseTypeEntity::class,
        ExerciseLogEntity::class,
        DailySummaryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v1からv2へのスキーマ変更なし
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_summaries ADD COLUMN stretchSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_summaries ADD COLUMN stretchSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "my_exercise.db"
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
            .fallbackToDestructiveMigration()
            .build()
        }

        val DEFAULT_TYPES = listOf(
            ExerciseTypeEntity(
                id = 1L,
                name = "スクワット",
                iconKey = "squat",
                defaultCount = 10,
                unit = "回",
                colorHex = "#4CAF50",
                displayOrder = 1
            ),
            ExerciseTypeEntity(
                id = 2L,
                name = "腕立て伏せ",
                iconKey = "pushup",
                defaultCount = 10,
                unit = "回",
                colorHex = "#2196F3",
                displayOrder = 2
            ),
            ExerciseTypeEntity(
                id = 3L,
                name = "腹筋",
                iconKey = "situp",
                defaultCount = 15,
                unit = "回",
                colorHex = "#FF9800",
                displayOrder = 3
            ),
            ExerciseTypeEntity(
                id = 4L,
                name = "ストレッチ",
                iconKey = "stretch",
                defaultCount = 60,
                unit = "秒",
                colorHex = "#9C27B0",
                displayOrder = 4
            )
        )
    }
}
