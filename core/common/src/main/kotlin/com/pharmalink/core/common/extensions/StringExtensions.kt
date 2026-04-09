package com.pharmalink.core.common.extensions

fun String.trimmedOrEmpty(): String = this?.trim() ?: ""
