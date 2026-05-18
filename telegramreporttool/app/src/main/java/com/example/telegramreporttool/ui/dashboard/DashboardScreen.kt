package com.example.telegramreporttool.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.telegramreporttool.data.model.Account
import com.example.telegramreporttool.data.model.Report
import com.example.telegramreporttool.data.model.ReportStatus
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    reports: List<Report>,
    accounts: List<Account>,
    onNewReport: () -> Unit,
    onReportClick: (Report) -> Unit,
    onSwitchAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onLogout: (Long) -> Unit,
    onMenuClick: () -> Unit
) {
    var showAccountSwitcher by remember { mutableStateOf(false) }
    val activeAccount = accounts.find { it.isActive }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                },
                title = { 
                    Column(modifier = Modifier.clickable { showAccountSwitcher = true }) {
                        Text("Report Dashboard")
                        activeAccount?.let {
                            Text(
                                text = "${it.firstName} (${it.phoneNumber})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAccountSwitcher = true }) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = "Accounts")
                    }
                    IconButton(onClick = { /* Refresh logic */ }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onNewReport,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New Report")
            }
        }
    ) { padding ->
        if (showAccountSwitcher) {
            AccountSwitcherDialog(
                accounts = accounts,
                onSwitch = { 
                    onSwitchAccount(it)
                    showAccountSwitcher = false
                },
                onAdd = {
                    onAddAccount()
                    showAccountSwitcher = false
                },
                onLogout = onLogout,
                onDismiss = { showAccountSwitcher = false }
            )
        }

        if (reports.isEmpty()) {
            EmptyDashboard(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports) { report ->
                    ReportItem(report = report, onClick = { onReportClick(report) })
                }
            }
        }
    }
}

@Composable
fun AccountSwitcherDialog(
    accounts: List<Account>,
    onSwitch: (Long) -> Unit,
    onAdd: () -> Unit,
    onLogout: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Switch Account") },
        text = {
            LazyColumn {
                items(accounts) { account ->
                    ListItem(
                        headlineContent = { Text("${account.firstName} ${account.lastName}") },
                        supportingContent = { Text(account.phoneNumber) },
                        leadingContent = {
                            Icon(
                                if (account.isActive) Icons.Rounded.CheckCircle else Icons.Rounded.Person,
                                contentDescription = null,
                                tint = if (account.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onLogout(account.userId) }) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Logout")
                            }
                        },
                        modifier = Modifier.clickable { onSwitch(account.userId) }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Add Account") },
                        leadingContent = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        modifier = Modifier.clickable { onAdd() }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ReportDetailScreen(report: Report) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 32.dp)
    ) {
        Text(
            text = "Report Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(label = "Target", value = report.target.value)
                DetailRow(label = "Type", value = report.target.type.name)
                DetailRow(label = "Category", value = report.template.category)
                DetailRow(label = "Description", value = report.template.description)
                DetailRow(label = "Status", value = report.status.name)
                DetailRow(label = "Timestamp", value = formatDate(report.timestamp))
            }
        }
        
        if (report.evidence.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Evidence",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            report.evidence.forEach { evidence ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (evidence.startsWith("content://") || evidence.startsWith("file://") || evidence.startsWith("http")) {
                            AsyncImage(
                                model = evidence,
                                contentDescription = "Evidence Image",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                            )
                        } else {
                            Text(text = evidence)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "$label: ", fontWeight = FontWeight.Bold)
        Text(text = value)
    }
}

@Composable
fun EmptyDashboard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Assessment,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No reports yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            "Tap '+' to start reporting violations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun ReportItem(report: Report, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIcon(status = report.status)
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.target.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = report.template.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = formatDate(report.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            StatusBadge(status = report.status)
        }
    }
}

@Composable
fun StatusIcon(status: ReportStatus) {
    val (icon, color) = when (status) {
        ReportStatus.PENDING -> Icons.Rounded.Pending to MaterialTheme.colorScheme.secondary
        ReportStatus.SENT -> Icons.Rounded.CheckCircle to MaterialTheme.colorScheme.primary
        ReportStatus.FAILED -> Icons.Rounded.Error to MaterialTheme.colorScheme.error
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(8.dp).size(24.dp),
            tint = color
        )
    }
}

@Composable
fun StatusBadge(status: ReportStatus) {
    val color = when (status) {
        ReportStatus.PENDING -> MaterialTheme.colorScheme.secondary
        ReportStatus.SENT -> MaterialTheme.colorScheme.primary
        ReportStatus.FAILED -> MaterialTheme.colorScheme.error
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
