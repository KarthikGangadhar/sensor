package io.geeteshk.sensor.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun Context.openAppDetails() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    intent.data = Uri.parse("package:$packageName")
    intent.addCategory(Intent.CATEGORY_DEFAULT)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    startActivity(intent)
}