package de.markusfisch.android.clearly.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataSource() {
	companion object {
		val DECISIONS = "decisions"
		val DECISIONS_ID = "_id"
		val DECISIONS_NAME = "name"
		val DECISIONS_CREATED = "created"
		val DECISIONS_CREATED_STRING = "created_string"

		val ARGUMENTS = "arguments"
		val ARGUMENTS_ID = "_id"
		val ARGUMENTS_DECISION = "fk_decision"
		val ARGUMENTS_TEXT = "text"
		val ARGUMENTS_WEIGHT = "weight"
		val ARGUMENTS_ORDER = "sort_order"

		private fun createDecisions(db: SQLiteDatabase) {
			db.execSQL("DROP TABLE IF EXISTS $DECISIONS")
			db.execSQL("""CREATE TABLE $DECISIONS (
					$DECISIONS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
					$DECISIONS_NAME TEXT,
					$DECISIONS_CREATED DATETIME)""")
		}

		private fun createArguments(db: SQLiteDatabase) {
			db.execSQL("DROP TABLE IF EXISTS $ARGUMENTS")
			db.execSQL("""CREATE TABLE $ARGUMENTS (
					$ARGUMENTS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
					$ARGUMENTS_DECISION INTEGER,
					$ARGUMENTS_TEXT TEXT NOT NULL,
					$ARGUMENTS_WEIGHT INTEGER,
					$ARGUMENTS_ORDER INTEGER)""")
		}

		private fun now(): String {
			return SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss",
					Locale.US).format(Date())
		}
	}

	private lateinit var db: SQLiteDatabase

	fun open(context: Context): Boolean {
		return try {
			db = OpenHelper(context).getWritableDatabase()
			true
		} catch (e: SQLException) {
			false
		}
	}

	fun getDecisions(): Cursor {
		return db.rawQuery("""SELECT
				$DECISIONS_ID,
				$DECISIONS_NAME,
				strftime('%Y-%m-%d %H:%M',
						$DECISIONS_CREATED) AS $DECISIONS_CREATED_STRING
				FROM $DECISIONS
				ORDER BY $DECISIONS_CREATED DESC""", null)
	}

	fun getDecisionName(id: Long): String {
		return queryStringColumn("""SELECT
				$DECISIONS_NAME
				FROM $DECISIONS
				WHERE $DECISIONS_ID = $id""", DECISIONS_NAME)
	}

	fun insertDecision(): Long {
		val cv = ContentValues()
		cv.put(DECISIONS_CREATED, now())
		return db.insert(DECISIONS, null, cv)
	}

	fun removeDecision(id: Long) {
		db.delete(ARGUMENTS, "$ARGUMENTS_DECISION = $id", null)
		db.delete(DECISIONS, "$DECISIONS_ID = $id", null)
	}

	fun updateDecisionName(id: Long, name: String) {
		val cv = ContentValues()
		cv.put(DECISIONS_NAME, name)
		db.update(DECISIONS, cv, "$DECISIONS_ID = $id", null)
	}

	fun getArguments(decisionId: Long): Cursor {
		return db.rawQuery("""SELECT
				$ARGUMENTS_ID,
				$ARGUMENTS_TEXT,
				$ARGUMENTS_WEIGHT,
				$ARGUMENTS_ORDER
				FROM $ARGUMENTS
				WHERE $ARGUMENTS_DECISION = $decisionId
				ORDER BY $ARGUMENTS_ORDER, $ARGUMENTS_ID""", null)
	}

	fun getArgumentText(id: Long): String {
		return queryStringColumn("""SELECT
				$ARGUMENTS_TEXT
				FROM $ARGUMENTS
				WHERE $ARGUMENTS_ID = $id""", ARGUMENTS_TEXT)
	}

	fun insertArgument(decisionId: Long, text: String, weight: Int): Long {
		val cv = ContentValues()
		cv.put(ARGUMENTS_DECISION, decisionId)
		cv.put(ARGUMENTS_TEXT, text)
		cv.put(ARGUMENTS_WEIGHT, weight)
		cv.put(ARGUMENTS_ORDER, 0)
		return db.insert(ARGUMENTS, null, cv)
	}

	fun removeArgument(id: Long) {
		db.delete(ARGUMENTS, "$ARGUMENTS_ID = $id", null)
	}

	fun updateArgumentText(id: Long, text: String) {
		val cv = ContentValues()
		cv.put(ARGUMENTS_TEXT, text)
		db.update(ARGUMENTS, cv, "$ARGUMENTS_ID = $id", null)
	}

	fun updateArgumentWeight(id: Long, weight: Int) {
		val cv = ContentValues()
		cv.put(ARGUMENTS_WEIGHT, weight)
		db.update(ARGUMENTS, cv, "$ARGUMENTS_ID = $id", null)
	}

	fun sortArguments(decisionId: Long) {
		// execSQL() instead of update() because we can't use column
		// names in ContentValues; another leaking abstraction
		db.execSQL("""UPDATE $ARGUMENTS
				SET $ARGUMENTS_ORDER = $ARGUMENTS_WEIGHT
				WHERE $ARGUMENTS_DECISION = $decisionId""")
	}

	private fun queryStringColumn(query: String, column: String): String {
		val cursor: Cursor? = db.rawQuery(query, null)

		if (cursor == null) {
			return ""
		} else if (!cursor.moveToFirst()) {
			cursor.close()
			return ""
		}

		val text: String? = cursor.getString(cursor.getColumnIndex(column))
		cursor.close()

		return text ?: ""
	}

	private class OpenHelper(context: Context):
			SQLiteOpenHelper(context, "arguments.db", null, 1) {
		override fun onCreate(db: SQLiteDatabase) {
			createDecisions(db)
			createArguments(db)
		}

		override fun onUpgrade(
				db: SQLiteDatabase,
				oldVersion: Int,
				newVersion: Int) {
		}
	}
}
