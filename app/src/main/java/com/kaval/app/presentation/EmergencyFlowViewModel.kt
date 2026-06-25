package com.kaval.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaval.app.domain.model.EmergencyFlowStage
import com.kaval.app.domain.model.EscapeAbility
import com.kaval.app.domain.model.SituationAnalysis
import com.kaval.app.domain.model.SituationLocation
import com.kaval.app.domain.model.SituationType
import com.kaval.app.domain.model.SosAnswers
import com.kaval.app.domain.model.SpeakingAbility
import com.kaval.app.domain.model.analyzeSituation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class EmergencyFlowUiState(
    val stage: EmergencyFlowStage = EmergencyFlowStage.RESPONSE_CHECK,
    val responseSeconds: Int = 15,
    val questionIndex: Int = 0,
    val questionSeconds: Int = 10,
    val answers: SosAnswers = SosAnswers(),
    val analysis: SituationAnalysis? = null,
    val stealthActive: Boolean = false,
    val helpCardVisible: Boolean = false,
    val safeConfirmationVisible: Boolean = false,
    val guardianAnalysisRevision: Int = 0,
    val guardianAnalysisSharedRevision: Int = 0,
    val noResponseEscalationPending: Boolean = false
)

class EmergencyFlowViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(EmergencyFlowUiState())
    val state: StateFlow<EmergencyFlowUiState> = mutableState.asStateFlow()
    private var timerJob: Job? = null

    init {
        startResponseTimer()
    }

    fun userResponded() {
        timerJob?.cancel()
        mutableState.update {
            it.copy(
                stage = EmergencyFlowStage.QUESTIONS,
                questionIndex = 0,
                questionSeconds = 10
            )
        }
        startQuestionTimer()
    }

    fun cannotRespond() {
        timerJob?.cancel()
        mutableState.update { it.copy(stage = EmergencyFlowStage.CRITICAL) }
    }

    fun answerLocation(value: SituationLocation) {
        answerAndAdvance(mutableState.value.answers.copy(location = value))
    }

    fun answerSituation(value: SituationType) {
        answerAndAdvance(mutableState.value.answers.copy(situation = value))
    }

    fun answerSpeaking(value: SpeakingAbility) {
        answerAndAdvance(mutableState.value.answers.copy(canSpeak = value))
    }

    fun answerEscape(value: EscapeAbility) {
        answerAndAdvance(mutableState.value.answers.copy(canEscape = value))
    }

    fun submitAnswer(answers: SosAnswers) {
        answerAndAdvance(answers)
    }

    fun restartAssessment() {
        timerJob?.cancel()
        mutableState.update {
            it.copy(
                stage = EmergencyFlowStage.QUESTIONS,
                questionIndex = 0,
                questionSeconds = 10,
                answers = SosAnswers(),
                analysis = null,
                stealthActive = false
            )
        }
        startQuestionTimer()
    }

    fun escalateToClassD() {
        val answers = mutableState.value.answers.copy(
            situation = SituationType.THREAT_VIOLENCE,
            canSpeak = SpeakingAbility.CAN_ONLY_TAP,
            canEscape = EscapeAbility.TRAPPED
        )
        publishAnalysis(answers)
    }

    fun setStealth(enabled: Boolean) = mutableState.update { it.copy(stealthActive = enabled) }
    fun setHelpCardVisible(visible: Boolean) = mutableState.update { it.copy(helpCardVisible = visible) }
    fun setSafeConfirmationVisible(visible: Boolean) =
        mutableState.update { it.copy(safeConfirmationVisible = visible) }

    fun markGuardianAnalysisShared(revision: Int) {
        mutableState.update {
            if (revision <= it.guardianAnalysisSharedRevision) it
            else it.copy(guardianAnalysisSharedRevision = revision)
        }
    }

    fun markNoResponseEscalationShared() {
        mutableState.update { it.copy(noResponseEscalationPending = false) }
    }

    private fun answerAndAdvance(answers: SosAnswers) {
        timerJob?.cancel()
        val current = mutableState.value
        if (current.questionIndex >= 3) {
            publishAnalysis(answers)
        } else {
            mutableState.update {
                it.copy(
                    answers = answers,
                    questionIndex = it.questionIndex + 1,
                    questionSeconds = 10
                )
            }
            startQuestionTimer()
        }
    }

    private fun publishAnalysis(answers: SosAnswers) {
        timerJob?.cancel()
        val analysis = analyzeSituation(answers)
        mutableState.update {
            it.copy(
                stage = if (analysis.situationClass.name == "CRITICAL") {
                    EmergencyFlowStage.CRITICAL
                } else {
                    EmergencyFlowStage.SUGGESTIONS
                },
                answers = answers,
                analysis = analysis,
                guardianAnalysisRevision = it.guardianAnalysisRevision + 1
            )
        }
    }

    private fun startResponseTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && mutableState.value.stage == EmergencyFlowStage.RESPONSE_CHECK) {
                delay(1_000)
                val seconds = mutableState.value.responseSeconds
                if (seconds <= 1) {
                    mutableState.update {
                        it.copy(
                            responseSeconds = 0,
                            stage = EmergencyFlowStage.CRITICAL,
                            noResponseEscalationPending = true
                        )
                    }
                    break
                }
                mutableState.update { it.copy(responseSeconds = seconds - 1) }
            }
        }
    }

    private fun startQuestionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && mutableState.value.stage == EmergencyFlowStage.QUESTIONS) {
                delay(1_000)
                val current = mutableState.value
                if (current.questionSeconds <= 1) {
                    if (current.questionIndex >= 3) {
                        publishAnalysis(current.answers)
                        break
                    }
                    mutableState.update {
                        it.copy(
                            questionIndex = it.questionIndex + 1,
                            questionSeconds = 10
                        )
                    }
                } else {
                    mutableState.update { it.copy(questionSeconds = current.questionSeconds - 1) }
                }
            }
        }
    }
}
