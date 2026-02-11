package com.screenrest.app.domain.model

sealed class DisplayMessage {
    data class Custom(val text: String) : DisplayMessage()
    data class QuranAyah(val ayah: Ayah) : DisplayMessage()
    data class IslamicReminder(val text: String) : DisplayMessage()
}
