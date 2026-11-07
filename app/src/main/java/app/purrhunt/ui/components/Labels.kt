package app.purrhunt.ui.components

import androidx.annotation.StringRes
import app.purrhunt.R
import app.purrhunt.domain.Achievement
import app.purrhunt.domain.GameMode
import app.purrhunt.domain.Location
import app.purrhunt.domain.NightMode
import app.purrhunt.domain.Rank
import app.purrhunt.domain.Species

@get:StringRes
val Species.nameRes: Int
    get() = when (this) {
        Species.GRAY_MOUSE -> R.string.species_gray_mouse
        Species.MOUSELET -> R.string.species_mouselet
        Species.RAT -> R.string.species_rat
        Species.WHITE_MOUSE -> R.string.species_white_mouse
        Species.SPARROW -> R.string.species_sparrow
        Species.TIT -> R.string.species_tit
        Species.BULLFINCH -> R.string.species_bullfinch
        Species.BUTTERFLY -> R.string.species_butterfly
        Species.SWALLOWTAIL -> R.string.species_swallowtail
        Species.FLY -> R.string.species_fly
        Species.LADYBUG -> R.string.species_ladybug
        Species.BEETLE -> R.string.species_beetle
        Species.CRICKET -> R.string.species_cricket
        Species.CRUCIAN -> R.string.species_crucian
        Species.GOLDFISH -> R.string.species_goldfish
        Species.TADPOLE -> R.string.species_tadpole
        Species.WATER_STRIDER -> R.string.species_water_strider
        Species.FIREFLY -> R.string.species_firefly
        Species.GIANT_FIREFLY -> R.string.species_giant_firefly
    }

@get:StringRes
val Species.factRes: Int
    get() = when (this) {
        Species.GRAY_MOUSE -> R.string.fact_gray_mouse
        Species.MOUSELET -> R.string.fact_mouselet
        Species.RAT -> R.string.fact_rat
        Species.WHITE_MOUSE -> R.string.fact_white_mouse
        Species.SPARROW -> R.string.fact_sparrow
        Species.TIT -> R.string.fact_tit
        Species.BULLFINCH -> R.string.fact_bullfinch
        Species.BUTTERFLY -> R.string.fact_butterfly
        Species.SWALLOWTAIL -> R.string.fact_swallowtail
        Species.FLY -> R.string.fact_fly
        Species.LADYBUG -> R.string.fact_ladybug
        Species.BEETLE -> R.string.fact_beetle
        Species.CRICKET -> R.string.fact_cricket
        Species.CRUCIAN -> R.string.fact_crucian
        Species.GOLDFISH -> R.string.fact_goldfish
        Species.TADPOLE -> R.string.fact_tadpole
        Species.WATER_STRIDER -> R.string.fact_water_strider
        Species.FIREFLY -> R.string.fact_firefly
        Species.GIANT_FIREFLY -> R.string.fact_giant_firefly
    }

@StringRes
fun locationNameRes(location: Location, night: Boolean): Int = when {
    night -> R.string.location_night_meadow
    location == Location.BASEMENT -> R.string.location_basement
    location == Location.GARDEN -> R.string.location_garden
    location == Location.MEADOW -> R.string.location_meadow
    location == Location.POND -> R.string.location_pond
    else -> R.string.location_mix
}

@StringRes
fun locationDescRes(location: Location, night: Boolean): Int = when {
    night -> R.string.location_desc_night_meadow
    location == Location.BASEMENT -> R.string.location_desc_basement
    location == Location.GARDEN -> R.string.location_desc_garden
    location == Location.MEADOW -> R.string.location_desc_meadow
    location == Location.POND -> R.string.location_desc_pond
    else -> R.string.location_desc_mix
}

@get:StringRes
val Rank.nameRes: Int
    get() = when (this) {
        Rank.KITTEN -> R.string.rank_kitten
        Rank.MOUSER -> R.string.rank_mouser
        Rank.YARD_TERROR -> R.string.rank_yard_terror
        Rank.HUNTER -> R.string.rank_hunter
        Rank.BASEMENT_LEGEND -> R.string.rank_basement_legend
    }

@get:StringRes
val GameMode.nameRes: Int
    get() = when (this) {
        GameMode.HUNT -> R.string.mode_hunt
        GameMode.FEATHER -> R.string.mode_feather
        GameMode.LASER -> R.string.mode_laser
    }

@get:StringRes
val GameMode.descRes: Int
    get() = when (this) {
        GameMode.HUNT -> R.string.mode_hunt_desc
        GameMode.FEATHER -> R.string.mode_feather_desc
        GameMode.LASER -> R.string.mode_laser_desc
    }

@get:StringRes
val NightMode.nameRes: Int
    get() = when (this) {
        NightMode.AUTO -> R.string.night_auto
        NightMode.ALWAYS -> R.string.night_always
        NightMode.NEVER -> R.string.night_never
    }

@get:StringRes
val Achievement.titleRes: Int
    get() = when (this) {
        Achievement.FIRST_CATCH -> R.string.ach_first_catch
        Achievement.TEN_CATCHES -> R.string.ach_ten_catches
        Achievement.HUNDRED_CATCHES -> R.string.ach_hundred_catches
        Achievement.THOUSAND_CATCHES -> R.string.ach_thousand_catches
        Achievement.HUNDRED_MICE -> R.string.ach_hundred_mice
        Achievement.RAT_CATCHER -> R.string.ach_rat_catcher
        Achievement.BIRD_WATCHER -> R.string.ach_bird_watcher
        Achievement.BUG_HUNTER -> R.string.ach_bug_hunter
        Achievement.GARDEN_COMPLETE -> R.string.ach_garden_complete
        Achievement.MEADOW_COMPLETE -> R.string.ach_meadow_complete
        Achievement.POND_COMPLETE -> R.string.ach_pond_complete
        Achievement.PERFECT_TEN -> R.string.ach_perfect_ten
        Achievement.SESSION_THIRTY -> R.string.ach_session_thirty
        Achievement.NIGHT_HUNTER -> R.string.ach_night_hunter
        Achievement.RARE_FIRST -> R.string.ach_rare_first
        Achievement.RARE_COLLECTOR -> R.string.ach_rare_collector
        Achievement.WEEK_STREAK -> R.string.ach_week_streak
        Achievement.FEATHER_FAN -> R.string.ach_feather_fan
        Achievement.LASER_CHASER -> R.string.ach_laser_chaser
        Achievement.EXPLORER -> R.string.ach_explorer
        Achievement.MARATHON -> R.string.ach_marathon
        Achievement.BESTIARY_COMPLETE -> R.string.ach_bestiary_complete
    }

@get:StringRes
val Achievement.descRes: Int
    get() = when (this) {
        Achievement.FIRST_CATCH -> R.string.ach_first_catch_desc
        Achievement.TEN_CATCHES -> R.string.ach_ten_catches_desc
        Achievement.HUNDRED_CATCHES -> R.string.ach_hundred_catches_desc
        Achievement.THOUSAND_CATCHES -> R.string.ach_thousand_catches_desc
        Achievement.HUNDRED_MICE -> R.string.ach_hundred_mice_desc
        Achievement.RAT_CATCHER -> R.string.ach_rat_catcher_desc
        Achievement.BIRD_WATCHER -> R.string.ach_bird_watcher_desc
        Achievement.BUG_HUNTER -> R.string.ach_bug_hunter_desc
        Achievement.GARDEN_COMPLETE -> R.string.ach_garden_complete_desc
        Achievement.MEADOW_COMPLETE -> R.string.ach_meadow_complete_desc
        Achievement.POND_COMPLETE -> R.string.ach_pond_complete_desc
        Achievement.PERFECT_TEN -> R.string.ach_perfect_ten_desc
        Achievement.SESSION_THIRTY -> R.string.ach_session_thirty_desc
        Achievement.NIGHT_HUNTER -> R.string.ach_night_hunter_desc
        Achievement.RARE_FIRST -> R.string.ach_rare_first_desc
        Achievement.RARE_COLLECTOR -> R.string.ach_rare_collector_desc
        Achievement.WEEK_STREAK -> R.string.ach_week_streak_desc
        Achievement.FEATHER_FAN -> R.string.ach_feather_fan_desc
        Achievement.LASER_CHASER -> R.string.ach_laser_chaser_desc
        Achievement.EXPLORER -> R.string.ach_explorer_desc
        Achievement.MARATHON -> R.string.ach_marathon_desc
        Achievement.BESTIARY_COMPLETE -> R.string.ach_bestiary_complete_desc
    }

@StringRes
fun dayRes(isoDay: Int): Int = when (isoDay) {
    1 -> R.string.day_1
    2 -> R.string.day_2
    3 -> R.string.day_3
    4 -> R.string.day_4
    5 -> R.string.day_5
    6 -> R.string.day_6
    else -> R.string.day_7
}
