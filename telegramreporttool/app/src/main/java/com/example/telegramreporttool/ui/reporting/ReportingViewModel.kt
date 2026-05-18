package com.example.telegramreporttool.ui.reporting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telegramreporttool.data.model.*
import com.example.telegramreporttool.data.repository.ReportRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ReportingUiState(
    val currentStep: ReportingStep = ReportingStep.TARGET,
    val targetValue: String = "",
    val targetType: TargetType = TargetType.HANDLE,
    val targetError: String? = null,
    val templates: List<Template> = emptyList(),
    val selectedTemplate: Template? = null,
    val evidence: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
    val submissionSuccess: Boolean? = null
)

enum class ReportingStep {
    TARGET, TEMPLATE, EVIDENCE_REVIEW
}

class ReportingViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportingUiState())
    val uiState: StateFlow<ReportingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allTemplates.collect { templates ->
                _uiState.update { it.copy(templates = templates) }
                if (templates.isEmpty()) {
                    repository.populateTemplates(getDefaultTemplates())
                }
            }
        }
    }

    fun onTargetValueChange(value: String) {
        _uiState.update { it.copy(targetValue = value, targetError = null) }
    }

    fun onTargetTypeChange(type: TargetType) {
        _uiState.update { it.copy(targetType = type) }
    }

    fun nextStep() {
        val currentState = _uiState.value
        when (currentState.currentStep) {
            ReportingStep.TARGET -> {
                if (validateTarget(currentState.targetValue)) {
                    _uiState.update { it.copy(currentStep = ReportingStep.TEMPLATE) }
                } else {
                    _uiState.update { it.copy(targetError = "Invalid Telegram handle or link") }
                }
            }
            ReportingStep.TEMPLATE -> {
                if (currentState.selectedTemplate != null) {
                    _uiState.update { it.copy(currentStep = ReportingStep.EVIDENCE_REVIEW) }
                }
            }
            ReportingStep.EVIDENCE_REVIEW -> submitReport()
        }
    }

    fun previousStep() {
        _uiState.update {
            val prevStep = when (it.currentStep) {
                ReportingStep.TARGET -> ReportingStep.TARGET
                ReportingStep.TEMPLATE -> ReportingStep.TARGET
                ReportingStep.EVIDENCE_REVIEW -> ReportingStep.TEMPLATE
            }
            it.copy(currentStep = prevStep)
        }
    }

    fun selectTemplate(template: Template) {
        _uiState.update { it.copy(selectedTemplate = template) }
    }

    fun addEvidence(snippet: String) {
        if (snippet.isNotBlank()) {
            _uiState.update { it.copy(evidence = it.evidence + snippet) }
        }
    }

    fun removeEvidence(index: Int) {
        _uiState.update { 
            val newList = it.evidence.toMutableList()
            newList.removeAt(index)
            it.copy(evidence = newList)
        }
    }

    private fun validateTarget(value: String): Boolean {
        if (value.isBlank()) return false
        
        // Handle: @username (5-32 chars, alphanumeric and underscores)
        val handleRegex = Regex("^@[a-zA-Z0-9_]{5,32}$")
        
        // Link: t.me/username or telegram.me/username
        val linkRegex = Regex("^(https?://)?(t\\.me|telegram\\.me)/[a-zA-Z0-9_]{5,32}$")
        
        // Message/Post: t.me/username/123 or t.me/c/123456789/123
        val messageRegex = Regex("^(https?://)?(t\\.me|telegram\\.me)/([a-zA-Z0-9_]{5,32}|c/\\d+)/\\d+$")
        
        return handleRegex.matches(value) || linkRegex.matches(value) || messageRegex.matches(value)
    }

    private fun submitReport() {
        val state = _uiState.value
        val report = Report(
            id = UUID.randomUUID().toString(),
            accountId = 0L, // Placeholder, filled by repository
            target = Target(state.targetType, state.targetValue),
            template = state.selectedTemplate!!,
            evidence = state.evidence,
            status = ReportStatus.PENDING,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val success = repository.submitReport(report)
            _uiState.update { it.copy(isSubmitting = false, submissionSuccess = success) }
        }
    }

    private fun getDefaultTemplates() = listOf(
        Template("1", "Spam & Scam", "Unsolicited promotional messages, phishing, or financial deception."),
        Template("2", "Violence", "Promotion of violence, physical harm, or illegal acts."),
        Template("3", "Pornography", "Sexually explicit content or non-consensual imagery."),
        Template("4", "Child Abuse", "Content involving the exploitation or harm of minors."),
        Template("5", "Copyright", "Infringement of intellectual property or unauthorized sharing."),
        Template("6", "Impersonation", "Accounts pretending to be someone else to deceive users."),
        Template("7", "Illegal Content", "Sale of illegal goods, drugs, or regulated substances."),
        Template("8", "Harassment", "Threats, bullying, stalking, or unwanted personal attacks."),
        Template("9", "Terrorism", "Promotion of terrorist organizations, recruitment, or extremist content.")
    )
}
