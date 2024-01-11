package de.markusfisch.android.libra.database

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.database.DatabaseErrorHandler
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import de.markusfisch.android.libra.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

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
				ORDER BY d.$ISSUES_CREATED DESC""".trimMargin(),
			null
		)
	}

	fun getIssueName(id: Long) = db.rawQuery(
		"""SELECT
			$ISSUES_NAME
			FROM $ISSUES
			WHERE $ISSUES_ID = ?""".trimMargin(),
		arrayOf("$id")
	)?.use {
		if (it.moveToFirst()) {
			it.getString(ISSUES_NAME)
		} else {
			null
		}
	} ?: ""

	fun insertIssue(name: String? = null): Long {
		val cv = ContentValues()
		cv.put(ISSUES_CREATED, now())
		name?.let {
			cv.put(ISSUES_NAME, name)
		}
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

	fun duplicateIssue(id: Long): Long {
		val copy = insertIssue(getIssueName(id))
		db.execSQL(
			"""INSERT INTO $ARGUMENTS (
				$ARGUMENTS_ISSUE,
				$ARGUMENTS_TEXT,
				$ARGUMENTS_WEIGHT,
				$ARGUMENTS_ORDER
			)
			SELECT
				?,
				$ARGUMENTS_TEXT,
				$ARGUMENTS_WEIGHT,
				$ARGUMENTS_ORDER
			FROM $ARGUMENTS
			WHERE $ARGUMENTS_ISSUE = ?""".trimMargin(),
			arrayOf("$copy", "$id")
		)
		return copy
	}

	fun getArguments(
		issueId: Long,
		sortedByWeight: Boolean = false
	): Cursor? = db.rawQuery(
		"""SELECT
			$ARGUMENTS_ID,
			$ARGUMENTS_TEXT,
			$ARGUMENTS_WEIGHT,
			$ARGUMENTS_ORDER
			FROM $ARGUMENTS
			WHERE $ARGUMENTS_ISSUE = ?
			ORDER BY ${
			if (sortedByWeight)
				"$ARGUMENTS_WEIGHT, $ARGUMENTS_ID" else
				"$ARGUMENTS_ORDER, $ARGUMENTS_ID"
		}""".trimMargin(),
		arrayOf("$issueId")
	)

	fun getArgument(id: Long): Argument? = db.rawQuery(
		"""SELECT
			$ARGUMENTS_TEXT,
			$ARGUMENTS_WEIGHT
			FROM $ARGUMENTS
			WHERE $ARGUMENTS_ID = ?""".trimMargin(),
		arrayOf("$id")
	)?.use {
		if (it.moveToFirst()) {
			Argument(
				it.getString(ARGUMENTS_TEXT),
				it.getInt(ARGUMENTS_WEIGHT)
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
				WHERE $ARGUMENTS_ISSUE = ?""".trimMargin(),
			arrayOf("$issueId")
		)
	}

	fun restoreArgumentInputOrder(issueId: Long) {
		// execSQL() instead of update() because we can't use column
		// names in values of ContentValues; another leaking abstraction
		db.execSQL(
			"""UPDATE $ARGUMENTS
				SET $ARGUMENTS_ORDER = $ARGUMENTS_ID
				WHERE $ARGUMENTS_ISSUE = ?""".trimMargin(),
			arrayOf("$issueId")
		)
	}

	fun moveArgument(issueId: Long, argumentId: Long, places: Int) {
		getArguments(issueId)?.use {
			if (!it.moveToFirst()) {
				return
			}
			val idColumn = it.getColumnIndex(ARGUMENTS_ID)
			if (idColumn < 0) {
				return
			}
			var currentPos = -1
			var i = 0
			do {
				if (it.getLong(idColumn) == argumentId) {
					currentPos = i
					break
				}
				++i
			} while (it.moveToNext())
			val newPos = currentPos + places
			if (currentPos < 0 ||
				min(it.count - 1, max(0, newPos)) == currentPos ||
				!it.moveToFirst()
			) {
				return
			}
			val shift = if (places < 0) 1 else -1
			val start = min(newPos, currentPos)
			val stop = max(newPos, currentPos)
			i = 0
			do {
				val id = it.getLong(idColumn)
				val cv = ContentValues()
				cv.put(
					ARGUMENTS_ORDER,
					if (i in start..stop) {
						if (id == argumentId) {
							newPos
						} else {
							i + shift
						}
					} else {
						i
					}
				)
				db.update(ARGUMENTS, cv, "$ARGUMENTS_ID = ?", arrayOf("$id"))
				++i
			} while (it.moveToNext())
		}
	}

	fun importDatabase(context: Context, fileName: String): String? {
		return db.importDatabase(context, fileName)
	}

	private class OpenHelper(context: Context) :
		SQLiteOpenHelper(context, FILE_NAME, null, 1) {
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
		const val FILE_NAME = "arguments.db"

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
			db.execSQL("DROP TABLE IF EXISTS $ISSUES".trimMargin())
			db.execSQL(
				"""CREATE TABLE $ISSUES (
					$ISSUES_ID INTEGER PRIMARY KEY AUTOINCREMENT,
					$ISSUES_NAME TEXT,
					$ISSUES_CREATED DATETIME)""".trimMargin()
			)
		}

		private fun createArguments(db: SQLiteDatabase) {
			db.execSQL("DROP TABLE IF EXISTS $ARGUMENTS".trimMargin())
			db.execSQL(
				"""CREATE TABLE $ARGUMENTS (
					$ARGUMENTS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
					$ARGUMENTS_ISSUE INTEGER,
					$ARGUMENTS_TEXT TEXT NOT NULL,
					$ARGUMENTS_WEIGHT INTEGER,
					$ARGUMENTS_ORDER INTEGER)""".trimMargin()
			)
		}

		private fun SQLiteDatabase.importDatabase(
			context: Context,
			fileName: String
		): String? {
			var edb: SQLiteDatabase? = null
			return try {
				edb = ImportHelper(
					ExternalDatabaseContext(context),
					fileName
				).readableDatabase
				beginTransaction()
				if (importIssuesFrom(edb)) {
					setTransactionSuccessful()
					null
				} else {
					context.getString(R.string.import_failed_unknown)
				}
			} catch (e: SQLException) {
				e.message
			} finally {
				if (inTransaction()) {
					endTransaction()
				}
				edb?.close()
			}
		}

		private fun SQLiteDatabase.importIssuesFrom(
			src: SQLiteDatabase
		): Boolean {
			val cursor = src.rawQuery(
				"""SELECT *
				FROM $ISSUES
				ORDER BY $ISSUES_ID""".trimIndent(),
				null
			) ?: return false
			val idIndex = cursor.getColumnIndex(ISSUES_ID)
			val nameIndex = cursor.getColumnIndex(ISSUES_NAME)
			val createdIndex = cursor.getColumnIndex(ISSUES_CREATED)
			var success = true
			if (cursor.moveToFirst()) {
				do {
					val srcId = cursor.getLong(idIndex)
					val name = cursor.getString(nameIndex)
					val created = cursor.getString(createdIndex)
					if (srcId < 1L || issueExists(name, created)) {
						continue
					}
					val destId = insert(
						ISSUES,
						null,
						ContentValues().apply {
							put(ISSUES_NAME, name)
							put(ISSUES_CREATED, created)
						}
					)
					if (destId < 1L ||
						!importArgumentsFrom(src, srcId, destId)
					) {
						success = false
						break
					}
				} while (cursor.moveToNext())
			}
			cursor.close()
			return success
		}

		private fun SQLiteDatabase.issueExists(
			name: String,
			created: String
		): Boolean {
			val cursor = rawQuery(
				"""SELECT $ISSUES_ID
					FROM $ISSUES
					WHERE $ISSUES_NAME = ?
						AND $ISSUES_CREATED = ?
					LIMIT 1""".trimMargin(),
				arrayOf(name, created)
			) ?: return false
			val exists = cursor.moveToFirst() && cursor.count > 0
			cursor.close()
			return exists
		}

		private fun SQLiteDatabase.importArgumentsFrom(
			src: SQLiteDatabase,
			srcId: Long,
			destId: Long
		): Boolean {
			val cursor = src.rawQuery(
				"""SELECT *
				FROM $ARGUMENTS
				WHERE $ARGUMENTS_ISSUE = ?
				ORDER BY $ARGUMENTS_ID""".trimIndent(),
				arrayOf(srcId.toString())
			) ?: return false
			val textIndex = cursor.getColumnIndex(ARGUMENTS_TEXT)
			val weightIndex = cursor.getColumnIndex(ARGUMENTS_WEIGHT)
			val orderIndex = cursor.getColumnIndex(ARGUMENTS_ORDER)
			var success = true
			if (cursor.moveToFirst()) {
				do {
					val text = cursor.getString(textIndex)
					val weight = cursor.getInt(weightIndex)
					val order = cursor.getInt(orderIndex)
					if (argumentExists(destId, text, weight, order)) {
						continue
					}
					if (insert(
							ARGUMENTS,
							null,
							ContentValues().apply {
								put(ARGUMENTS_ISSUE, destId)
								put(ARGUMENTS_TEXT, text)
								put(ARGUMENTS_WEIGHT, weight)
								put(ARGUMENTS_ORDER, order)
							}
						) < 1L
					) {
						success = false
						break
					}
				} while (cursor.moveToNext())
			}
			cursor.close()
			return success
		}

		private fun SQLiteDatabase.argumentExists(
			issueId: Long,
			text: String,
			weight: Int,
			order: Int
		): Boolean {
			val cursor = rawQuery(
				"""SELECT $ARGUMENTS_ID
					FROM $ARGUMENTS
					WHERE $ARGUMENTS_ISSUE = ?
						AND $ARGUMENTS_TEXT = ?
						AND $ARGUMENTS_WEIGHT = ?
						AND $ARGUMENTS_ORDER = ?
					LIMIT 1""".trimMargin(),
				arrayOf(
					issueId.toString(),
					text,
					weight.toString(),
					order.toString(),
				)
			) ?: return false
			val exists = cursor.moveToFirst() && cursor.count > 0
			cursor.close()
			return exists
		}
	}
}

private fun now(): String = SimpleDateFormat(
	"yyyy-MM-dd HH:mm:ss",
	Locale.US
).format(Date())

private class ImportHelper constructor(context: Context, path: String) :
	SQLiteOpenHelper(context, path, null, 1) {
	override fun onCreate(db: SQLiteDatabase) {
		// Do nothing.
	}

	override fun onDowngrade(
		db: SQLiteDatabase,
		oldVersion: Int,
		newVersion: Int
	) {
		// Do nothing, but without that method we cannot open
		// different versions.
	}

	override fun onUpgrade(
		db: SQLiteDatabase,
		oldVersion: Int,
		newVersion: Int
	) {
		// Do nothing, but without that method we cannot open
		// different versions.
	}
}

// Somehow it's required to use this ContextWrapper to access the
// tables in an external database. Without this, the database will
// only contain the table "android_metadata".
private class ExternalDatabaseContext(base: Context?) : ContextWrapper(base) {
	override fun getDatabasePath(name: String) = File(filesDir, name)

	override fun openOrCreateDatabase(
		name: String,
		mode: Int,
		factory: SQLiteDatabase.CursorFactory,
		errorHandler: DatabaseErrorHandler?
	): SQLiteDatabase = openOrCreateDatabase(name, mode, factory)

	override fun openOrCreateDatabase(
		name: String,
		mode: Int,
		factory: SQLiteDatabase.CursorFactory
	): SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(
		getDatabasePath(name), null
	)
}
