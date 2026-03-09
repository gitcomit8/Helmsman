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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import helmsman.client.data.local.AppDatabase
import helmsman.client.ui.theme.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(commandId: String, db: AppDatabase, onBack: () -> Unit) {
    val vm: ConsoleViewModel = viewModel(factory = ConsoleViewModel.Factory(db))
    val lines     by vm.lines.collectAsStateWithLifecycle()
    val isRunning by vm.isRunning.collectAsStateWithLifecycle()
    val exitCode  by vm.exitCode.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val ext = LocalExtendedColors.current

    LaunchedEffect(Unit) { vm.startStream(commandId) }
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Console", fontWeight = FontWeight.SemiBold)
                        Text(commandId, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.disconnect(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isRunning) {
                        IconButton(onClick = { vm.disconnect() }) {
                            Icon(Icons.Default.Stop, "Stop", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            when {
                exitCode != null -> Surface(
                    color = if (exitCode == 0) ext.success.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (exitCode == 0) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            null, modifier = Modifier.size(16.dp),
                            tint = if (exitCode == 0) ext.success else MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Exited with code $exitCode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (exitCode == 0) ext.success else MaterialTheme.colorScheme.error
                        )
                    }
                }
                isRunning -> LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        },
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(lines) { line ->
                val (color, badge) = when (line.stream) {
                    "stdout" -> MaterialTheme.colorScheme.onBackground to null
                    "stderr" -> MaterialTheme.colorScheme.error to "ERR"
                    "exit"   -> (if (line.text.contains("0")) ext.success else MaterialTheme.colorScheme.error) to "EXIT"
                    "system" -> ext.info to "SYS"
                    else     -> MaterialTheme.colorScheme.onBackground to null
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (badge != null) {
                        Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp)) {
                            Text(badge, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(line.text, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = color, lineHeight = 17.sp)
                }
            }
        }
    }
}
