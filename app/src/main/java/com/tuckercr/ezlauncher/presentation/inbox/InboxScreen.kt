package com.tuckercr.ezlauncher.presentation.inbox

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.domain.model.CallType
import com.tuckercr.ezlauncher.domain.model.InboxItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(viewModel: InboxViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbox", fontWeight = FontWeight.Bold) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val s = state) {
                is InboxUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is InboxUiState.Error -> {
                    Text(
                        text = s.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                is InboxUiState.Empty -> {
                    Text(
                        text = "No recent calls or messages",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                    )
                }

                is InboxUiState.Success -> {
                    LazyColumn {
                        items(s.items, key = { "${it.javaClass.simpleName}_${it.id}" }) { item ->
                            when (item) {
                                is InboxItem.Call -> CallRow(
                                    call = item,
                                    onCallBack = { viewModel.onCallTapped(item) },
                                )

                                is InboxItem.Message -> MessageRow(
                                    message = item,
                                    onOpen = { viewModel.onMessageTapped(item) },
                                    onCall = { viewModel.onCallFromMessage(item) },
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Call row ──────────────────────────────────────────────────────────────────

@Composable
private fun CallRow(
    call: InboxItem.Call,
    onCallBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCallBack)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InboxAvatar(photoUri = call.photoUri, initials = call.initials)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (call.isMissed) Color(0xFFEF9A9A) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(callTypeIcon(call.type)),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = callTypeColor(call.type),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = callTypeLabel(call.type),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = relativeTime(call.timestamp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = onCallBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_call),
                contentDescription = "Call back",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun callTypeIcon(type: CallType): Int =
    when (type) {
        CallType.INCOMING -> R.drawable.ic_call
        CallType.OUTGOING -> R.drawable.ic_call
        CallType.MISSED, CallType.REJECTED -> R.drawable.ic_call
        else -> R.drawable.ic_call
    }

private fun callTypeColor(type: CallType): Color =
    when (type) {
        CallType.MISSED, CallType.REJECTED -> Color(0xFFEF9A9A)
        CallType.INCOMING -> Color(0xFFA5D6A7)
        else -> Color(0xFFAAAAAA)
    }

private fun callTypeLabel(type: CallType): String =
    when (type) {
        CallType.INCOMING -> "Incoming"
        CallType.OUTGOING -> "Outgoing"
        CallType.MISSED -> "Missed"
        CallType.REJECTED -> "Rejected"
        CallType.BLOCKED -> "Blocked"
        CallType.UNKNOWN -> "Unknown"
    }

// ── Message row ───────────────────────────────────────────────────────────────

@Composable
private fun MessageRow(
    message: InboxItem.Message,
    onOpen: () -> Unit,
    onCall: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InboxAvatar(photoUri = message.photoUri, initials = message.initials)

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message.displayName,
                    fontSize = 18.sp,
                    fontWeight = if (!message.isRead) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = relativeTime(message.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = message.snippet,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Button(
            onClick = onCall,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_call),
                contentDescription = "Call",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Shared avatar ─────────────────────────────────────────────────────────────

@Composable
private fun InboxAvatar(
    photoUri: Uri?,
    initials: String,
) {
    if (photoUri != null) {
        AsyncImage(
            model = photoUri,
            contentDescription = initials,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape),
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        ) {
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun relativeTime(epochMs: Long): String =
    DateUtils
        .getRelativeTimeSpanString(
            epochMs,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString()
