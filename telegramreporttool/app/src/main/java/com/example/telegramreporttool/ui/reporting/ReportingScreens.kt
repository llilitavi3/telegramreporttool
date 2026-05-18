package com.example.telegramreporttool.ui.reporting

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.telegramreporttool.data.model.TargetType
import com.example.telegramreporttool.data.model.Template

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingScreen(
    state: ReportingUiState,
    onValueChange: (String) -> Unit,
    onTypeChange: (TargetType) -> Unit,
    onTemplateSelect: (Template) -> Unit,
    onAddEvidence: (String) -> Unit,
    onRemoveEvidence: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Telegram Content") },
                navigationIcon = {
                    if (state.currentStep != ReportingStep.TARGET) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.submissionSuccess != true) {
                BottomAppBar {
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onNext,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        enabled = when (state.currentStep) {
                            ReportingStep.TARGET -> state.targetValue.isNotBlank()
                            ReportingStep.TEMPLATE -> state.selectedTemplate != null
                            ReportingStep.EVIDENCE_REVIEW -> !state.isSubmitting
                        }
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(if (state.currentStep == ReportingStep.EVIDENCE_REVIEW) "Submit Report" else "Next")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            StepIndicator(currentStep = state.currentStep)
            Spacer(Modifier.height(24.dp))

            when (state.currentStep) {
                ReportingStep.TARGET -> TargetProfiler(
                    value = state.targetValue,
                    type = state.targetType,
                    error = state.targetError,
                    onValueChange = onValueChange,
                    onTypeChange = onTypeChange
                )
                ReportingStep.TEMPLATE -> TemplateSelection(
                    templates = state.templates,
                    selected = state.selectedTemplate,
                    onSelect = onTemplateSelect
                )
                ReportingStep.EVIDENCE_REVIEW -> EvidenceReview(
                    evidence = state.evidence,
                    onAdd = onAddEvidence,
                    onRemove = onRemoveEvidence,
                    success = state.submissionSuccess
                )
            }
        }
    }
}

@Composable
fun StepIndicator(currentStep: ReportingStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepItem("Target", currentStep >= ReportingStep.TARGET)
        HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        StepItem("Template", currentStep >= ReportingStep.TEMPLATE)
        HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        StepItem("Review", currentStep >= ReportingStep.EVIDENCE_REVIEW)
    }
}

@Composable
fun StepItem(label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        Icon(
            imageVector = if (active) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = color
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun TargetProfiler(
    value: String,
    type: TargetType,
    error: String?,
    onValueChange: (String) -> Unit,
    onTypeChange: (TargetType) -> Unit
) {
    Column {
        Text("Target Information", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Enter the Telegram handle, link, or message URL you wish to report.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TargetTypeChip(TargetType.HANDLE, type == TargetType.HANDLE) { onTypeChange(TargetType.HANDLE) }
            TargetTypeChip(TargetType.CHANNEL, type == TargetType.CHANNEL) { onTypeChange(TargetType.CHANNEL) }
            TargetTypeChip(TargetType.MESSAGE, type == TargetType.MESSAGE) { onTypeChange(TargetType.MESSAGE) }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Telegram Handle or Link") },
            placeholder = { Text("@username or t.me/...") },
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetTypeChip(type: TargetType, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(type.name) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null
    )
}

@Composable
fun TemplateSelection(
    templates: List<Template>,
    selected: Template?,
    onSelect: (Template) -> Unit
) {
    Column {
        Text("Violation Category", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Select the category that best describes the violation.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates) { template ->
                Card(
                    onClick = { onSelect(template) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected?.id == template.id) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(template.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(template.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun EvidenceReview(
    evidence: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
    success: Boolean?
) {
    var text by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onAdd(it.toString()) }
    }

    if (success != null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val (icon, title, color) = if (success) {
                Triple(Icons.Rounded.CloudDone, "Report Submitted!", MaterialTheme.colorScheme.primary)
            } else {
                Triple(Icons.Rounded.ErrorOutline, "Submission Failed", MaterialTheme.colorScheme.error)
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = color
            )
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(
                if (success) "Thank you for helping keep Telegram safe." 
                else "An error occurred while sending the report. Please check the dashboard."
            )
        }
    } else {
        Column {
            Text("Evidence & Review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Add links, screenshots, or text snippets as evidence.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Add Evidence (Link or Text)") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { 
                        if (text.isNotBlank()) {
                            onAdd(text)
                            text = ""
                        }
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add")
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Screenshot")
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(evidence.size) { index ->
                    ListItem(
                        headlineContent = { Text(evidence[index], maxLines = 1) },
                        trailingContent = {
                            IconButton(onClick = { onRemove(index) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove")
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    )
                }
            }
        }
    }
}
