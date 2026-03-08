package helmsman.client.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import helmsman.client.data.model.ServerStatus
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import helmsman.client.ui.components.LoadingIndicator
import helmsman.client.ui.components.SectionHeader
import helmsman.client.ui.components.StatusDot
import helmsman.client.ui.navigation.Routes
import helmsman.client.ui.theme.DeathStrandingColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(api: HelmsmanApi, navController: NavController) {
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(api))
    val daemonStatus by viewModel.daemonStatus.collectAsStateWithLifecycle()
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val serverStatuses by viewModel.serverStatuses.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HELMSMAN", fontWeight = FontWeight.ExtraBold, letterSpacing = 5.sp, color = DeathStrandingColors.Gold, fontSize = 18.sp)
                        Text("Remote Command Center", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.TextMuted, letterSpacing = 1.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = DeathStrandingColors.Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeathStrandingColors.DeepNavy)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = DeathStrandingColors.SurfaceCard, tonalElevation = 0.dp) {
                val tabs = listOf(
                    Triple("Home", Icons.Default.Dashboard, Routes.HOME),
                    Triple("Commands", Icons.Default.Terminal, Routes.COMMANDS),
                    Triple("Jobs", Icons.Default.Schedule, Routes.JOBS),
                    Triple("Servers", Icons.Default.Dns, Routes.SERVERS)
                )
                tabs.forEachIndexed { i, (title, icon, route) ->
                    NavigationBarItem(
                        icon = { Icon(icon, title, modifier = Modifier.size(22.dp)) },
                        label = { Text(title, fontSize = 10.sp, letterSpacing = 0.5.sp) },
                        selected = selectedTab == i,
                        onClick = { selectedTab = i; if (i != 0) navController.navigate(route) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeathStrandingColors.Gold,
                            selectedTextColor = DeathStrandingColors.Gold,
                            unselectedIconColor = DeathStrandingColors.TextMuted,
                            unselectedTextColor = DeathStrandingColors.TextMuted,
                            indicatorColor = DeathStrandingColors.Gold.copy(alpha = 0.12f)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero status card
            DaemonStatusHero(daemonStatus)

            // Quick nav
            SectionHeader(title = "NAVIGATION")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple(Icons.Default.Terminal, "Commands", Routes.COMMANDS),
                    Triple(Icons.Default.Schedule, "Jobs", Routes.JOBS),
                    Triple(Icons.Default.Dns, "Servers", Routes.SERVERS)
                ).forEach { (icon, label, route) ->
                    QuickCard(icon, label, Modifier.weight(1f)) { navController.navigate(route) }
                }
            }

            // Server health
            if (servers is UiState.Loading) {
                LoadingIndicator()
            } else if (servers is UiState.Success) {
                val list = (servers as UiState.Success).data
                if (list.isNotEmpty()) {
                    SectionHeader(title = "SERVER HEALTH")
                    list.forEach { server ->
                        ServerHealthRow(
                            name = server.name,
                            host = server.host,
                            status = serverStatuses[server.id]
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DaemonStatusHero(daemonStatus: UiState<String>) {
    val isOnline = daemonStatus is UiState.Success
    val isLoading = daemonStatus is UiState.Loading
    val glowColor = if (isOnline) DeathStrandingColors.SuccessGreen else DeathStrandingColors.ErrorRed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DeathStrandingColors.SurfaceCard)
            .border(1.dp, DeathStrandingColors.Border, RoundedCornerShape(20.dp))
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(glowColor.copy(alpha = if (isOnline) 0.07f else 0.04f), Color.Transparent),
                        center = Offset(0f, size.height),
                        radius = size.maxDimension
                    )
                )
            }
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(glowColor.copy(alpha = 0.12f))
                        .border(1.dp, glowColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = DeathStrandingColors.Gold, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Icon(
                            if (isOnline) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            null,
                            tint = glowColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text("DAEMON STATUS", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.TextMuted, letterSpacing = 2.sp)
                    Text(
                        when (daemonStatus) {
                            is UiState.Success -> "Operational"
                            is UiState.Loading -> "Checking..."
                            is UiState.Error -> "Unreachable"
                            else -> "Unknown"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (isOnline) DeathStrandingColors.SuccessGreen else DeathStrandingColors.TextPrimary
                    )
                }
                Spacer(Modifier.weight(1f))
                StatusDot(isOnline = isOnline && !isLoading, size = 12.dp)
            }

            if (daemonStatus is UiState.Error) {
                Surface(color = DeathStrandingColors.ErrorRed.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        (daemonStatus as UiState.Error).message,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = DeathStrandingColors.ErrorRed
                    )
                }
            }

            // Decorative divider with dots
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DeathStrandingColors.Border)
                Text("http://100.111.67.98:3000", style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.TextMuted.copy(alpha = 0.6f))
                HorizontalDivider(modifier = Modifier.weight(1f), color = DeathStrandingColors.Border)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCard(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeathStrandingColors.SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DeathStrandingColors.Border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(DeathStrandingColors.Gold.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, label, tint = DeathStrandingColors.Gold, modifier = Modifier.size(20.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = DeathStrandingColors.TextSecondary)
        }
    }
}

@Composable
private fun ServerHealthRow(name: String, host: String, status: ServerStatus?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DeathStrandingColors.SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DeathStrandingColors.Border)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, color = DeathStrandingColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(host, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = DeathStrandingColors.TextMuted)
                }
                if (status != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusDot(isOnline = status.pingMs != null, size = 8.dp)
                        Text(
                            status.pingMs?.let { "%.1f ms".format(it) } ?: "Offline",
                            fontWeight = FontWeight.Bold,
                            color = if (status.pingMs != null) DeathStrandingColors.SuccessGreen else DeathStrandingColors.ErrorRed,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeathStrandingColors.Gold, strokeWidth = 2.dp)
                }
            }
            if (status != null && status.uname.isNotBlank()) {
                Surface(color = DeathStrandingColors.DeepNavy, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        status.uname,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = DeathStrandingColors.TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
