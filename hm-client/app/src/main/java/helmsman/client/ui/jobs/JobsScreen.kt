package helmsman.client.ui.jobs

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import helmsman.client.data.model.Job
import helmsman.client.data.model.JobCreateRequest
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState


import helmsman.client.ui.components.*
import helmsman.client.ui.theme.DeathStrandingColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(api: HelmsmanApi, navController: NavController) {
    val viewModel: JobsViewModel = viewModel(factory = JobsViewModel.Factory(api))
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val createState by viewModel.createState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadJobs() }

    var editingJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(createState) {
        if (createState is UiState.Success) { editingJob = null; viewModel.resetCreateState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled Jobs", fontWeight = FontWeight.Bold, color = DeathStrandingColors.Gold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DeathStrandingColors.Gold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadJobs() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = DeathStrandingColors.TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeathStrandingColors.DeepNavy)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingJob = Job(id = "", name = "", schedule = "", commandId = "", nextRun = "") },
                containerColor = DeathStrandingColors.Gold,
                contentColor = DeathStrandingColors.DeepNavy,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Add Job") }
        },
        containerColor = DeathStrandingColors.DeepNavy
    ) { padding ->
        when (val state = jobs) {
            is UiState.Loading -> LoadingIndicator(Modifier.padding(padding))
            is UiState.Error -> EmptyStateMessage(Icons.Default.Warning, state.message, Modifier.padding(padding))
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyStateMessage(Icons.Default.Schedule, "No scheduled jobs yet", Modifier.padding(padding))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(state.data, key = { it.id }) { job ->
                            JobCard(
                                job = job,
                                onEdit = { editingJob = job },
                                onDelete = { viewModel.deleteJob(job.id) }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    editingJob?.let { job ->
        val isNew = job.id.isEmpty()
        JobDialog(
            title = if (isNew) "New Job" else "Edit Job",
            initial = job,
            lockId = !isNew,
            onDismiss = { editingJob = null; viewModel.resetCreateState() },
            onSave = { viewModel.createJob(it) },
            isLoading = createState is UiState.Loading,
            error = (createState as? UiState.Error)?.message
        )
    }
}

@Composable
private fun JobCard(job: Job, onEdit: () -> Unit, onDelete: () -> Unit) {
    GoldGradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(job.name, style = MaterialTheme.typography.titleSmall, color = DeathStrandingColors.TextPrimary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = DeathStrandingColors.Gold.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = DeathStrandingColors.ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            Surface(color = DeathStrandingColors.DeepNavy, shape = RoundedCornerShape(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = DeathStrandingColors.Gold, modifier = Modifier.size(16.dp))
                    Text(job.schedule, style = MaterialTheme.typography.bodySmall, color = DeathStrandingColors.Amber, fontWeight = FontWeight.Medium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldChip("id:${job.id}", color = DeathStrandingColors.TextMuted)
                GoldChip("cmd:${job.commandId}", color = DeathStrandingColors.StreamBlue)
            }
            HorizontalDivider(color = DeathStrandingColors.Border.copy(alpha = 0.4f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (job.lastRun != null) {
                    Column {
                        Text("LAST RUN", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.TextMuted, letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp))
                        Text(job.lastRun.take(16).replace('T', ' '), style = MaterialTheme.typography.bodySmall, color = DeathStrandingColors.TextSecondary)
                    }
                }
                Column {
                    Text("NEXT RUN", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.TextMuted, letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Text(job.nextRun.take(16).replace('T', ' '), style = MaterialTheme.typography.bodySmall, color = DeathStrandingColors.Gold)
                }
            }
        }
    }
}

@Composable
private fun JobDialog(
    title: String,
    initial: Job,
    lockId: Boolean,
    onDismiss: () -> Unit,
    onSave: (JobCreateRequest) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var id by remember { mutableStateOf(initial.id) }
    var name by remember { mutableStateOf(initial.name) }
    var schedule by remember { mutableStateOf(initial.schedule) }
    var commandId by remember { mutableStateOf(initial.commandId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeathStrandingColors.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, color = DeathStrandingColors.Gold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HmTextField(value = id, label = "Job ID", onValueChange = { id = it }, enabled = !lockId)
                HmTextField(value = name, label = "Name", onValueChange = { name = it })
                HmTextField(value = schedule, label = "Cron schedule (e.g. 0 * * * *)", onValueChange = { schedule = it })
                HmTextField(value = commandId, label = "Command ID", onValueChange = { commandId = it })
                Surface(color = DeathStrandingColors.Gold.copy(alpha = 0.07f), shape = RoundedCornerShape(8.dp)) {
                    Text("Cron format: minute hour dom month dow", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.TextMuted)
                }
                if (error != null) HmErrorText(error)
            }
        },
        confirmButton = {
            GoldButton(
                text = if (isLoading) "Saving…" else "Save",
                enabled = !isLoading && id.isNotBlank() && name.isNotBlank() && schedule.isNotBlank() && commandId.isNotBlank(),
                onClick = { onSave(JobCreateRequest(id = id.trim(), name = name.trim(), schedule = schedule.trim(), commandId = commandId.trim())) }
            )
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = DeathStrandingColors.TextMuted) } }
    )
}
