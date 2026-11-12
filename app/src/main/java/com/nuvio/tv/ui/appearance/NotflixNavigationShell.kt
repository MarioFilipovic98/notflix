package com.nuvio.tv.ui.appearance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.nuvio.tv.R
import com.nuvio.tv.core.ui.rememberNotflixConfirmSound
import com.nuvio.tv.ui.navigation.Screen
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage

data class NotflixDestination(val route: String, val title: String)

private const val PROFILE_ROUTE = "profile"

@Composable
fun rememberNotflixNavigationShell(
    navigation: NotflixNavigationState,
    currentRoute: String?,
    selectedRoute: String?,
    rootRoutes: Set<String>,
    homeRoute: String,
    destinations: List<NotflixDestination>,
    contentFocusRequester: FocusRequester,
    profileName: String?,
    onSwitchProfile: () -> Unit,
    onNavigate: (String) -> Unit,
    onExit: () -> Unit,
    longPressBackHeld: MutableState<Boolean>,
    profileAvatarUrl: String? = null,
    canSwitchProfile: Boolean = true
): PresentationShell {
    val root = currentRoute in rootRoutes
    val isHome = currentRoute == homeRoute
    val showProfileSwitch = profileName != null && canSwitchProfile
    val orderedDestinations = remember(destinations) {
        destinations.sortedBy { it.route != Screen.Search.route }
    }
    SideEffect {
        navigation.selectedRoute = selectedRoute
        navigation.topOrder = (if (showProfileSwitch) listOf(PROFILE_ROUTE) else emptyList()) +
            orderedDestinations.map { it.route }
    }
    BackHandler(root) {
        if (longPressBackHeld.value) return@BackHandler
        if (navigation.topFocused) onExit() else navigation.focusTop()
    }
    return PresentationShell(
        rootModifier = Modifier.background(NotflixVisualSpec.Background).onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) navigation.interacted = true
            if (event.key == Key.Back && root && event.nativeKeyEvent.isLongPress && event.type == KeyEventType.KeyDown) {
                longPressBackHeld.value = true
                navigation.focusTop()
                true
            } else if (event.key == Key.Back && longPressBackHeld.value) {
                if (event.type == KeyEventType.KeyUp) longPressBackHeld.value = false
                true
            } else false
        },
        contentModifier = (if (root && currentRoute != homeRoute) Modifier.padding(top = NotflixVisualSpec.NAV_HEIGHT.dp) else Modifier)
            .onFocusChanged { if (it.hasFocus) navigation.topFocused = false }
            // M29: only Home registered the content target, so Down from the bar was lost on Search/Library/Settings.
            // Other root screens: the content group is the target and returns to its last focused element.
            .then(if (root && currentRoute != homeRoute) Modifier.focusRequester(contentFocusRequester).focusRestorer() else Modifier)
            .focusGroup(),
        chrome = {
            if (root && !navigation.immersive) {
                Row(
                    Modifier.fillMaxWidth().height(NotflixVisualSpec.NAV_HEIGHT.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF15171A),
                                    Color.Black.copy(alpha = if (isHome && navigation.homeAtTop) .88f else 1f)
                                )
                            )
                        )
                        .padding(horizontal = NotflixVisualSpec.GUTTER.dp)
                        .testTag("notflix-navigation"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(56.dp), contentAlignment = Alignment.CenterStart) {
                        if (profileName != null) {
                            if (showProfileSwitch) {
                                TopDestination(
                                    PROFILE_ROUTE, profileName, false, navigation,
                                    contentFocusRequester, isHome, onSwitchProfile
                                ) {
                                    TopProfileAvatar(profileAvatarUrl, showCaret = true)
                                }
                            } else {
                                TopProfileAvatar(profileAvatarUrl, showCaret = false)
                            }
                        }
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            orderedDestinations.forEach { item ->
                                key(item.route) {
                                    TopDestination(
                                        item.route, item.title, item.route == selectedRoute, navigation,
                                        contentFocusRequester, isHome, { onNavigate(item.route) }
                                    ) {
                                        if (item.route == Screen.Search.route) {
                                            Icon(Icons.Default.Search, null, Modifier.size(24.dp), tint = NotflixVisualSpec.Text)
                                        } else {
                                            Text(
                                                item.title, color = NotflixVisualSpec.Text,
                                                fontFamily = NotflixVisualSpec.BodyFont, fontSize = 15.sp,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box(Modifier.width(56.dp), contentAlignment = Alignment.CenterEnd) {
                        Image(
                            painterResource(R.drawable.notflix_top_mark), null,
                            Modifier.size(width = 18.dp, height = 30.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun TopDestination(
    route: String, title: String, active: Boolean, navigation: NotflixNavigationState,
    contentFocusRequester: FocusRequester, isHome: Boolean, onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val requester = remember { FocusRequester() }
    val confirmSound = rememberNotflixConfirmSound()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var focused by remember { mutableStateOf(false) }
    DisposableEffect(route, requester) {
        navigation.topRequesters[route] = requester
        onDispose { navigation.topRequesters.remove(route) }
    }
    val profile = route == PROFILE_ROUTE
    val iconOnly = profile || route == Screen.Search.route
    Box(
        Modifier.widthIn(min = 48.dp, max = if (profile) 56.dp else 144.dp)
            .height(48.dp).focusRequester(requester).onFocusChanged {
            focused = it.isFocused
            if (it.isFocused) {
                if (navigation.topFocused || navigation.homeNeedsFocusRepair?.invoke() != true) {
                    navigation.topFocused = true
                    navigation.homeCancelFocus?.invoke()
                }
            }
        }.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                navigation.topFocused = false
                if (isHome) navigation.homeDown?.invoke() else runCatching { contentFocusRequester.requestFocus() }
                true
            } else if (event.type == KeyEventType.KeyDown && event.key in setOf(Key.DirectionLeft, Key.DirectionRight)) {
                navigation.topFocused = true
                navigation.homeCancelFocus?.invoke()
                val index = navigation.topOrder.indexOf(route)
                val direction = if ((event.key == Key.DirectionRight) != rtl) 1 else -1
                navigation.topOrder.getOrNull(index + direction)?.let { navigation.topRequesters[it]?.requestFocus() }
                true
            } else false
        }.clip(CircleShape)
            .clickable(role = if (profile) Role.Button else Role.Tab, onClick = {
                confirmSound()
                navigation.topFocused = true
                navigation.homeCancelFocus?.invoke()
                onClick()
            }).semantics {
                if (!profile) selected = active
                if (iconOnly) contentDescription = title
            }.testTag("notflix-destination-$route"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 40.dp)
                .background(
                    when {
                        focused -> Color(0xFF565659)
                        active -> Color(0xFF3B3B3D)
                        else -> Color.Transparent
                    }, CircleShape
                )
                .then(if (focused) Modifier.border(2.dp, NotflixVisualSpec.Text, CircleShape) else Modifier)
                .padding(horizontal = if (iconOnly) 4.dp else 14.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun TopProfileAvatar(url: String?, showCaret: Boolean) {
    val fallback = painterResource(R.drawable.notflix_top_avatar)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val avatarModifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
        if (url == null) {
            Image(fallback, null, avatarModifier)
        } else {
            AsyncImage(
                model = url, contentDescription = null, modifier = avatarModifier,
                placeholder = fallback, error = fallback, contentScale = ContentScale.Crop
            )
        }
        if (showCaret) {
            Icon(Icons.Default.ArrowDropDown, null, Modifier.size(14.dp), tint = NotflixVisualSpec.Text)
        }
    }
}
