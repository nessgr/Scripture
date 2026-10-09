package app.scripture.ui.screens

/**
 * Opens a web address in the platform's browser. Returns false when the
 * platform has nothing to open it with (e.g. no browser installed), so the
 * caller can offer the raw link to copy instead.
 */
expect fun openUrl(url: String): Boolean