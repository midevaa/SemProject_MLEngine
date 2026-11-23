package io.github.ephym6.plantid.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.ephym6.plantid.R
import kotlinx.serialization.Serializable

// Type-safe navigation routes. Destinations that need arguments later (e.g. a
// history record id) become `@Serializable data class`es with those fields.

@Serializable
data object HomeRoute

@Serializable
data object HistoryRoute

@Serializable
data object AboutRoute

/** Preview of the selected photo. [photoId] is a file name from PhotoStore. */
@Serializable
data class PhotoPreviewRoute(val photoId: String)

/** Identifying, then the result, for the photo [photoId]. */
@Serializable
data class IdentificationRoute(val photoId: String)

/** Care guide for the catalog plant [plantId]. */
@Serializable
data class CareGuideRoute(val plantId: String)

/** A saved history record. */
@Serializable
data class HistoryDetailRoute(val recordId: String)

/** Destinations shown in the bottom navigation bar, in display order. */
enum class TopLevelDestination(
    val route: Any,
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
) {
    Home(HomeRoute, R.drawable.ic_home, R.string.nav_home),
    History(HistoryRoute, R.drawable.ic_history, R.string.nav_history),
    About(AboutRoute, R.drawable.ic_info, R.string.nav_about),
}
