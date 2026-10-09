package app.scripture.ui.screens

import android.content.Intent
import android.net.Uri
import app.scripture.data.AndroidApp

/** Android: a VIEW intent against the user's chosen browser. */
actual fun openUrl(url: String): Boolean = runCatching {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    AndroidApp.context.startActivity(intent)
}.isSuccess