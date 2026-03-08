package helmsman.client.ui.commands

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
fun CommandsScreen(
    api: HelmsmanApi,
    onStreamCommand: (String) -> Unit,
    navController: NavController
) {
    val viewModel: CommandsViewModel = viewModel(
        factory = CommandsViewModel.Factory(api)
    )
    val commands by viewModel.commands.collectAsStateWithLifecycle()
    val createState by viewModel.createState.collectAsStateWithLifecycle()
    val runResult by viewModel.runResult.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadCommands() }

    var showAddDialog by remember { mutableStateOf(false) }
    var showRunResult by remember { mutableStateOf(false) }

    LaunchedEffect(createState) {
        if (createState is UiState.Success) {
            showAddDialog = false
            viewModel.resetCreateState()
        }
    }

    LaunchedEffect(runResult) {
        if (runResult is UiState.Success || runResult is UiState.Error) {
            showRunResult = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Commands", fontWeight = FontWeight.Bold, color = DeathStrandingColors.Gold)
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
                Icon(Icons.Default.Add, "Add Command")
            }
        },
        containerColor = DeathStrandingColors.DeepNavy
    ) { padding ->
        when (val state = commands) {
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
                        icon = Icons.Default.Terminal,
                        message = "No commands registered yet",
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
                        items(state.data, key = { it.id }) { command ->
                            CommandCard(
                                command = command,
                                isRunning = runResult is UiState.Loading,
                                onRun = { viewModel.runCommand(command.id) },
                                onStream = { onStreamCommand(command.id) },
                                onDelete = { viewModel.deleteCommand(command.id) }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    if (showAddDialog) {
        AddCommandDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { viewModel.createCommand(it) },
            isLoading = createState is UiState.Loading,
            error = (createState as? UiState.Error)?.message
        )
    }

    if (showRunResult && runResult is UiState.Success) {
        RunResultDialog(
            result = (runResult as UiState.Success).data,
            onDismiss = {
                showRunResult = false
                viewModel.resetRunResult()
            }
        )
    }
}

@Composable
private fun CommandCard(
    command: CommandSpec,
    isRunning: Boolean,
    onRun: () -> Unit,
    onStream: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    GoldGradientCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = command.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = DeathStrandingColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = command.command.joinToString(" "),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = DeathStrandingColors.Amber,
                    maxLines = 2
                )
                if (command.workingDir != null) {
                    Text(
                        text = "📁 ${command.workingDir}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeathStrandingColors.TextMuted
                    )
                }
                if (command.timeoutSeconds != null) {
                    Text(
                        text = "⏱ ${command.timeoutSeconds}s timeout",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeathStrandingColors.TextMuted
                    )
                }
            }

            Row {
                IconButton(onClick = onRun, enabled = !isRunning) {
                    Icon(
                        Icons.Default.PlayArrow,
                        "Run",
                        tint = DeathStrandingColors.SuccessGreen
                    )
                }
                IconButton(onClick = onStream) {
                    Icon(
                        Icons.Default.Stream,
                        "Stream",
                        tint = DeathStrandingColors.InfoCyan
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = DeathStrandingColors.ErrorRed.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCommandDialog(
    onDismiss: () -> Unit,
    onAdd: (CommandSpec) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var workingDir by remember { mutableStateOf("") }
    var timeout by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeathStrandingColors.SurfaceCard,
        title = {
            Text("New Command", color = DeathStrandingColors.Gold, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = id, onValueChange = { id = it },
                    label = { Text("ID (e.g. backup-db)") },
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
                    value = command, onValueChange = { command = it },
                    label = { Text("Command (space separated)") },
                    singleLine = true,
                    colors = goldTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = workingDir, onValueChange = { workingDir = it },
                    label = { Text("Working Directory (optional)") },
                    singleLine = true,
                    colors = goldTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = timeout, onValueChange = { timeout = it },
                    label = { Text("Timeout (seconds, optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = goldTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error, color = DeathStrandingColors.ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            GoldButton(
                text = if (isLoading) "Adding..." else "Add",
                onClick = {
                    val argv = command.split(" ").filter { it.isNotBlank() }
                    if (id.isNotBlank() && name.isNotBlank() && argv.isNotEmpty()) {
                        onAdd(
                            CommandSpec(
                                id = id.trim(),
                                name = name.trim(),
                                command = argv,
                                workingDir = workingDir.ifBlank { null },
                                timeoutSeconds = timeout.toIntOrNull()
                            )
                        )
                    }
                },
                enabled = !isLoading && id.isNotBlank() && name.isNotBlank() && command.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DeathStrandingColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun RunResultDialog(result: RunResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeathStrandingColors.SurfaceCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (result.exitCode == 0) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (result.exitCode == 0)
                        DeathStrandingColors.SuccessGreen
                    else
                        DeathStrandingColors.ErrorRed
                )
                Text(
                    "Exit Code: ${result.exitCode ?: "killed"}",
                    color = DeathStrandingColors.Gold,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Duration: ${result.durationMs}ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = DeathStrandingColors.TextSecondary
                )
                if (result.stdout.isNotBlank()) {
                    Text("stdout", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.Gold)
                    Surface(
                        color = DeathStrandingColors.DeepNavy,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = result.stdout,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = DeathStrandingColors.SuccessGreen,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                if (result.stderr.isNotBlank()) {
                    Text("stderr", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.ErrorRed)
                    Surface(
                        color = DeathStrandingColors.DeepNavy,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = result.stderr,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = DeathStrandingColors.ErrorRed,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            GoldButton(text = "Close", onClick = onDismiss)
        }
    )
}

@Composable
fun goldTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DeathStrandingColors.Gold,
    unfocusedBorderColor = DeathStrandingColors.Border,
    cursorColor = DeathStrandingColors.Gold,
    focusedLabelColor = DeathStrandingColors.Gold,
    unfocusedLabelColor = DeathStrandingColors.TextMuted,
    focusedTextColor = DeathStrandingColors.TextPrimary,
    unfocusedTextColor = DeathStrandingColors.TextPrimary
)
