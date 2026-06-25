package com.kaval.app.domain.model

enum class EmergencyFlowStage {
    RESPONSE_CHECK,
    CRITICAL,
    QUESTIONS,
    SUGGESTIONS
}

enum class SituationClass { A, B, C, D, E, CRITICAL }

enum class SituationLocation {
    IN_VEHICLE,
    WALKING,
    PUBLIC_PLACE,
    HOME,
    WITH_UNSAFE_PERSON,
    UNKNOWN
}

enum class SituationType {
    BEING_FOLLOWED,
    WRONG_ROUTE,
    HARASSMENT,
    THREAT_VIOLENCE,
    STRANDED,
    MEDICAL,
    JUST_UNSAFE
}

enum class SpeakingAbility {
    CAN_SPEAK,
    PRETEND_NORMAL,
    CANNOT_SPEAK,
    CAN_ONLY_TAP
}

enum class EscapeAbility {
    CAN_ESCAPE,
    NEED_HELP,
    TRAPPED,
    DONT_KNOW
}

data class SosAnswers(
    val location: SituationLocation? = null,
    val situation: SituationType? = null,
    val canSpeak: SpeakingAbility? = null,
    val canEscape: EscapeAbility? = null
)

data class SituationAnalysis(
    val situationClass: SituationClass,
    val title: String,
    val userAdvice: List<String>,
    val guardianInstruction: String,
    val answerSummary: String
)

fun classifySituation(answers: SosAnswers): SituationClass {
    val silenced = answers.canSpeak == SpeakingAbility.CAN_ONLY_TAP ||
        answers.canSpeak == SpeakingAbility.CANNOT_SPEAK
    val trapped = answers.canEscape == EscapeAbility.TRAPPED ||
        answers.canEscape == EscapeAbility.DONT_KNOW

    if (silenced && trapped) return SituationClass.D
    if (answers.situation == SituationType.MEDICAL ||
        answers.situation == SituationType.STRANDED
    ) return SituationClass.E
    if (answers.location == SituationLocation.IN_VEHICLE &&
        (answers.situation == SituationType.WRONG_ROUTE ||
            answers.situation == SituationType.HARASSMENT)
    ) return SituationClass.A
    if (answers.location == SituationLocation.WALKING &&
        answers.situation == SituationType.BEING_FOLLOWED
    ) return SituationClass.B
    if (answers.situation == SituationType.HARASSMENT ||
        answers.situation == SituationType.JUST_UNSAFE ||
        answers.location == SituationLocation.WITH_UNSAFE_PERSON
    ) return SituationClass.C
    if (answers.situation == SituationType.THREAT_VIOLENCE &&
        answers.canEscape != EscapeAbility.CAN_ESCAPE
    ) return SituationClass.D
    if (answers.location == null && answers.situation == null) return SituationClass.CRITICAL
    return SituationClass.C
}

fun analyzeSituation(answers: SosAnswers): SituationAnalysis {
    val situationClass = classifySituation(answers)
    val title = when (situationClass) {
        SituationClass.A -> "Cab or route risk"
        SituationClass.B -> "Being followed"
        SituationClass.C -> "Harassment or unsafe interaction"
        SituationClass.D -> "Trapped or unable to speak"
        SituationClass.E -> "Medical issue or stranded"
        SituationClass.CRITICAL -> "Unable to assess safely"
    }
    val advice = when (situationClass) {
        SituationClass.A -> listOf(
            "Do not confront the driver while the vehicle is moving.",
            "Ask to stop at a busy, well-lit public place.",
            "Keep your guardian watching your location and call them when safe."
        )
        SituationClass.B -> listOf(
            "Do not go directly home or to an isolated place.",
            "Move toward people, bright light, security, or an open shop.",
            "Call a guardian and describe landmarks without confronting the person."
        )
        SituationClass.C -> listOf(
            "Create distance and stay close to other people.",
            "Use a short exit excuse or request an immediate safety call.",
            "Escalate immediately if the person blocks you, follows you, or threatens you."
        )
        SituationClass.D -> listOf(
            "Keep the phone discreet and use tap-only controls.",
            "Resend your location and request police help.",
            "Use the loud alarm only when it will not increase immediate danger."
        )
        SituationClass.E -> listOf(
            "Call 108 for an ambulance or 112 for unified emergency help.",
            "Show your medical note to a nearby person or responder.",
            "Stay where responders can find you unless the location itself is unsafe."
        )
        SituationClass.CRITICAL -> listOf(
            "Kaval could not safely classify the situation.",
            "Keep location sharing and recording active.",
            "Contact 112 or a trusted guardian as soon as possible."
        )
    }
    val guardianInstruction = when (situationClass) {
        SituationClass.A -> "Route or cab risk detected. Call now, monitor location, and be ready to contact police."
        SituationClass.B -> "User may be followed. Stay on the phone, monitor movement, and guide them to a public place."
        SituationClass.C -> "Harassment or unsafe interaction detected. Call now and help create a safe exit."
        SituationClass.D -> "User may be trapped and unable to speak. Do not repeatedly call. Monitor location and consider police immediately."
        SituationClass.E -> "Medical or stranded situation detected. Call the user and coordinate 108 or 112."
        SituationClass.CRITICAL -> "Kaval could not classify the emergency. Treat it as high risk and call immediately."
    }
    val answerSummary = listOfNotNull(
        answers.location?.name?.replace('_', ' ')?.lowercase()?.let { "place=$it" },
        answers.situation?.name?.replace('_', ' ')?.lowercase()?.let { "risk=$it" },
        answers.canSpeak?.name?.replace('_', ' ')?.lowercase()?.let { "speech=$it" },
        answers.canEscape?.name?.replace('_', ' ')?.lowercase()?.let { "escape=$it" }
    ).joinToString(", ").ifBlank { "No reliable answers received" }

    return SituationAnalysis(
        situationClass = situationClass,
        title = title,
        userAdvice = advice,
        guardianInstruction = guardianInstruction,
        answerSummary = answerSummary
    )
}
