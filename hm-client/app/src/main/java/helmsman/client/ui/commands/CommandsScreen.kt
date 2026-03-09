package helmsman.client.ui.commands

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import helmsman.client.data.model.CommandSpec
import helmsman.client.data.model.RunResult
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import helmsman.client.ui.components.*
import helmsman.client.ui.theme.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandsScreen(
    api: HelmsmanApi,
    onStreamCommand: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val vm: CommandsViewModel = viewModel(factory = CommandsViewModel.Factory(api))
    val commands    by vm.commands.collectAsStateWithLifecycle()
    val createState by vm.createState.collectAsStateWithLifecycle()
    val runResult   by vm.runResult.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadCommands() }

    var editing by remember { mutableStateOf<CommandSpec?>(null) }
    var showResult by remember { mutableStateOf(false) }

    LaunchedEffect(createState) {
        if (createState is UiState.Success) { editing = null; vm.resetCreateState() }
    }
    LaunchedEffect(runResult) {
        if (runResult is UiState.Success || runResult is UiState.Error) showResult = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commands", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { vm.loadCommands() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { editing = CommandSpec("", "", emptyList()) }) {
                        Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        when (val s = commands) {
            is UiState.Loading -> LoadingIndicator(Modifier.padding(inner).padding(contentPadding))
            is UiState.Error   -> EmptyState(Icons.Default.Warning, s.message, Modifier.padding(inner).padding(contentPadding))
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyState(Icons.Default.Terminal, "No commands yet — tap + to add one", Modifier.padding(inner).padding(contentPadding))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(inner).padding(contentPadding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(s.data, key = { it.id }) { cmd ->
                            CommandCard(
                                cmd = cmd,
                                isRunLoading = runResult is UiState.Loading,
                                onRun    = { vm.runCommand(cmd.id) },
                                onStream = { onStreamCommand(cmd.id) },
                                onEdit   = { editing = cmd },
                                onDelete = { vm.deleteCommand(cmd.id) }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    editing?.let { cmd ->
        CommandDialog(
            initial  = cmd,
            isNew    = cmd.id.isEmpty(),
            isLoading = createState is UiState.Loading,
            error    = (createState as? UiState.Error)?.message,
            onDismiss = { editing = null; vm.resetCreateState() },
            onSave   = { vm.createCommand(it) }
        )
    }

    if (showResult) {
        when (val r = runResult) {
            is UiState.Success -> RunResultSheet(r.data) { showResult = false; vm.resetRunResult() }
            is UiState.Error   -> AlertDialog(
                onDismissRequest = { showResult = false; vm.resetRunResult() },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                title = { Text("Run failed") },
                text  = { Text(r.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = { TextButton({ showResult = false; vm.resetRunResult() }) { Text("OK") } }
            )
            else -> {}
        }
    }
}

@Composable
private fun CommandCard(
    cmd: CommandSpec,
    isRunLoading: Boolean,
    onRun: () -> Unit,
    onStream: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(cmd.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        cmd.command.joinToString(" "),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag("id:${cmd.id}")
                    cmd.timeoutSeconds?.let { Tag("${it}s", MaterialTheme.colorScheme.secondary) }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val ext = LocalExtendedColors.current
            OutlinedButton(
                onClick = onRun, enabled = !isRunLoading,
                modifier = Modifier.weight(1f).height(34.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.success),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ext.success.copy(alpha = 0.4f))
                )
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Run", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = onStream,
                modifier = Modifier.weight(1f).height(34.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                )
            ) {
                Icon(Icons.Default.Sensors, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Live", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun CommandDialog(
    initial: CommandSpec,
    isNew: Boolean,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (CommandSpec) -> Unit
) {
    var id      by remember { mutableStateOf(initial.id) }
    var name    by remember { mutableStateOf(initial.name) }
    var command by remember { mutableStateOf(initial.command.joinToString(" ")) }
    var workDir by remember { mutableStateOf(initial.workingDir ?: "") }
    var timeout by remember { mutableStateOf(initial.timeoutSeconds?.toString() ?: "") }

    HmDialog(
        title         = if (isNew) "New command" else "Edit command",
        confirmLabel  = "Save",
        confirmEnabled = id.isNotBlank() && name.isNotBlank() && command.isNotBlank(),
        isLoading     = isLoading,
        onDismiss     = onDismiss,
        onConfirm     = {
            onSave(CommandSpec(
                id = id.trim(), name = name.trim(),
                command = command.trim().split(Regex("\\s+")).filter { it.isNotBlank() },
                workingDir = workDir.ifBlank { null },
                timeoutSeconds = timeout.toIntOrNull()
            ))
        }
    ) {
        HmTextField(id, "ID", { id = it }, enabled = isNew)
        HmTextField(name, "Name", { name = it })
        HmTextField(command, "Command (space-separated)", { command = it })
        HmTextField(workDir, "Working directory (optional)", { workDir = it })
        HmTextField(timeout, "Timeout seconds (optional)", { timeout = it }, keyboardType = KeyboardType.Number)
        if (error != null) HmErrorText(error)
    }
}

@Composable
private fun RunResultSheet(result: RunResult, onDismiss: () -> Unit) {
    val ext = LocalExtendedColors.current
    val ok = (result.exitCode ?: -1) == 0
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(isOnline = ok, size = 10.dp)
                Text("Exit ${result.exitCode ?: "killed"} · ${result.durationMs} ms")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.stdout.isNotBlank()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            result.stdout.take(1000),
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = ext.onCode
                        )
                    }
                }
                if (result.stderr.isNotBlank()) {
                    Text("stderr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            result.stderr.take(400),
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Close") } }
    )
}
