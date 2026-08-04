package com.fangjet.launcher.presentation.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.fangjet.launcher.R
import com.fangjet.launcher.domain.model.AppInfo

/**
 * The Apps Row: horizontally scrollable real device apps with their real
 * icons — Spotify, Audible, whatever the user actually opens. Populated by
 * usage ranking or an explicit selection (Settings → Apps Row).
 *
 * Tap launches; long-press opens the system App Info page. [badgedPackages]
 * members get a Pixel-style dot: a notification is waiting.
 */
@Composable
fun FavoriteAppsRow(
    apps: List<AppInfo>,
    badgedPackages: Set<String>,
    onAppTapped: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(apps, key = { it.packageName }) { app ->
            FavoriteAppItem(
                app = app,
                showBadge = app.packageName in badgedPackages,
                onTap = { onAppTapped(app.packageName) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAppItem(
    app: AppInfo,
    showBadge: Boolean,
    onTap: () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    // Render larger than display size so icons stay sharp on xxhdpi+ screens.
    val bitmap: ImageBitmap? = remember(app.packageName) {
        app.icon?.let { drawable ->
            runCatching { drawable.toBitmap(256, 256).asImageBitmap() }.getOrNull()
        }
    }

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(90.dp)
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = onTap,
                    onLongClick = { menuOpen = true },
                ).padding(vertical = 6.dp),
        ) {
            Box {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.label,
                        modifier = Modifier.size(62.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
                if (showBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(17.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935)),
                    )
                }
            }
            Text(
                text = app.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.apps_row_app_info), fontSize = 18.sp) },
                onClick = {
                    menuOpen = false
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", app.packageName, null),
                        ),
                    )
                },
            )
        }
    }
}
