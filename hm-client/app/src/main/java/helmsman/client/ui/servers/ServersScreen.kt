package helmsman.client.ui.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import helmsman.client.data.model.Server
import helmsman.client.data.model.ServerStatus
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import helmsman.client.ui.components.*
import helmsman.client.ui.theme.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(api: HelmsmanApi, contentPadding: PaddingValues = PaddingValues()) {
    val vm: ServersViewModel = viewModel(factory = ServersViewModel.Factory(api))
    val servers     by vm.servers.collectAsStateWithLifecycle()
    val createState by vm.createState.collectAsStateWithLifecycle()
    val statuses    by vm.serverStatuses.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadServers() }

    var editing by remember { mutableStateOf<Server?>(null) }
    LaunchedEffect(createState) {
        if (createState is UiState.Success) { editing = null; vm.resetCreateState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servers", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { vm.loadServers() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { editing = Server("", "", "") }) {
                        Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        when (val s = servers) {
            is UiState.Loading -> LoadingIndicator(Modifier.padding(inner).padding(contentPadding))
            is UiState.Error   -> EmptyState(Icons.Default.Warning, s.message, Modifier.padding(inner).padding(contentPadding))
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyState(Icons.Default.Dns, "No servers configured yet", Modifier.padding(inner).padding(contentPadding))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(inner).padding(contentPadding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(s.data, key = { it.id }) { server ->
                            ServerCard(
                                server = server,
                                status = statuses[server.id],
                                onProbe  = { vm.probeServer(server.id) },
                                onEdit   = { editing = server },
                                onDelete = { vm.deleteServer(server.id) }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    editing?.let { server ->
        ServerDialog(
            initial   = server,
            isNew     = server.id.isEmpty(),
            isLoading = createState is UiState.Loading,
            error     = (createState as? UiState.Error)?.message,
            onDismiss = { editing = null; vm.resetCreateState() },
            onSave    = { vm.createServer(it) }
        )
    }
}

@Composable
private fun ServerCard(
    server: Server,
    status: UiState<ServerStatus>?,
    onProbe: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalExtendedColors.current
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(server.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(server.host, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        when (status) {
            null -> TextButton(
                onClick = onProbe,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.NetworkCheck, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Probe", style = MaterialTheme.typography.labelMedium)
            }

            is UiState.Loading -> LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )

            is UiState.Success -> {
                val s = status.data
                val online = s.pingMs != null
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Ping chip
                    Surface(
                        color = (if (online) ext.success else MaterialTheme.colorScheme.error).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusDot(isOnline = online, size = 7.dp)
                            Text(
                                s.pingMs?.let { "%.0f ms".format(it) } ?: "offline",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (online) ext.success else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    // Re-probe button
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onProbe, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, "Re-probe", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (s.uname.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            s.uname,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            is UiState.Error -> {
                Text(status.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onProbe, contentPadding = PaddingValues(0.dp)) {
                    Text("Retry", style = MaterialTheme.typography.labelSmall)
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun ServerDialog(
    initial: Server,
    isNew: Boolean,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (Server) -> Unit
) {
    var id   by remember { mutableStateOf(initial.id) }
    var name by remember { mutableStateOf(initial.name) }
    var host by remember { mutableStateOf(initial.host) }

    HmDialog(
        title          = if (isNew) "New server" else "Edit server",
        confirmLabel   = "Save",
        confirmEnabled = id.isNotBlank() && name.isNotBlank() && host.isNotBlank(),
        isLoading      = isLoading,
        onDismiss      = onDismiss,
        onConfirm      = { onSave(Server(id.trim(), name.trim(), host.trim())) }
    ) {
        HmTextField(id, "Server ID", { id = it }, enabled = isNew)
        HmTextField(name, "Display name", { name = it })
        HmTextField(host, "Hostname or IP", { host = it })
        if (error != null) HmErrorText(error)
    }
}
