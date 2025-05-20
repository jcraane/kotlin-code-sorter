package com.github.jcraane.kotlincodesorter.model

/**
 * Defines the rules for sorting Kotlin elements.
 * The sorting follows this hierarchy:
 * 1. abstract val/var
 * 2. public val/var
 * 3. private val/var (viewModel at the top of the class)
 * 4. companion object
 * 5. init
 * 6. override fun
 * 7. abstract fun
 * 8. public fun
 * 9. protected fun
 * 10. private fun
 * 11. @Composable fun (ContentView() before the rest)
 * 12. data class, sealed class, inner class
 */
object SortingRules {
    /**
     * Gets the rank of a Kotlin element based on the sorting hierarchy.
     * Lower rank means the element should appear earlier in the sorted file.
     */
    fun getElementRank(element: KotlinElement): Int {
        return when (element) {
            is KotlinElement.Property -> {
                when {
                    element.isAbstract -> 1
                    element.isPublic -> 2
                    element.isPrivate -> 3
                    else -> 4 // protected or other visibility
                }
            }
            is KotlinElement.CompanionObject -> 5
            is KotlinElement.InitBlock -> 6
            is KotlinElement.Function -> {
                when {
                    element.isOverride && !element.isComposable -> 7
                    element.isAbstract && !element.isComposable-> 8
                    element.isPublic && !element.isComposable-> 9
                    element.isProtected && !element.isComposable-> 10
                    element.isPrivate && !element.isComposable-> 11
                    element.isComposable -> 12
                    else -> 13
                }
            }
            is KotlinElement.ClassDeclaration -> 14
        }
    }

    /**
     * Comparator for sorting Kotlin elements according to the defined rules.
     */
    val kotlinElementComparator = Comparator<KotlinElement> { a, b ->
        // First compare by rank
        val rankComparison = getElementRank(a) - getElementRank(b)
        if (rankComparison != 0) return@Comparator rankComparison

        // Special case for viewModel properties
        if (a is KotlinElement.Property && b is KotlinElement.Property &&
            a.isPrivate && b.isPrivate) {
            if (a.isViewModelProperty && !b.isViewModelProperty) return@Comparator -1
            if (!a.isViewModelProperty && b.isViewModelProperty) return@Comparator 1
        }

        // Special case for ContentView composable
        if (a is KotlinElement.Function && b is KotlinElement.Function &&
            a.isComposable && b.isComposable) {
            if (a.isContentView && !b.isContentView) return@Comparator -1
            if (!a.isContentView && b.isContentView) return@Comparator 1
        }

        // Alphabetical ordering for same type
        a.name.compareTo(b.name)
    }
}
