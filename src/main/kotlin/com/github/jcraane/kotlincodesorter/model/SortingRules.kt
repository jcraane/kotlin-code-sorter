package com.github.jcraane.kotlincodesorter.model

import com.github.jcraane.kotlincodesorter.settings.SortingRuleType
import com.github.jcraane.kotlincodesorter.settings.SortingSettingsService

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
 *
 * This order can be customized in the settings.
 */
object SortingRules {
    /**
     * Gets the settings service instance.
     */
    private var _settingsService: SortingSettingsService? = null

    /**
     * Gets or sets the settings service.
     * This allows for testing with a custom settings service.
     */
    var settingsService: SortingSettingsService
        get() = _settingsService ?: SortingSettingsService.getInstance()
        set(value) {
            _settingsService = value
        }

    /**
     * Resets the settings service to use the default instance.
     * This is useful for testing.
     */
    fun resetSettingsService() {
        _settingsService = null
    }
    /**
     * Gets the rank of a Kotlin element based on the sorting hierarchy.
     * Lower rank means the element should appear earlier in the sorted file.
     * The rank is determined by the order of rules in the settings.
     */
    fun getElementRank(element: KotlinElement): Int {
        val ruleType = when (element) {
            is KotlinElement.Property -> {
                when {
                    element.isAbstract -> SortingRuleType.ABSTRACT_PROPERTY
                    element.isPublic -> SortingRuleType.PUBLIC_PROPERTY
                    element.isPrivate -> SortingRuleType.PRIVATE_PROPERTY
                    else -> SortingRuleType.PUBLIC_PROPERTY // Default to public for other visibilities
                }
            }
            is KotlinElement.CompanionObject -> SortingRuleType.COMPANION_OBJECT
            is KotlinElement.InitBlock -> SortingRuleType.INIT_BLOCK
            is KotlinElement.Function -> {
                when {
                    element.isOverride && !element.isComposable -> SortingRuleType.OVERRIDE_FUNCTION
                    element.isAbstract && !element.isComposable -> SortingRuleType.ABSTRACT_FUNCTION
                    element.isPublic && !element.isComposable -> SortingRuleType.PUBLIC_FUNCTION
                    element.isProtected && !element.isComposable -> SortingRuleType.PROTECTED_FUNCTION
                    element.isPrivate && !element.isComposable -> SortingRuleType.PRIVATE_FUNCTION
                    element.isComposable -> SortingRuleType.COMPOSABLE_FUNCTION
                    else -> SortingRuleType.PUBLIC_FUNCTION // Default to public for other cases
                }
            }
            is KotlinElement.ClassDeclaration -> SortingRuleType.CLASS_DECLARATION
        }

        // Get the index of the rule type in the current sorting order
        val sortingOrder = settingsService.getSortingRuleOrder()
        return sortingOrder.indexOf(ruleType).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }

    /**
     * Comparator for sorting Kotlin elements according to the defined rules.
     */
    val kotlinElementComparator = Comparator<KotlinElement> { a, b ->
        // Check if either element should be excluded from sorting
        val excludedNames = settingsService.getExcludedElementNamesList()
        val aExcluded = excludedNames.contains(a.name)
        val bExcluded = excludedNames.contains(b.name)

        if (aExcluded && bExcluded) {
            return@Comparator 0 // Keep original order between excluded elements
        }


        // If both elements are excluded or neither is excluded, proceed with normal sorting
        // If only one is excluded, preserve its original position
        if (aExcluded && !bExcluded) {
            // a is excluded, b is not - a should stay in its original position (after b)
            return@Comparator 1
        } else if (!aExcluded && bExcluded) {
            // b is excluded, a is not - b should stay in its original position (after a)
            return@Comparator -1
        }

        // If we get here, either both elements are excluded or neither is excluded
        // In either case, proceed with normal sorting

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

        // Alphabetical ordering for same type (if enabled)
        if (settingsService.getSortAlphabetically()) {
            a.name.compareTo(b.name)
        } else {
            0 // Keep original order if alphabetical sorting is disabled
        }
    }
}
