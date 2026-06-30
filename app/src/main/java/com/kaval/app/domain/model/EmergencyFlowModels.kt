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
    val calmPrompt: String,
    val primaryAction: String,
    val userAdvice: List<String>,
    val avoidAdvice: List<String>,
    val guardianInstruction: String,
    val guardianBrief: String,
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
    val calmPrompt = when (situationClass) {
        SituationClass.A -> "Breathe slowly. Your location is being watched. Focus on getting to a public stop."
        SituationClass.B -> "Do not panic or run unless you must. Move steadily toward people and light."
        SituationClass.C -> "You do not need to explain yourself. Create distance and choose the safest exit."
        SituationClass.D -> "Stay quiet. Use only tap controls. Help is being updated without needing your voice."
        SituationClass.E -> "Stay still if movement makes things worse. Keep the screen visible for helpers."
        SituationClass.CRITICAL -> "Keep the phone with you. Kaval is treating this as high risk."
    }
    val primaryAction = when (situationClass) {
        SituationClass.A -> "Ask to stop at the nearest busy, well-lit place."
        SituationClass.B -> "Enter a shop, security desk, hospital, station, or crowded area."
        SituationClass.C -> "Move beside other people and use an exit excuse."
        SituationClass.D -> "Keep silent and send police/location updates by tapping."
        SituationClass.E -> "Call 108 or 112 and show your medical note."
        SituationClass.CRITICAL -> "Call 112 or keep emergency sharing active."
    }
    val advice = when (situationClass) {
        SituationClass.A -> listOf(
            "Share the vehicle number or visible landmark if you can do it discreetly.",
            "Keep the call short and factual: where you are, direction of travel, and what changed.",
            "If the driver refuses to stop, prepare to call 112 and keep live tracking active."
        )
        SituationClass.B -> listOf(
            "Cross the road or change direction once to confirm whether they are following.",
            "Call a guardian and describe landmarks without confronting the person.",
            "Ask a shopkeeper, guard, family group, or police desk to let you wait there."
        )
        SituationClass.C -> listOf(
            "Use a short sentence: \"I have to leave now\" or \"Someone is waiting for me.\"",
            "Keep your body angled toward an exit and avoid moving into private spaces.",
            "Escalate immediately if the person blocks you, follows you, or threatens you."
        )
        SituationClass.D -> listOf(
            "Keep the phone discreet and avoid visible typing if that increases risk.",
            "Use location resend or police request instead of voice calls.",
            "Use the loud alarm only when it will not increase immediate danger."
        )
        SituationClass.E -> listOf(
            "Tell the nearest person: \"I need medical help. Please call 108 or 112.\"",
            "Show your medical note to a nearby person or responder.",
            "Stay where responders can find you unless the location itself is unsafe."
        )
        SituationClass.CRITICAL -> listOf(
            "Kaval could not safely classify the situation.",
            "Keep location sharing and recording active.",
            "Contact 112 or a trusted guardian as soon as possible."
        )
    }
    val avoidAdvice = when (situationClass) {
        SituationClass.A -> listOf(
            "Do not accuse or argue while the vehicle is moving.",
            "Do not get dropped in an isolated shortcut."
        )
        SituationClass.B -> listOf(
            "Do not go home directly.",
            "Do not enter a lift, stairwell, empty lane, or parked vehicle."
        )
        SituationClass.C -> listOf(
            "Do not stay to be polite.",
            "Do not move somewhere private to continue the conversation."
        )
        SituationClass.D -> listOf(
            "Do not trigger sound if silence is safer.",
            "Do not repeatedly answer calls if you cannot speak safely."
        )
        SituationClass.E -> listOf(
            "Do not drive yourself if dizzy, injured, or disoriented.",
            "Do not leave the shared location unless the spot is unsafe."
        )
        SituationClass.CRITICAL -> listOf(
            "Do not turn off location sharing.",
            "Do not wait if you can call emergency services safely."
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
    val guardianBrief = when (situationClass) {
        SituationClass.A -> "Guardian: verify route change, keep the user talking only if safe, save vehicle details, and call 112 if the vehicle will not stop."
        SituationClass.B -> "Guardian: stay on call, guide the user to a public staffed place, note landmarks, and avoid asking them to go home."
        SituationClass.C -> "Guardian: create an exit reason, call immediately if requested, and escalate if the unsafe person follows or blocks them."
        SituationClass.D -> "Guardian: assume the user may be watched. Avoid repeated calls, monitor live location, and contact police if movement looks unsafe."
        SituationClass.E -> "Guardian: coordinate 108 or 112, keep the user still if medically safer, and share medical notes with responders."
        SituationClass.CRITICAL -> "Guardian: no reliable answers received. Treat as high risk, call once, monitor live location, and contact 112 if no response."
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
        calmPrompt = calmPrompt,
        primaryAction = primaryAction,
        userAdvice = advice,
        avoidAdvice = avoidAdvice,
        guardianInstruction = guardianInstruction,
        guardianBrief = guardianBrief,
        answerSummary = answerSummary
    )
}
