package helmsman.client.ui.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import helmsman.client.data.model.Server
import helmsman.client.data.model.ServerStatus
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import helmsman.client.ui.commands.goldTextFieldColors
import helmsman.client.ui.components.*
import helmsman.client.ui.theme.DeathStrandingColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    api: HelmsmanApi,
    navController: NavController
) {
    val viewModel: ServersViewModel = viewModel(factory = ServersViewModel.Factory(api))
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val createState by viewModel.createState.collectAsStateWithLifecycle()
    val serverStatuses by viewModel.serverStatuses.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadServers() }

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(createState) {
        if (createState is UiState.Success) {
            showAddDialog = false
            viewModel.resetCreateState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Servers", fontWeight = FontWeight.Bold, color = DeathStrandingColors.Gold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DeathStrandingColors.Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeathStrandingColors.DeepNavy
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DeathStrandingColors.Gold,
                contentColor = DeathStrandingColors.DeepNavy
            ) {
                Icon(Icons.Default.Add, "Add Server")
            }
        },
        containerColor = DeathStrandingColors.DeepNavy
    ) { padding ->
        when (val state = servers) {
            is UiState.Loading -> LoadingIndicator(Modifier.padding(padding))
            is UiState.Error -> {
                EmptyStateMessage(
                    icon = Icons.Default.Error,
                    message = state.message,
                    modifier = Modifier.padding(padding)
                )
            }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyStateMessage(
                        icon = Icons.Default.Dns,
                        message = "No servers registered yet",
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(state.data, key = { it.id }) { server ->
                            ServerCard(
                                server = server,
                                status = serverStatuses[server.id],
                                onProbe = { viewModel.probeServer(server.id) },
                                onDelete = { viewModel.deleteServer(server.id) }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { viewModel.createServer(it) },
            isLoading = createState is UiState.Loading,
            error = (createState as? UiState.Error)?.message
        )
    }
}

@Composable
private fun ServerCard(
    server: Server,
    status: UiState<ServerStatus>?,
    onProbe: () -> Unit,
    onDelete: () -> Unit
) {
    GoldGradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = DeathStrandingColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = server.host,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = DeathStrandingColors.Amber
                    )
                }
                Row {
                    IconButton(onClick = onProbe) {
                        Icon(Icons.Default.NetworkPing, "Probe", tint = DeathStrandingColors.InfoCyan)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = DeathStrandingColors.ErrorRed.copy(alpha = 0.7f))
                    }
                }
            }

            when (status) {
                is UiState.Loading -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = DeathStrandingColors.Gold,
                        trackColor = DeathStrandingColors.Border
                    )
                }
                is UiState.Success -> {
                    val data = status.data
                    Surface(
                        color = DeathStrandingColors.DeepNavy,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusDot(isOnline = data.pingMs != null)
                                Text(
                                    text = data.pingMs?.let { "%.2f ms".format(it) } ?: "Unreachable",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (data.pingMs != null)
                                        DeathStrandingColors.SuccessGreen
                                    else
                                        DeathStrandingColors.ErrorRed
                                )
                            }
                            if (data.pingError != null) {
                                Text(
                                    text = data.pingError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeathStrandingColors.ErrorRed
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = data.uname,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = DeathStrandingColors.TextMuted,
                                maxLines = 2
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = DeathStrandingColors.ErrorRed
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onAdd: (Server) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeathStrandingColors.SurfaceCard,
        title = {
            Text("Register Server", color = DeathStrandingColors.Gold, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = id, onValueChange = { id = it },
                    label = { Text("Server ID") },
                    singleLine = true,
                    colors = goldTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = goldTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = host, onValueChange = { host = it },
                    label = { Text("Host (IP or hostname)") },
                    singleLine = true,
                    colors = goldTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("192.168.1.42", color = DeathStrandingColors.TextMuted)
                    }
                )
                if (error != null) {
                    Text(error, color = DeathStrandingColors.ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            GoldButton(
                text = if (isLoading) "Adding..." else "Register",
                onClick = {
                    if (id.isNotBlank() && name.isNotBlank() && host.isNotBlank()) {
                        onAdd(Server(id = id.trim(), name = name.trim(), host = host.trim()))
                    }
                },
                enabled = !isLoading && id.isNotBlank() && name.isNotBlank() && host.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DeathStrandingColors.TextSecondary)
            }
        }
    )
}
