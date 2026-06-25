package com.kaval.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EmergencyFlowModelsTest {
    @Test
    fun allSituationClassesAreReachable() {
        val cases = mapOf(
            SituationClass.A to SosAnswers(
                location = SituationLocation.IN_VEHICLE,
                situation = SituationType.WRONG_ROUTE,
                canSpeak = SpeakingAbility.PRETEND_NORMAL,
                canEscape = EscapeAbility.NEED_HELP
            ),
            SituationClass.B to SosAnswers(
                location = SituationLocation.WALKING,
                situation = SituationType.BEING_FOLLOWED,
                canSpeak = SpeakingAbility.CAN_SPEAK,
                canEscape = EscapeAbility.CAN_ESCAPE
            ),
            SituationClass.C to SosAnswers(
                location = SituationLocation.PUBLIC_PLACE,
                situation = SituationType.HARASSMENT,
                canSpeak = SpeakingAbility.CAN_SPEAK,
                canEscape = EscapeAbility.NEED_HELP
            ),
            SituationClass.D to SosAnswers(
                location = SituationLocation.WITH_UNSAFE_PERSON,
                situation = SituationType.THREAT_VIOLENCE,
                canSpeak = SpeakingAbility.CAN_ONLY_TAP,
                canEscape = EscapeAbility.TRAPPED
            ),
            SituationClass.E to SosAnswers(
                location = SituationLocation.HOME,
                situation = SituationType.MEDICAL,
                canSpeak = SpeakingAbility.CAN_SPEAK,
                canEscape = EscapeAbility.NEED_HELP
            ),
            SituationClass.CRITICAL to SosAnswers()
        )

        cases.forEach { (expected, answers) ->
            assertEquals(expected, classifySituation(answers))
        }
    }

    @Test
    fun everyClassProducesUserAdviceAndGuardianInstructions() {
        val answers = listOf(
            SosAnswers(
                location = SituationLocation.IN_VEHICLE,
                situation = SituationType.WRONG_ROUTE,
                canSpeak = SpeakingAbility.PRETEND_NORMAL,
                canEscape = EscapeAbility.NEED_HELP
            ),
            SosAnswers(
                location = SituationLocation.WALKING,
                situation = SituationType.BEING_FOLLOWED,
                canSpeak = SpeakingAbility.CAN_SPEAK,
                canEscape = EscapeAbility.CAN_ESCAPE
            ),
            SosAnswers(
                location = SituationLocation.PUBLIC_PLACE,
                situation = SituationType.HARASSMENT,
                canSpeak = SpeakingAbility.CAN_SPEAK,
                canEscape = EscapeAbility.NEED_HELP
            ),
            SosAnswers(
                location = SituationLocation.WITH_UNSAFE_PERSON,
                situation = SituationType.THREAT_VIOLENCE,
                canSpeak = SpeakingAbility.CAN_ONLY_TAP,
                canEscape = EscapeAbility.TRAPPED
            ),
            SosAnswers(
                location = SituationLocation.HOME,
                situation = SituationType.MEDICAL,
                canSpeak = SpeakingAbility.CAN_SPEAK,
                canEscape = EscapeAbility.NEED_HELP
            )
        )

        answers.map(::analyzeSituation).forEach { analysis ->
            assert(analysis.userAdvice.size >= 3)
            assert(analysis.guardianInstruction.isNotBlank())
            assert(analysis.answerSummary.isNotBlank())
        }
    }
}
