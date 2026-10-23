package app.uamo.ynotes.data

enum class SortOrder(val label: String) {
    DATE_MODIFIED_DESC("Más recientes"),
    DATE_MODIFIED_ASC("Más antiguas"),
    DATE_CREATED_DESC("Creadas: recientes"),
    DATE_CREATED_ASC("Creadas: antiguas"),
    NAME_ASC("Nombre A - Z"),
    NAME_DESC("Nombre Z - A"),
    SIZE_DESC("Más largas"),
    SIZE_ASC("Más cortas"),
}

fun List<NoteEntity>.applySortOrder(order: SortOrder): List<NoteEntity> = when (order) {
    SortOrder.DATE_MODIFIED_DESC -> sortedByDescending { it.updatedAt }
    SortOrder.DATE_MODIFIED_ASC  -> sortedBy { it.updatedAt }
    SortOrder.DATE_CREATED_DESC  -> sortedByDescending { it.createdAt }
    SortOrder.DATE_CREATED_ASC   -> sortedBy { it.createdAt }
    SortOrder.NAME_ASC           -> sortedBy { it.title.lowercase() }
    SortOrder.NAME_DESC          -> sortedByDescending { it.title.lowercase() }
    SortOrder.SIZE_DESC          -> sortedByDescending { it.title.length + it.body.length }
    SortOrder.SIZE_ASC           -> sortedBy { it.title.length + it.body.length }
}
