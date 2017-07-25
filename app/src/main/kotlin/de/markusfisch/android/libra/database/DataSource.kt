package de.markusfisch.android.libra.database

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
		val ISSUES = "issues"
		val ISSUES_ID = "_id"
		val ISSUES_NAME = "name"
		val ISSUES_CREATED = "created"
		val ISSUES_CREATED_TIMESTAMP = "created_timestamp"
		val ISSUES_NEGATIVE = "negative"
		val ISSUES_POSITIVE = "positive"

		val ARGUMENTS = "arguments"
		val ARGUMENTS_ID = "_id"
		val ARGUMENTS_ISSUE = "fk_issue"
		val ARGUMENTS_TEXT = "text"
		val ARGUMENTS_WEIGHT = "weight"
		val ARGUMENTS_ORDER = "sort_order"

		private fun createIssues(db: SQLiteDatabase) {
			db.execSQL("DROP TABLE IF EXISTS $ISSUES")
			db.execSQL("""CREATE TABLE $ISSUES (
					$ISSUES_ID INTEGER PRIMARY KEY AUTOINCREMENT,
					$ISSUES_NAME TEXT,
					$ISSUES_CREATED DATETIME)""")
		}

		private fun createArguments(db: SQLiteDatabase) {
			db.execSQL("DROP TABLE IF EXISTS $ARGUMENTS")
			db.execSQL("""CREATE TABLE $ARGUMENTS (
					$ARGUMENTS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
					$ARGUMENTS_ISSUE INTEGER,
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
			db = OpenHelper(context).writableDatabase
			true
		} catch (e: SQLException) {
			false
		}
	}

	fun getIssues(): Cursor {
		return db.rawQuery("""SELECT
				d.$ISSUES_ID,
				d.$ISSUES_NAME,
				strftime('%s', d.$ISSUES_CREATED, 'utc')
					AS $ISSUES_CREATED_TIMESTAMP,
				(SELECT SUM(a.$ARGUMENTS_WEIGHT)
					FROM $ARGUMENTS AS a
					WHERE a.$ARGUMENTS_ISSUE = d.$ISSUES_ID AND
						a.$ARGUMENTS_WEIGHT < 0) AS $ISSUES_NEGATIVE,
				(SELECT SUM(a.$ARGUMENTS_WEIGHT)
					FROM $ARGUMENTS AS a
					WHERE a.$ARGUMENTS_ISSUE = d.$ISSUES_ID AND
						a.$ARGUMENTS_WEIGHT > 0) AS $ISSUES_POSITIVE
				FROM $ISSUES AS d
				ORDER BY d.$ISSUES_CREATED DESC""", null)
	}

	fun getIssueName(id: Long): String {
		return queryStringColumn("""SELECT
				$ISSUES_NAME
				FROM $ISSUES
				WHERE $ISSUES_ID = $id""", ISSUES_NAME)
	}

	fun insertIssue(): Long {
		val cv = ContentValues()
		cv.put(ISSUES_CREATED, now())
		return db.insert(ISSUES, null, cv)
	}

	fun removeIssue(id: Long) {
		db.delete(ARGUMENTS, "$ARGUMENTS_ISSUE = $id", null)
		db.delete(ISSUES, "$ISSUES_ID = $id", null)
	}

	fun updateIssueName(id: Long, name: String) {
		val cv = ContentValues()
		cv.put(ISSUES_NAME, name)
		db.update(ISSUES, cv, "$ISSUES_ID = $id", null)
	}

	fun getArguments(issueId: Long): Cursor {
		return db.rawQuery("""SELECT
				$ARGUMENTS_ID,
				$ARGUMENTS_TEXT,
				$ARGUMENTS_WEIGHT,
				$ARGUMENTS_ORDER
				FROM $ARGUMENTS
				WHERE $ARGUMENTS_ISSUE = $issueId
				ORDER BY $ARGUMENTS_ORDER, $ARGUMENTS_ID""", null)
	}

	fun getArgumentText(id: Long): String {
		return queryStringColumn("""SELECT
				$ARGUMENTS_TEXT
				FROM $ARGUMENTS
				WHERE $ARGUMENTS_ID = $id""", ARGUMENTS_TEXT)
	}

	fun insertArgument(issueId: Long, text: String, weight: Int): Long {
		val cv = ContentValues()
		cv.put(ARGUMENTS_ISSUE, issueId)
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

	fun sortArguments(issueId: Long) {
		// execSQL() instead of update() because we can't use column
		// names in ContentValues; another leaking abstraction
		db.execSQL("""UPDATE $ARGUMENTS
				SET $ARGUMENTS_ORDER = $ARGUMENTS_WEIGHT
				WHERE $ARGUMENTS_ISSUE = $issueId""")
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
			createIssues(db)
			createArguments(db)
		}

		override fun onUpgrade(
				db: SQLiteDatabase,
				oldVersion: Int,
				newVersion: Int) {
		}
	}
}
