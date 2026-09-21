package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SpeedDialEntity::class,
        ScreeningRuleEntity::class,
        QuickDeclineMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SalimDatabase : RoomDatabase() {
    abstract fun speedDialDao(): SpeedDialDao
    abstract fun screeningRuleDao(): ScreeningRuleDao

    companion object {
        @Volatile
        private var INSTANCE: SalimDatabase? = null

        fun getDatabase(context: Context): SalimDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalimDatabase::class.java,
                    "salim_phone_db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default quick decline messages and rules
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).screeningRuleDao()
                            dao.insertQuickMessage(QuickDeclineMessageEntity(text = "Sorry, I can't talk right now."))
                            dao.insertQuickMessage(QuickDeclineMessageEntity(text = "I'm on my way."))
                            dao.insertQuickMessage(QuickDeclineMessageEntity(text = "Can I call you later?"))
                            dao.insertQuickMessage(QuickDeclineMessageEntity(text = "I'm in a meeting."))
                            dao.setRule(ScreeningRuleEntity(id = "block_private", enabled = false))
                            dao.setRule(ScreeningRuleEntity(id = "block_unknown", enabled = false))
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
