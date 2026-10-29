package ai.mlc.mlcchat.enterprise

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Local app-private database. Chat text is not encrypted at rest. */
class ChatHistoryDatabase(context: Context) : SQLiteOpenHelper(context, "enterprise_history.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, model TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, session_id INTEGER NOT NULL, role TEXT NOT NULL, content TEXT NOT NULL, FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX idx_messages_session ON messages(session_id)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun saveSnapshot(model: String, messages: List<Pair<String, String>>): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val session = ContentValues().apply { put("model", model); put("created_at", System.currentTimeMillis()) }
            val id = db.insertOrThrow("sessions", null, session)
            messages.forEach { (role, content) ->
                db.insertOrThrow("messages", null, ContentValues().apply {
                    put("session_id", id); put("role", role); put("content", content)
                })
            }
            db.setTransactionSuccessful()
            return id
        } finally { db.endTransaction() }
    }
    fun sessions(): List<SessionSummary> {
        val result = mutableListOf<SessionSummary>()
        readableDatabase.rawQuery("SELECT s.id,s.model,s.created_at,COUNT(m.id) FROM sessions s LEFT JOIN messages m ON m.session_id=s.id GROUP BY s.id ORDER BY s.created_at DESC LIMIT 200", null).use { c ->
            while (c.moveToNext()) result.add(SessionSummary(c.getLong(0), c.getString(1), c.getLong(2), c.getInt(3)))
        }
        return result
    }
    fun messages(sessionId: Long): List<Pair<String,String>> {
        val result = mutableListOf<Pair<String,String>>()
        readableDatabase.rawQuery("SELECT role,content FROM messages WHERE session_id=? ORDER BY id", arrayOf(sessionId.toString())).use { c ->
            while(c.moveToNext()) result.add(c.getString(0) to c.getString(1))
        }
        return result
    }
    fun delete(sessionId: Long) { writableDatabase.delete("sessions", "id=?", arrayOf(sessionId.toString())); writableDatabase.delete("messages", "session_id=?", arrayOf(sessionId.toString())) }
}
data class SessionSummary(val id: Long, val model: String, val createdAt: Long, val messageCount: Int)
