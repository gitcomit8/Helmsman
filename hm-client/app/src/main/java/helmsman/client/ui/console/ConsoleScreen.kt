package helmsman.client.ui.console

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import helmsman.client.data.local.AppDatabase
import helmsman.client.ui.theme.DeathStrandingColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    commandId: String,
    db: AppDatabase,
    onBack: () -> Unit
) {
    val viewModel: ConsoleViewModel = viewModel(
        factory = ConsoleViewModel.Factory(db)
    )
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val exitCode by viewModel.exitCode.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.startStream(commandId)
    }

    // Auto-scroll to bottom
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Live Console",
                            fontWeight = FontWeight.Bold,
                            color = DeathStrandingColors.Gold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            commandId,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = DeathStrandingColors.TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DeathStrandingColors.Gold)
                    }
                },
                actions = {
                    if (isRunning) {
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(Icons.Default.Stop, "Stop", tint = DeathStrandingColors.ErrorRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeathStrandingColors.DeepNavy
                )
            )
        },
        bottomBar = {
            if (exitCode != null) {
                Surface(
                    color = if (exitCode == 0)
                        DeathStrandingColors.SuccessGreen.copy(alpha = 0.15f)
                    else
                        DeathStrandingColors.ErrorRed.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (exitCode == 0) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (exitCode == 0)
                                DeathStrandingColors.SuccessGreen
                            else
                                DeathStrandingColors.ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Process exited with code $exitCode",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (exitCode == 0)
                                DeathStrandingColors.SuccessGreen
                            else
                                DeathStrandingColors.ErrorRed
                        )
                    }
                }
            } else if (isRunning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = DeathStrandingColors.Gold,
                    trackColor = DeathStrandingColors.Border
                )
            }
        },
        containerColor = androidx.compose.ui.graphics.Color(0xFF000000)
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(lines, key = null) { line ->
                ConsoleLineRow(line)
            }
        }
    }
}

@Composable
private fun ConsoleLineRow(line: ConsoleLine) {
    val (textColor, prefix) = when (line.stream) {
        "stdout" -> DeathStrandingColors.TextPrimary to null
        "stderr" -> DeathStrandingColors.ErrorRed to "ERR"
        "exit" -> (if (line.text.contains("code 0"))
            DeathStrandingColors.SuccessGreen
        else
            DeathStrandingColors.ErrorRed) to "EXIT"
        "error" -> DeathStrandingColors.ErrorRed to "ERR"
        "system" -> DeathStrandingColors.InfoCyan to "SYS"
        else -> DeathStrandingColors.TextPrimary to null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (prefix != null) {
            Surface(
                color = textColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            color = textColor
        )
    }
}

