package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.MriRepository
import com.example.data.UserDataRepository
import com.example.data.local.MriDatabase
import com.example.data.local.NodeProgressEntity
import com.example.util.NodeStatus
import com.example.util.PathStateResolver
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SkipTestAndProgressSourceTest {
    private lateinit var context: Context
    private lateinit var db: MriDatabase
    private lateinit var repository: UserDataRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MriDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UserDataRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun skippedNodesResolveAsSkippedAndUpgradeToReadOnReadCompletion() = runBlocking {
        val m01 = MriRepository.modules.first { it.id == "M01" }
        val preReadCode = m01.nodes[0].code
        val skippedSlice = listOf(m01.nodes[1].code, m01.nodes[2].code)

        repository.markNodeRead(preReadCode)
        val insertedCount = repository.markNodesSkippedByTest(listOf(preReadCode) + skippedSlice)
        // preReadCode yang sudah READ tidak ditimpa menjadi SKIP_TEST
        assertEquals(skippedSlice.size, insertedCount)

        val rows = db.nodeProgressDao().getAll().associateBy { it.nodeCode }
        assertEquals("READ", rows[preReadCode]?.source)
        assertEquals("SKIP_TEST", rows[skippedSlice[0]]?.source)
        assertEquals("SKIP_TEST", rows[skippedSlice[1]]?.source)

        val skipSet = db.nodeProgressDao().getNodeCodesBySource("SKIP_TEST").toSet()
        val pathBeforeUpgrade = PathStateResolver.resolveModule(
            module = m01,
            allModules = MriRepository.modules,
            completedCodes = rows.keys,
            strictLocks = true,
            skippedCodes = skipSet
        )
        assertEquals(NodeStatus.DONE, pathBeforeUpgrade.nodes[0].status)
        assertEquals(NodeStatus.SKIPPED, pathBeforeUpgrade.nodes[1].status)
        assertEquals(NodeStatus.SKIPPED, pathBeforeUpgrade.nodes[2].status)
        assertEquals(NodeStatus.CURRENT, pathBeforeUpgrade.nodes[3].status)

        // Baca dan selesaikan simpul yang tadinya SKIP_TEST -> berubah menjadi READ & DONE
        val upgraded = repository.markNodeRead(skippedSlice[0])
        assertTrue(upgraded)
        assertFalse(repository.markNodeRead(skippedSlice[0]))

        val updatedRow = db.nodeProgressDao().getByNodeCode(skippedSlice[0])
        assertEquals("READ", updatedRow?.source)

        val updatedSkipSet = db.nodeProgressDao().getNodeCodesBySource("SKIP_TEST").toSet()
        val pathAfterUpgrade = PathStateResolver.resolveModule(
            module = m01,
            allModules = MriRepository.modules,
            completedCodes = db.nodeProgressDao().getAll().map { it.nodeCode }.toSet(),
            strictLocks = true,
            skippedCodes = updatedSkipSet
        )
        assertEquals(NodeStatus.DONE, pathAfterUpgrade.nodes[1].status)
        assertEquals(NodeStatus.SKIPPED, pathAfterUpgrade.nodes[2].status)
    }
}
