package fe.linksheet.composable.settings.browser

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import fe.linksheet.R
import fe.linksheet.composable.settings.SettingsScaffold
import fe.linksheet.composable.util.ColoredIcon
import fe.linksheet.composable.util.SettingsItemRow
import fe.linksheet.inAppBrowserSettingsRoute
import fe.linksheet.preferredBrowserSettingsRoute


@Composable
fun BrowserSettingsRoute(
    navController: NavHostController,
    onBackPressed: () -> Unit,
) {
    SettingsScaffold(R.string.browser, onBackPressed = onBackPressed) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxHeight(), contentPadding = PaddingValues(horizontal = 5.dp)
        ) {
            item(key = preferredBrowserSettingsRoute) {
                SettingsItemRow(
                    navController = navController,
                    navigateTo = preferredBrowserSettingsRoute,
                    headline = R.string.preferred_browser,
                    subtitle = R.string.preferred_browser_explainer,
                    image = {
                        ColoredIcon(
                            icon = Icons.Default.OpenInBrowser,
                            description = R.string.preferred_browser
                        )
                    }
                )
            }

            item(key = inAppBrowserSettingsRoute) {
                SettingsItemRow(
                    navController = navController,
                    navigateTo = inAppBrowserSettingsRoute,
                    headline = R.string.in_app_browser,
                    subtitle = R.string.in_app_browser_explainer,
                    image = {
                        ColoredIcon(
                            icon = Icons.Default.ExitToApp,
                            description = R.string.in_app_browser
                        )
                    }
                )
            }
        }
    }
}