package com.sniperoftdm.booktracker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object LibraryRoute

@Serializable
object StatsRoute

@Serializable
object SettingsRoute

@Serializable
data class BookDetailRoute(val bookId: Long)

@Serializable
object AddBookRoute

@Serializable
data class EditBookRoute(val bookId: Long)

@Serializable
object DebugRoute

@Serializable
object ClayGalleryRoute

@Serializable
object SearchOnlineRoute
