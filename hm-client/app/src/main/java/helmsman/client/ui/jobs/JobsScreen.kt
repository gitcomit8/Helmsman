package helmsman.client.ui.jobs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import helmsman.client.data.model.Job
import helmsman.client.data.model.JobCreateRequest
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import helmsman.client.ui.commands.goldTextFieldColors
import helmsman.client.ui.components.*
import helmsman.client.ui.theme.DeathStrandingColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    api: HelmsmanApi,
    navController: NavController
) {
    val viewModel: JobsViewModel = viewModel(factory = JobsViewModel.Factory(api))
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val createState by viewModel.createState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadJobs() }

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
                    Text("Scheduled Jobs", fontWeight = FontWeight.Bold, color = DeathStrandingColors.Gold)
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
                Icon(Icons.Default.Add, "Add Job")
            }
        },
        containerColor = DeathStrandingColors.DeepNavy
    ) { padding ->
        when (val state = jobs) {
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
                        icon = Icons.Default.Schedule,
                        message = "No jobs scheduled yet",
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
                        items(state.data, key = { it.id }) { job ->
                            JobCard(
                                job = job,
                                onDelete = { viewModel.deleteJob(job.id) }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    if (showAddDialog) {
        AddJobDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { viewModel.createJob(it) },
            isLoading = createState is UiState.Loading,
            error = (createState as? UiState.Error)?.message
        )
    }
}

@Composable
private fun JobCard(job: Job, onDelete: () -> Unit) {
    GoldGradientCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = DeathStrandingColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = DeathStrandingColors.Amber
                    )
                    Text(
                        text = job.schedule,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = DeathStrandingColors.Amber
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Command: ${job.commandId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeathStrandingColors.TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            "Last Run",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeathStrandingColors.TextMuted
                        )
                        Text(
                            text = job.lastRun?.take(19)?.replace("T", " ") ?: "Never",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeathStrandingColors.TextSecondary
                        )
                    }
                    Column {
                        Text(
                            "Next Run",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeathStrandingColors.TextMuted
                        )
                        Text(
                            text = job.nextRun.take(19).replace("T", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = DeathStrandingColors.InfoCyan
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = DeathStrandingColors.ErrorRed.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun AddJobDialog(
    onDismiss: () -> Unit,
    onAdd: (JobCreateRequest) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("") }
    var commandId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeathStrandingColors.SurfaceCard,
        title = {
            Text("Schedule Job", color = DeathStrandingColors.Gold, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = id, onValueChange = { id = it },
                    label = { Text("Job ID") },
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
                    value = schedule, onValueChange = { schedule = it },
                    label = { Text("Cron (sec min hr day mon dow)") },
                    singleLine = true,
                    colors = goldTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("0 0 2 * * *", color = DeathStrandingColors.TextMuted)
                    }
                )
                OutlinedTextField(
                    value = commandId, onValueChange = { commandId = it },
                    label = { Text("Command ID") },
                    singleLine = true,
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
                text = if (isLoading) "Scheduling..." else "Schedule",
                onClick = {
                    if (id.isNotBlank() && name.isNotBlank() &&
                        schedule.isNotBlank() && commandId.isNotBlank()
                    ) {
                        onAdd(
                            JobCreateRequest(
                                id = id.trim(),
                                name = name.trim(),
                                schedule = schedule.trim(),
                                commandId = commandId.trim()
                            )
                        )
                    }
                },
                enabled = !isLoading && id.isNotBlank() && name.isNotBlank() &&
                        schedule.isNotBlank() && commandId.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DeathStrandingColors.TextSecondary)
            }
        }
    )
}
