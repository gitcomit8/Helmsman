package helmsman.client.ui.jobs

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import helmsman.client.data.model.Job
import helmsman.client.data.model.JobCreateRequest
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import helmsman.client.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(api: HelmsmanApi, contentPadding: PaddingValues = PaddingValues()) {
    val vm: JobsViewModel = viewModel(factory = JobsViewModel.Factory(api))
    val jobs        by vm.jobs.collectAsStateWithLifecycle()
    val createState by vm.createState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadJobs() }

    var editing by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(createState) {
        if (createState is UiState.Success) { editing = null; vm.resetCreateState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jobs", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { vm.loadJobs() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { editing = Job("", "", "", "", nextRun = "") }) {
                        Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        when (val s = jobs) {
            is UiState.Loading -> LoadingIndicator(Modifier.padding(inner).padding(contentPadding))
            is UiState.Error   -> EmptyState(Icons.Default.Warning, s.message, Modifier.padding(inner).padding(contentPadding))
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyState(Icons.Default.Schedule, "No scheduled jobs yet", Modifier.padding(inner).padding(contentPadding))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(inner).padding(contentPadding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(s.data, key = { it.id }) { job ->
                            JobCard(job = job, onEdit = { editing = job }, onDelete = { vm.deleteJob(job.id) })
                        }
                    }
                }
            }
            else -> {}
        }
    }

    editing?.let { job ->
        JobDialog(
            initial   = job,
            isNew     = job.id.isEmpty(),
            isLoading = createState is UiState.Loading,
            error     = (createState as? UiState.Error)?.message,
            onDismiss = { editing = null; vm.resetCreateState() },
            onSave    = { vm.createJob(it) }
        )
    }
}

@Composable
private fun JobCard(job: Job, onEdit: () -> Unit, onDelete: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(job.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(6.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            job.schedule,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag("cmd:${job.commandId}", MaterialTheme.colorScheme.secondary)
                    Tag("id:${job.id}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    job.lastRun?.let {
                        Column {
                            Text("LAST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(it.take(16).replace('T', ' '), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (job.nextRun.isNotBlank()) {
                        Column {
                            Text("NEXT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(job.nextRun.take(16).replace('T', ' '), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
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
    }
}

@Composable
private fun JobDialog(
    initial: Job,
    isNew: Boolean,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (JobCreateRequest) -> Unit
) {
    var id        by remember { mutableStateOf(initial.id) }
    var name      by remember { mutableStateOf(initial.name) }
    var schedule  by remember { mutableStateOf(initial.schedule) }
    var commandId by remember { mutableStateOf(initial.commandId) }

    HmDialog(
        title          = if (isNew) "New job" else "Edit job",
        confirmLabel   = "Save",
        confirmEnabled = id.isNotBlank() && name.isNotBlank() && schedule.isNotBlank() && commandId.isNotBlank(),
        isLoading      = isLoading,
        onDismiss      = onDismiss,
        onConfirm      = { onSave(JobCreateRequest(id.trim(), name.trim(), schedule.trim(), commandId.trim())) }
    ) {
        HmTextField(id, "Job ID", { id = it }, enabled = isNew)
        HmTextField(name, "Name", { name = it })
        HmTextField(schedule, "Cron expression (e.g. 0 * * * *)", { schedule = it })
        HmTextField(commandId, "Command ID to run", { commandId = it })
        if (error != null) HmErrorText(error)
    }
}
