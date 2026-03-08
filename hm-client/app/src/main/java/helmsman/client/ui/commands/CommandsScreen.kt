package helmsman.client.ui.commands

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import helmsman.client.data.model.CommandSpec
import helmsman.client.data.model.RunResult
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import helmsman.client.ui.components.*
import helmsman.client.ui.theme.DeathStrandingColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandsScreen(api: HelmsmanApi, onStreamCommand: (String) -> Unit, navController: NavController) {
    val viewModel: CommandsViewModel = viewModel(factory = CommandsViewModel.Factory(api))
    val commands by viewModel.commands.collectAsStateWithLifecycle()
    val createState by viewModel.createState.collectAsStateWithLifecycle()
    val runResult by viewModel.runResult.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadCommands() }

    var editingCommand by remember { mutableStateOf<CommandSpec?>(null) }
    var showRunResult by remember { mutableStateOf(false) }

    LaunchedEffect(createState) {
        if (createState is UiState.Success) { editingCommand = null; viewModel.resetCreateState() }
    }
    LaunchedEffect(runResult) {
        if (runResult is UiState.Success || runResult is UiState.Error) showRunResult = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commands", fontWeight = FontWeight.Bold, color = DeathStrandingColors.Gold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DeathStrandingColors.Gold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCommands() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = DeathStrandingColors.TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeathStrandingColors.DeepNavy)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingCommand = CommandSpec(id = "", name = "", command = emptyList()) },
                containerColor = DeathStrandingColors.Gold,
                contentColor = DeathStrandingColors.DeepNavy,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Add Command") }
        },
        containerColor = DeathStrandingColors.DeepNavy
    ) { padding ->
        when (val state = commands) {
            is UiState.Loading -> LoadingIndicator(Modifier.padding(padding))
            is UiState.Error -> EmptyStateMessage(Icons.Default.Warning, state.message, Modifier.padding(padding))
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyStateMessage(Icons.Default.Terminal, "No commands registered yet", Modifier.padding(padding))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(state.data, key = { it.id }) { cmd ->
                            CommandCard(
                                command = cmd,
                                isRunning = runResult is UiState.Loading,
                                onRun = { viewModel.runCommand(cmd.id) },
                                onStream = { onStreamCommand(cmd.id) },
                                onEdit = { editingCommand = cmd },
                                onDelete = { viewModel.deleteCommand(cmd.id) }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    editingCommand?.let { cmd ->
        val isNew = cmd.id.isEmpty()
        CommandDialog(
            title = if (isNew) "New Command" else "Edit Command",
            initial = cmd,
            lockId = !isNew,
            onDismiss = { editingCommand = null; viewModel.resetCreateState() },
            onSave = { viewModel.createCommand(it) },
            isLoading = createState is UiState.Loading,
            error = (createState as? UiState.Error)?.message
        )
    }

    if (showRunResult) {
        when (val r = runResult) {
            is UiState.Success -> RunResultDialog(r.data) { showRunResult = false; viewModel.resetRunResult() }
            is UiState.Error -> AlertDialog(
                onDismissRequest = { showRunResult = false; viewModel.resetRunResult() },
                containerColor = DeathStrandingColors.SurfaceCard,
                title = { Text("Execution Failed", color = DeathStrandingColors.ErrorRed, fontWeight = FontWeight.Bold) },
                text = { Text(r.message, color = DeathStrandingColors.TextSecondary) },
                confirmButton = { GoldButton("Close") { showRunResult = false; viewModel.resetRunResult() } }
            )
            else -> {}
        }
    }
}

@Composable
private fun CommandCard(
    command: CommandSpec,
    isRunning: Boolean,
    onRun: () -> Unit,
    onStream: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GoldGradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(command.name, style = MaterialTheme.typography.titleSmall, color = DeathStrandingColors.TextPrimary, fontWeight = FontWeight.Bold)
                GoldChip("id:${command.id}", color = DeathStrandingColors.TextMuted)
            }
            Surface(color = DeathStrandingColors.DeepNavy, shape = RoundedCornerShape(8.dp)) {
                Text(
                    command.command.joinToString(" "),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = DeathStrandingColors.Amber,
                    maxLines = 3
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                command.workingDir?.let { GoldChip("📁 $it", color = DeathStrandingColors.TextMuted) }
                command.timeoutSeconds?.let { GoldChip("⏱ ${it}s", color = DeathStrandingColors.Amber) }
            }
            HorizontalDivider(color = DeathStrandingColors.Border.copy(alpha = 0.4f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRun, enabled = !isRunning,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeathStrandingColors.SuccessGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeathStrandingColors.SuccessGreen.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Run", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onStream,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeathStrandingColors.StreamBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeathStrandingColors.StreamBlue.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Sensors, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Live", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = DeathStrandingColors.Gold.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = DeathStrandingColors.ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun CommandDialog(
    title: String,
    initial: CommandSpec,
    lockId: Boolean,
    onDismiss: () -> Unit,
    onSave: (CommandSpec) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var id by remember { mutableStateOf(initial.id) }
    var name by remember { mutableStateOf(initial.name) }
    var command by remember { mutableStateOf(initial.command.joinToString(" ")) }
    var workingDir by remember { mutableStateOf(initial.workingDir ?: "") }
    var timeout by remember { mutableStateOf(initial.timeoutSeconds?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeathStrandingColors.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, color = DeathStrandingColors.Gold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HmTextField(id, "ID", { id = it }, enabled = !lockId)
                HmTextField(name, "Name", { name = it })
                HmTextField(command, "Command (space-separated args)", { command = it })
                HmTextField(workingDir, "Working directory (optional)", { workingDir = it })
                HmTextField(timeout, "Timeout seconds (optional)", { timeout = it }, keyboardType = KeyboardType.Number)
                if (error != null) HmErrorText(error)
            }
        },
        confirmButton = {
            GoldButton(
                text = if (isLoading) "Saving…" else "Save",
                enabled = !isLoading && id.isNotBlank() && name.isNotBlank() && command.isNotBlank(),
                onClick = {
                    onSave(CommandSpec(
                        id = id.trim(), name = name.trim(),
                        command = command.trim().split(Regex("\\s+")).filter { it.isNotBlank() },
                        workingDir = workingDir.ifBlank { null },
                        timeoutSeconds = timeout.toIntOrNull()
                    ))
                }
            )
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = DeathStrandingColors.TextMuted) } }
    )
}

@Composable
private fun RunResultDialog(result: RunResult, onDismiss: () -> Unit) {
    val ok = (result.exitCode ?: -1) == 0
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeathStrandingColors.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    if (ok) Icons.Default.CheckCircle else Icons.Default.Cancel, null,
                    tint = if (ok) DeathStrandingColors.SuccessGreen else DeathStrandingColors.ErrorRed,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text("Exit: ${result.exitCode ?: "killed"}", color = DeathStrandingColors.Gold, fontWeight = FontWeight.Bold)
                    Text("${result.durationMs} ms", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.TextMuted)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.stdout.isNotBlank()) {
                    Text("STDOUT", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.Gold, letterSpacing = 2.sp)
                    Surface(color = DeathStrandingColors.DeepNavy, shape = RoundedCornerShape(8.dp)) {
                        Text(result.stdout.take(800), modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = DeathStrandingColors.SuccessGreen)
                    }
                }
                if (result.stderr.isNotBlank()) {
                    Text("STDERR", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.ErrorRed, letterSpacing = 2.sp)
                    Surface(color = DeathStrandingColors.DeepNavy, shape = RoundedCornerShape(8.dp)) {
                        Text(result.stderr.take(400), modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = DeathStrandingColors.ErrorRed)
                    }
                }
                if (result.stdout.isBlank() && result.stderr.isBlank()) {
                    Text("(no output)", style = MaterialTheme.typography.bodySmall, color = DeathStrandingColors.TextMuted)
                }
            }
        },
        confirmButton = { GoldButton("Close", onClick = onDismiss) }
    )
}
