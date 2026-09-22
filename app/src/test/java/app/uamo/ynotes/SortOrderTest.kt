package app.uamo.ynotes

import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.data.SortOrder
import app.uamo.ynotes.data.applySortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class SortOrderTest {

    private val noteA = NoteEntity(id = "1", title = "Alpha", body = "Short", createdAt = 100L, updatedAt = 300L)
    private val noteB = NoteEntity(id = "2", title = "Beta", body = "Longer content", createdAt = 200L, updatedAt = 100L)
    private val noteC = NoteEntity(id = "3", title = "Gamma", body = "Longest content text body", createdAt = 300L, updatedAt = 200L)

    private val notes = listOf(noteA, noteB, noteC)

    @Test
    fun testSortByName() {
        val asc = notes.applySortOrder(SortOrder.NAME_ASC)
        assertEquals(listOf("Alpha", "Beta", "Gamma"), asc.map { it.title })

        val desc = notes.applySortOrder(SortOrder.NAME_DESC)
        assertEquals(listOf("Gamma", "Beta", "Alpha"), desc.map { it.title })
    }

    @Test
    fun testSortByDateModified() {
        val desc = notes.applySortOrder(SortOrder.DATE_MODIFIED_DESC)
        assertEquals(listOf(noteA, noteC, noteB), desc)

        val asc = notes.applySortOrder(SortOrder.DATE_MODIFIED_ASC)
        assertEquals(listOf(noteB, noteC, noteA), asc)
    }

    @Test
    fun testSortBySize() {
        val desc = notes.applySortOrder(SortOrder.SIZE_DESC)
        assertEquals(listOf(noteC, noteB, noteA), desc)

        val asc = notes.applySortOrder(SortOrder.SIZE_ASC)
        assertEquals(listOf(noteA, noteB, noteC), asc)
    }
}
