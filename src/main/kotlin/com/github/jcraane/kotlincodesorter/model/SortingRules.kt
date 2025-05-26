package com.github.jcraane.kotlincodesorter.model

import com.github.jcraane.kotlincodesorter.settings.SortingRuleType
import com.github.jcraane.kotlincodesorter.settings.SortingSettingsService

/**
 * Defines the rules for sorting Kotlin elements. The order can be customized in the settings.
 */
object SortingRules {
    private var _settingsService: SortingSettingsService? = null

    var settingsService: SortingSettingsService
        get() = _settingsService ?: SortingSettingsService.getInstance()
        set(value) {
            _settingsService = value
        }

    fun resetSettingsService() {
        _settingsService = null
    }

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

        val sortingOrder = settingsService.getSortingRuleOrder()
        return sortingOrder.indexOf(ruleType).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }

    val kotlinElementComparator = Comparator<KotlinElement> { a, b ->
        val excludedNames = settingsService.getExcludedElementNamesList()
        val aExcluded = excludedNames.contains(a.name)
        val bExcluded = excludedNames.contains(b.name)

        if (aExcluded && bExcluded) {
            val originalOrder = 0
            return@Comparator originalOrder
        }


        if (aExcluded && !bExcluded) {
            return@Comparator 1
        } else if (!aExcluded && bExcluded) {
            return@Comparator -1
        }

        // If we get here, either both elements are excluded or neither is excluded
        // In either case, proceed with normal sorting

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
