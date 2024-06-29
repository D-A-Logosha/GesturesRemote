package com.example.appserver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


@Composable
@ExperimentalMaterial3Api
fun EventLogScreen(
    viewModel: EventLogViewModel = koinViewModel(),
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val pullRefreshState = rememberPullToRefreshState()
    LaunchedEffect(key1 = pullRefreshState.isRefreshing, key2 = uiState.isRefreshing) {
        if (pullRefreshState.isRefreshing) viewModel.refreshEvents()
        if (!uiState.isRefreshing) pullRefreshState.endRefresh()
    }

    LaunchedEffect(
        key1 = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0,
        key2 = uiState.reachedEnd,
        key3 = uiState.isLoading
    ) {
        val indexLastVisibleItems =
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (indexLastVisibleItems > uiState.events.size - 10 && !uiState.reachedEnd && !uiState.isLoading) {
            viewModel.loadEvents()
        }
    }
    LaunchedEffect(
        key1 = uiState.scrollMode == EventLogUiState.ScrollMode.AUTO,
        key2 = uiState.events.firstOrNull()?.id
    ) {
        if (uiState.scrollMode == EventLogUiState.ScrollMode.AUTO) {
            listState.animateScrollToItem(index = 0)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { viewModel.toggleScrollMode() },
                    containerColor = if (uiState.scrollMode == EventLogUiState.ScrollMode.AUTO) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        FloatingActionButtonDefaults.containerColor
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Auto scroll to new top"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        }, modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(all = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                state = listState,
            ) {
                if (!uiState.isLoading && !uiState.isRefreshing) {
                    items(uiState.events, key = { it.id }) { event ->
                        EventCard(event = event)
                    }
                }
            }
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            LaunchedEffect(key1 = uiState.error) {
                uiState.error?.let { error ->
                    launch {
                        snackbarHostState.showSnackbar(message = error)
                    }
                    viewModel.onErrorShown()
                }
            }
        }
    }
}

@Composable
fun EventCard(event: EventLogUiState.EventUI) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                text = event.timestamp, style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "${event.eventType}", style = MaterialTheme.typography.titleSmall
            )
            if (event.clientId != null) {
                Text(
                    text = "Client ID: ${event.clientId}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (event.details != null) {
                Text(
                    text = "Details: ${event.details}", style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
