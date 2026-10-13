package com.eventom.language

import java.util.Locale

actual fun deviceLanguageCode(): String = Locale.getDefault().language