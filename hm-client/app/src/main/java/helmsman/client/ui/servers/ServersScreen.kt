package helmsman.client.ui.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import helmsman.client.data.model.Server
import helmsman.client.data.model.ServerStatus
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState


import helmsman.client.ui.components.*
import helmsman.client.ui.theme.DeathStrandingColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(api: HelmsmanApi, navController: NavController) {
    val viewModel: ServersViewModel = viewModel(factory = ServersViewModel.Factory(api))
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val createState by viewModel.createState.collectAsStateWithLifecycle()
    val statuses by viewModel.serverStatuses.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadServers() }

    var editingServer by remember { mutableStateOf<Server?>(null) }

    LaunchedEffect(createState) {
        if (createState is UiState.Success) { editingServer = null; viewModel.resetCreateState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servers", fontWeight = FontWeight.Bold, color = DeathStrandingColors.Gold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DeathStrandingColors.Gold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadServers() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = DeathStrandingColors.TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeathStrandingColors.DeepNavy)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingServer = Server(id = "", name = "", host = "") },
                containerColor = DeathStrandingColors.Gold,
                contentColor = DeathStrandingColors.DeepNavy,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Add Server") }
        },
        containerColor = DeathStrandingColors.DeepNavy
    ) { padding ->
        when (val state = servers) {
            is UiState.Loading -> LoadingIndicator(Modifier.padding(padding))
            is UiState.Error -> EmptyStateMessage(Icons.Default.Warning, state.message, Modifier.padding(padding))
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyStateMessage(Icons.Default.Dns, "No remote servers configured", Modifier.padding(padding))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(state.data, key = { it.id }) { server ->
                            ServerCard(
                                server = server,
                                statusState = statuses[server.id],
                                onProbe = { viewModel.probeServer(server.id) },
                                onEdit = { editingServer = server },
                                onDelete = { viewModel.deleteServer(server.id) }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    editingServer?.let { server ->
        val isNew = server.id.isEmpty()
        ServerDialog(
            title = if (isNew) "New Server" else "Edit Server",
            initial = server,
            lockId = !isNew,
            onDismiss = { editingServer = null; viewModel.resetCreateState() },
            onSave = { viewModel.createServer(it) },
            isLoading = createState is UiState.Loading,
            error = (createState as? UiState.Error)?.message
        )
    }
}

@Composable
private fun ServerCard(
    server: Server,
    statusState: UiState<ServerStatus>?,
    onProbe: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GoldGradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Dns, null, tint = DeathStrandingColors.Gold, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(server.name, style = MaterialTheme.typography.titleSmall, color = DeathStrandingColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text(server.host, style = MaterialTheme.typography.bodySmall, color = DeathStrandingColors.TextMuted, fontFamily = FontFamily.Monospace)
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = DeathStrandingColors.Gold.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = DeathStrandingColors.ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            when (statusState) {
                null -> OutlinedButton(
                    onClick = onProbe,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeathStrandingColors.Gold),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeathStrandingColors.Gold.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.NetworkCheck, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Probe Server", style = MaterialTheme.typography.labelMedium)
                }
                is UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = DeathStrandingColors.Gold, trackColor = DeathStrandingColors.Border)
                is UiState.Success -> ServerStatusPanel(statusState.data, onProbe)
                is UiState.Error -> {
                    Surface(color = DeathStrandingColors.ErrorRed.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(statusState.message, style = MaterialTheme.typography.bodySmall, color = DeathStrandingColors.ErrorRed, modifier = Modifier.weight(1f))
                            TextButton(onProbe, contentPadding = PaddingValues(8.dp)) { Text("Retry", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.Gold) }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ServerStatusPanel(status: ServerStatus, onRefresh: () -> Unit) {
    val pingOk = status.pingMs != null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = DeathStrandingColors.Border.copy(alpha = 0.4f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(modifier = Modifier.weight(1f), color = DeathStrandingColors.DeepNavy, shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusDot(isOnline = pingOk, size = 8.dp)
                        Text(if (pingOk) "REACHABLE" else "OFFLINE", style = MaterialTheme.typography.labelSmall, color = if (pingOk) DeathStrandingColors.SuccessGreen else DeathStrandingColors.ErrorRed, letterSpacing = 1.5.sp)
                    }
                    if (pingOk) {
                        Text("${status.pingMs?.let { "%.0f".format(it) }} ms", style = MaterialTheme.typography.headlineSmall, color = DeathStrandingColors.Gold, fontWeight = FontWeight.Bold)
                    } else {
                        Text(status.pingError ?: "unreachable", style = MaterialTheme.typography.bodySmall, color = DeathStrandingColors.ErrorRed)
                    }
                }
            }
            Surface(modifier = Modifier.weight(2f), color = DeathStrandingColors.DeepNavy, shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("OS / KERNEL", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.TextMuted, letterSpacing = 1.5.sp)
                    Text(status.uname, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = DeathStrandingColors.TextSecondary, maxLines = 3)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onRefresh, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp), tint = DeathStrandingColors.TextMuted)
                Spacer(Modifier.width(4.dp))
                Text("Re-probe", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.TextMuted)
            }
        }
    }
}

@Composable
private fun ServerDialog(
    title: String,
    initial: Server,
    lockId: Boolean,
    onDismiss: () -> Unit,
    onSave: (Server) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var id by remember { mutableStateOf(initial.id) }
    var name by remember { mutableStateOf(initial.name) }
    var host by remember { mutableStateOf(initial.host) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeathStrandingColors.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, color = DeathStrandingColors.Gold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HmTextField(value = id, label = "Server ID", onValueChange = { id = it }, enabled = !lockId)
                HmTextField(value = name, label = "Display name", onValueChange = { name = it })
                HmTextField(value = host, label = "Hostname or IP", onValueChange = { host = it })
                if (error != null) HmErrorText(error)
            }
        },
        confirmButton = {
            GoldButton(
                text = if (isLoading) "Saving…" else "Save",
                enabled = !isLoading && id.isNotBlank() && name.isNotBlank() && host.isNotBlank(),
                onClick = { onSave(Server(id = id.trim(), name = name.trim(), host = host.trim())) }
            )
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = DeathStrandingColors.TextMuted) } }
    )
}
