package app.purrhunt.domain

data class CatProfile(
    val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val photoPath: String? = null,
    val createdAt: Long = 0,
) {
    companion object {
        /** Coat colours offered for a profile avatar. */
        val COLORS = listOf(
            0xFFE0A060.toInt(), // ginger
            0xFF8A8A8A.toInt(), // grey
            0xFF2E2B2A.toInt(), // black
            0xFFF2EEE6.toInt(), // white
            0xFFB89A7A.toInt(), // fawn
            0xFF6E5A4C.toInt(), // tabby brown
            0xFF9FB0C4.toInt(), // blue
            0xFFD9C3A0.toInt(), // cream
        )
    }
}
