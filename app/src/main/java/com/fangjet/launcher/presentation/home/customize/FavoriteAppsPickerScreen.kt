package com.fangjet.launcher.presentation.home.customize

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.launcher.R
import com.fangjet.launcher.presentation.common.BigBackButton

/**
 * Elderly-friendly checkbox list for hand-picking the My Apps row: big rows,
 * big switches, real app icons. Changes save instantly — no Save button to
 * forget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteAppsPickerScreen(
    onBack: () -> Unit,
    viewModel: FavoriteAppsPickerViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.favorite_picker_title,
                            selectedCount,
                            viewModel.maxApps,
                        ),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
        bottomBar = { BigBackButton(onClick = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            items(items, key = { it.app.packageName }) { item ->
                PickerRow(
                    item = item,
                    onToggle = { viewModel.onToggle(item.app.packageName) },
                )
            }
        }
    }
}

@Composable
private fun PickerRow(
    item: PickerItem,
    onToggle: () -> Unit,
) {
    val bitmap: ImageBitmap? = remember(item.app.packageName) {
        item.app.icon?.let { drawable ->
            runCatching { drawable.toBitmap(144, 144).asImageBitmap() }.getOrNull()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = item.app.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Switch(
            checked = item.isSelected,
            onCheckedChange = { onToggle() },
            modifier = Modifier.scale(1.2f),
        )
    }
}
