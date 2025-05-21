package com.github.jcraane.kotlincodesorter.settings

/**
 * Enum representing the different types of sorting rules.
 * The order of the enum values defines the default sorting order.
 */
enum class SortingRuleType(val displayName: String) {
    ABSTRACT_PROPERTY("abstract val/var"),
    PUBLIC_PROPERTY("public val/var"),
    PRIVATE_PROPERTY("private val/var (viewModel at the top of the class)"),
    COMPANION_OBJECT("companion object"),
    INIT_BLOCK("init"),
    OVERRIDE_FUNCTION("override fun"),
    ABSTRACT_FUNCTION("abstract fun"),
    PUBLIC_FUNCTION("public fun"),
    PROTECTED_FUNCTION("protected fun"),
    PRIVATE_FUNCTION("private fun"),
    COMPOSABLE_FUNCTION("@Composable fun (ContentView() before the rest)"),
    CLASS_DECLARATION("data class, sealed class, inner class");

    companion object {
        /**
         * Returns the default order of sorting rules.
         */
        fun defaultOrder(): List<SortingRuleType> = values().toList()
    }
}
