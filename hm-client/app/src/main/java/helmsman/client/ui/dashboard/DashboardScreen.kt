package helmsman.client.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import helmsman.client.data.model.ServerStatus
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import helmsman.client.ui.components.*
import helmsman.client.ui.navigation.Routes
import helmsman.client.ui.theme.DeathStrandingColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    api: HelmsmanApi,
    navController: NavController
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(api)
    )
    val daemonStatus by viewModel.daemonStatus.collectAsStateWithLifecycle()
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val serverStatuses by viewModel.serverStatuses.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem("Dashboard", Icons.Default.Dashboard),
        TabItem("Commands", Icons.Default.Terminal),
        TabItem("Jobs", Icons.Default.Schedule),
        TabItem("Servers", Icons.Default.Dns)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "HELMSMAN",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = DeathStrandingColors.Gold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeathStrandingColors.DeepNavy
                ),
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = DeathStrandingColors.Gold
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeathStrandingColors.SurfaceCard,
                contentColor = DeathStrandingColors.Gold
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            when (index) {
                                1 -> navController.navigate(Routes.COMMANDS)
                                2 -> navController.navigate(Routes.JOBS)
                                3 -> navController.navigate(Routes.SERVERS)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeathStrandingColors.Gold,
                            selectedTextColor = DeathStrandingColors.Gold,
                            unselectedIconColor = DeathStrandingColors.TextMuted,
                            unselectedTextColor = DeathStrandingColors.TextMuted,
                            indicatorColor = DeathStrandingColors.Gold.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        containerColor = DeathStrandingColors.DeepNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Daemon Status Card
            GoldGradientCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusDot(isOnline = daemonStatus is UiState.Success)
                    Column {
                        Text(
                            "Daemon Status",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeathStrandingColors.TextPrimary
                        )
                        Text(
                            when (daemonStatus) {
                                is UiState.Success -> "Operational"
                                is UiState.Loading -> "Checking..."
                                is UiState.Error -> (daemonStatus as UiState.Error).message
                                else -> "Unknown"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (daemonStatus) {
                                is UiState.Success -> DeathStrandingColors.SuccessGreen
                                is UiState.Error -> DeathStrandingColors.ErrorRed
                                else -> DeathStrandingColors.TextMuted
                            }
                        )
                    }
                }
            }

            // Quick Actions
            SectionHeader(title = "Quick Actions")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.Terminal,
                    label = "Commands",
                    count = null,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.COMMANDS) }
                )
                QuickActionCard(
                    icon = Icons.Default.Schedule,
                    label = "Jobs",
                    count = null,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.JOBS) }
                )
                QuickActionCard(
                    icon = Icons.Default.Dns,
                    label = "Servers",
                    count = null,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.SERVERS) }
                )
            }

            // Server Status Overview
            if (servers is UiState.Success) {
                val serverList = (servers as UiState.Success).data
                if (serverList.isNotEmpty()) {
                    SectionHeader(title = "Server Health")
                    serverList.forEach { server ->
                        val status = serverStatuses[server.id]
                        ServerHealthCard(
                            serverName = server.name,
                            host = server.host,
                            status = status
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    count: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DeathStrandingColors.SurfaceCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DeathStrandingColors.Gold,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = DeathStrandingColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ServerHealthCard(
    serverName: String,
    host: String,
    status: ServerStatus?
) {
    GoldGradientCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = serverName,
                    style = MaterialTheme.typography.titleSmall,
                    color = DeathStrandingColors.TextPrimary
                )
                Text(
                    text = host,
                    style = MaterialTheme.typography.bodySmall,
                    color = DeathStrandingColors.TextMuted
                )
            }

            if (status != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusDot(isOnline = status.pingMs != null)
                        Text(
                            text = status.pingMs?.let { "%.1f ms".format(it) } ?: "Offline",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (status.pingMs != null)
                                DeathStrandingColors.SuccessGreen
                            else
                                DeathStrandingColors.ErrorRed
                        )
                    }
                    if (status.pingError != null) {
                        Text(
                            text = status.pingError,
                            style = MaterialTheme.typography.bodySmall,
                            color = DeathStrandingColors.ErrorRed
                        )
                    }
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = DeathStrandingColors.Gold,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

private data class TabItem(val title: String, val icon: ImageVector)
