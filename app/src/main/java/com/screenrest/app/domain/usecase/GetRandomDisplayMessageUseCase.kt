package com.screenrest.app.domain.usecase

import com.screenrest.app.data.repository.AyahRepository
import com.screenrest.app.data.repository.CustomMessageRepository
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.DisplayMessage
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetRandomDisplayMessageUseCase @Inject constructor(
    private val customMessageRepository: CustomMessageRepository,
    private val ayahRepository: AyahRepository,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        val islamicReminders = listOf(
            "Allah is with me, Allah is watching me.",
            "Allah is a witness over what I do.",
            "I will stand before Allah and He will question me about how I spent my time.",
            "Every moment wasted is a moment I can never get back. Use it wisely for Allah's sake.",
            "The Prophet (PBUH) said: Take advantage of five before five: your youth before your old age, your health before your illness, your wealth before your poverty, your free time before your busyness, and your life before your death.",
            "Am I doing something that would please Allah right now?",
            "My eyes, my time, and my body are all trusts from Allah. I must guard them.",
            "Allah sees what is on my screen. Would I be comfortable if others saw it too?",
            "Time is my most valuable asset. Once gone, it never returns.",
            "The best of deeds are those done consistently, even if small. Let me take this break to reset.",
            "Wasting time is a sign of ingratitude. Let me be grateful and use it wisely.",
            "Allah does not burden a soul beyond that it can bear. I can step away from the screen."
        )
    }

    suspend operator fun invoke(): DisplayMessage {
        val customMessages = customMessageRepository.getAllMessages().first()
        val breakConfig = settingsRepository.breakConfig.first()
        val quranEnabled = breakConfig.quranMessagesEnabled
        val islamicEnabled = breakConfig.islamicRemindersEnabled
        
        // Build pool of enabled source types
        val sourceTypes = mutableListOf<String>()
        
        if (quranEnabled) sourceTypes.add("quran")
        if (islamicEnabled) sourceTypes.add("islamic")
        if (customMessages.isNotEmpty()) sourceTypes.add("custom")
        
        if (sourceTypes.isEmpty()) {
            return DisplayMessage.Custom("Take a moment to rest your eyes and reflect.")
        }
        
        return when (sourceTypes.random()) {
            "quran" -> {
                val ayahResult = ayahRepository.getRandomAyah()
                ayahResult.fold(
                    onSuccess = { ayah -> DisplayMessage.QuranAyah(ayah) },
                    onFailure = { DisplayMessage.Custom("Take a moment to rest your eyes and reflect.") }
                )
            }
            "islamic" -> DisplayMessage.IslamicReminder(islamicReminders.random())
            "custom" -> DisplayMessage.Custom(customMessages.random().text)
            else -> DisplayMessage.Custom("Take a moment to rest your eyes and reflect.")
        }
    }
}
