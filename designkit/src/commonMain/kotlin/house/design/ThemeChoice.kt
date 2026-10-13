package house.design

/**
 * The one UI preference every house app shares: follow the system, or force
 * light/dark. SYSTEM keeps both platforms matching whatever the user set
 * globally. Persistence is each app's own concern (Scripture keeps its
 * ThemeStore).
 */
enum class ThemeChoice { SYSTEM, LIGHT, DARK }