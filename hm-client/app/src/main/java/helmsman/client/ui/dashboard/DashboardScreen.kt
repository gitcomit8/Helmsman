package helmsman.client.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import helmsman.client.data.model.ServerStatus
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import helmsman.client.ui.components.*
import helmsman.client.ui.theme.AppThemeMode
import helmsman.client.ui.theme.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    api: HelmsmanApi,
    themeMode: AppThemeMode,
    onCycleTheme: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(api))
    val daemonStatus by vm.daemonStatus.collectAsStateWithLifecycle()
    val servers      by vm.servers.collectAsStateWithLifecycle()
    val statuses     by vm.serverStatuses.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refresh() }

    val ext = LocalExtendedColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Helmsman", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "100.111.67.98:3000",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                actions = {
                    // Theme toggle — icon clearly different per mode
                    IconButton(onClick = onCycleTheme) {
                        Icon(
                            if (themeMode == AppThemeMode.AMOLED) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Switch theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Daemon status card ─────────────────────────────────────────
            val isOnline = daemonStatus is UiState.Success
            Card(
                shape  = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatusDot(isOnline = isOnline, size = 10.dp)
                        Column {
                            Text(
                                when (daemonStatus) {
                                    is UiState.Success -> "Daemon operational"
                                    is UiState.Loading -> "Connecting…"
                                    is UiState.Error   -> "Unreachable"
                                    else               -> "Unknown"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOnline) ext.success else MaterialTheme.colorScheme.error
                            )
                            if (daemonStatus is UiState.Error) {
                                Text(
                                    (daemonStatus as UiState.Error).message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    if (daemonStatus is UiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
            }

            // ── Server list ────────────────────────────────────────────────
            if (servers is UiState.Success) {
                val list = (servers as UiState.Success).data
                if (list.isNotEmpty()) {
                    SectionHeader("SERVERS")
                    list.forEach { server ->
                        val status = statuses[server.id]
                        Card(
                            shape  = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier  = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(server.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(server.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                                    }
                                    if (status != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            StatusDot(isOnline = status.pingMs != null, size = 8.dp)
                                            Text(
                                                status.pingMs?.let { "%.0f ms".format(it) } ?: "offline",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (status.pingMs != null) ext.success else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                                    }
                                }
                                if (status?.uname?.isNotBlank() == true) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            status.uname,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (servers is UiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
