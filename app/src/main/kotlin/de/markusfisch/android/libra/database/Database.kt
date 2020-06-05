package de.markusfisch.android.libra.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class Database {
	data class Argument(val text: String, val weight: Int)

	private lateinit var db: SQLiteDatabase

	fun open(context: Context) {
		db = OpenHelper(context).writableDatabase
	}

	fun getIssues(): Cursor? {
		return db.rawQuery(
			"""SELECT
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
				ORDER BY d.$ISSUES_CREATED DESC""",
			null
		)
	}

	fun getIssueName(id: Long) = db.rawQuery(
		"""SELECT
			$ISSUES_NAME
			FROM $ISSUES
			WHERE $ISSUES_ID = ?""",
		arrayOf("$id")
	)?.use {
		if (it.moveToFirst()) {
			it.getString(it.getColumnIndex(ISSUES_NAME))
		} else {
			null
		}
	} ?: ""

	fun insertIssue(): Long {
		val cv = ContentValues()
		cv.put(ISSUES_CREATED, now())
		return db.insert(ISSUES, null, cv)
	}

	fun removeIssue(id: Long) {
		val args = arrayOf("$id")
		db.delete(ARGUMENTS, "$ARGUMENTS_ISSUE = ?", args)
		db.delete(ISSUES, "$ISSUES_ID = ?", args)
	}

	fun updateIssueName(id: Long, name: String) {
		val cv = ContentValues()
		cv.put(ISSUES_NAME, name)
		db.update(ISSUES, cv, "$ISSUES_ID = ?", arrayOf("$id"))
	}

	fun getArguments(issueId: Long, sorted: Boolean = false): Cursor? = db.rawQuery(
		"""SELECT
			$ARGUMENTS_ID,
			$ARGUMENTS_TEXT,
			$ARGUMENTS_WEIGHT,
			$ARGUMENTS_ORDER
			FROM $ARGUMENTS
			WHERE $ARGUMENTS_ISSUE = ?
			ORDER BY ${if (sorted)
			ARGUMENTS_WEIGHT else
			"$ARGUMENTS_ORDER, $ARGUMENTS_ID"}""",
		arrayOf("$issueId")
	)

	fun getArgument(id: Long): Argument? = db.rawQuery(
		"""SELECT
			$ARGUMENTS_TEXT,
			$ARGUMENTS_WEIGHT
			FROM $ARGUMENTS
			WHERE $ARGUMENTS_ID = ?""",
		arrayOf("$id")
	)?.use {
		if (it.moveToFirst()) {
			Argument(
				it.getString(it.getColumnIndex(ARGUMENTS_TEXT)),
				it.getInt(it.getColumnIndex(ARGUMENTS_WEIGHT))
			)
		} else {
			null
		}
	}

	fun insertArgument(issueId: Long, text: String, weight: Int): Long {
		val cv = ContentValues()
		cv.put(ARGUMENTS_ISSUE, issueId)
		cv.put(ARGUMENTS_TEXT, text)
		cv.put(ARGUMENTS_WEIGHT, weight)
		cv.put(ARGUMENTS_ORDER, 999999)
		return db.insert(ARGUMENTS, null, cv)
	}

	fun removeArgument(id: Long) {
		db.delete(ARGUMENTS, "$ARGUMENTS_ID = ?", arrayOf("$id"))
	}

	fun updateArgument(id: Long, text: String, weight: Int) {
		val cv = ContentValues()
		cv.put(ARGUMENTS_TEXT, text)
		cv.put(ARGUMENTS_WEIGHT, weight)
		db.update(ARGUMENTS, cv, "$ARGUMENTS_ID = ?", arrayOf("$id"))
	}

	fun sortArguments(issueId: Long) {
		// execSQL() instead of update() because we can't use column
		// names in values of ContentValues; another leaking abstraction
		db.execSQL(
			"""UPDATE $ARGUMENTS
				SET $ARGUMENTS_ORDER = $ARGUMENTS_WEIGHT
				WHERE $ARGUMENTS_ISSUE = ?""",
			arrayOf("$issueId")
		)
	}

	private class OpenHelper(context: Context) :
		SQLiteOpenHelper(context, "arguments.db", null, 1) {
		override fun onCreate(db: SQLiteDatabase) {
			createIssues(db)
			createArguments(db)
		}

		override fun onUpgrade(
			db: SQLiteDatabase,
			oldVersion: Int,
			newVersion: Int
		) {
		}
	}

	companion object {
		const val ISSUES = "issues"
		const val ISSUES_ID = "_id"
		const val ISSUES_NAME = "name"
		const val ISSUES_CREATED = "created"
		const val ISSUES_CREATED_TIMESTAMP = "created_timestamp"
		const val ISSUES_NEGATIVE = "negative"
		const val ISSUES_POSITIVE = "positive"

		const val ARGUMENTS = "arguments"
		const val ARGUMENTS_ID = "_id"
		const val ARGUMENTS_ISSUE = "fk_issue"
		const val ARGUMENTS_TEXT = "text"
		const val ARGUMENTS_WEIGHT = "weight"
		const val ARGUMENTS_ORDER = "sort_order"

		private fun createIssues(db: SQLiteDatabase) {
			db.execSQL("DROP TABLE IF EXISTS $ISSUES")
			db.execSQL(
				"""CREATE TABLE $ISSUES (
					$ISSUES_ID INTEGER PRIMARY KEY AUTOINCREMENT,
					$ISSUES_NAME TEXT,
					$ISSUES_CREATED DATETIME)"""
			)
		}

		private fun createArguments(db: SQLiteDatabase) {
			db.execSQL("DROP TABLE IF EXISTS $ARGUMENTS")
			db.execSQL(
				"""CREATE TABLE $ARGUMENTS (
					$ARGUMENTS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
					$ARGUMENTS_ISSUE INTEGER,
					$ARGUMENTS_TEXT TEXT NOT NULL,
					$ARGUMENTS_WEIGHT INTEGER,
					$ARGUMENTS_ORDER INTEGER)"""
			)
		}
	}
}

fun now(): String {
	return SimpleDateFormat(
		"yyyy-MM-dd HH:mm:ss",
		Locale.US
	).format(Date())
}
