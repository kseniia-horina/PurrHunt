package app.purrhunt.domain

/** Where a creature lives and moves. Determines spawn area and which hiding spots it may use. */
enum class Medium { GROUND, AIR, WATER, WATER_SURFACE }

/** Body plan shared by several species: one behaviour and one renderer per family. */
enum class Family { RODENT, BIRD, BUTTERFLY, FLY, CRAWLER, CRICKET, FISH, TADPOLE, STRIDER, FIREFLY }

enum class Species(
    val family: Family,
    val medium: Medium,
    /** Body length in dp. */
    val sizeDp: Float,
    val rare: Boolean = false,
    val nightOnly: Boolean = false,
) {
    GRAY_MOUSE(Family.RODENT, Medium.GROUND, 64f),
    MOUSELET(Family.RODENT, Medium.GROUND, 44f),
    RAT(Family.RODENT, Medium.GROUND, 96f),
    WHITE_MOUSE(Family.RODENT, Medium.GROUND, 62f, rare = true),

    SPARROW(Family.BIRD, Medium.GROUND, 78f),
    TIT(Family.BIRD, Medium.GROUND, 70f),
    BULLFINCH(Family.BIRD, Medium.GROUND, 82f),

    BUTTERFLY(Family.BUTTERFLY, Medium.AIR, 64f),
    SWALLOWTAIL(Family.BUTTERFLY, Medium.AIR, 84f, rare = true),
    FLY(Family.FLY, Medium.AIR, 30f),
    LADYBUG(Family.CRAWLER, Medium.GROUND, 32f),
    BEETLE(Family.CRAWLER, Medium.GROUND, 46f),
    CRICKET(Family.CRICKET, Medium.GROUND, 44f),

    CRUCIAN(Family.FISH, Medium.WATER, 100f),
    GOLDFISH(Family.FISH, Medium.WATER, 88f, rare = true),
    TADPOLE(Family.TADPOLE, Medium.WATER, 40f),
    WATER_STRIDER(Family.STRIDER, Medium.WATER_SURFACE, 48f),

    FIREFLY(Family.FIREFLY, Medium.AIR, 26f, nightOnly = true),
    GIANT_FIREFLY(Family.FIREFLY, Medium.AIR, 44f, rare = true, nightOnly = true);

    companion object {
        val bestiary: List<Species> = entries
        fun byName(name: String): Species? = entries.firstOrNull { it.name == name }
    }
}
