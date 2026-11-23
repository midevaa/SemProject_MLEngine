package io.github.ephym6.plantid.data.history

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * History saved before the confidence column existed (schema 1) must survive the
 * upgrade to schema 2 unchanged, with no confidence. Uses the exported schemas
 * in app/schemas/.
 */
@RunWith(AndroidJUnit4::class)
class HistoryMigrationTest {

    private val databaseName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PlantIdDatabase::class.java)

    @Test
    fun schema1RecordsSurviveTheUpgradeWithoutConfidence() {
        helper.createDatabase(databaseName, 1).use { db ->
            db.execSQL(
                """INSERT INTO history (id, identified_at, status, class_id, plant_id, common_name,
                   scientific_name, model_name, model_version, is_demo, photo_file)
                   VALUES ('old-1', 1700000000000, 'Identified', 'golden_pothos', 'golden_pothos',
                   'Golden pothos', 'Epipremnum aureum', 'PlantID demo identifier', NULL, 1, 'old-1.jpg')""",
            )
        }

        helper.runMigrationsAndValidate(databaseName, 2, true).use { db ->
            db.query("SELECT id, common_name, is_demo, photo_file, confidence FROM history").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("old-1", cursor.getString(0))
                assertEquals("Golden pothos", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
                assertEquals("old-1.jpg", cursor.getString(3))
                assertTrue("old records have no confidence", cursor.isNull(4))
            }
        }
    }
}
