package com.github.jcraane.kotlincodesorter.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

/**
 * Service for accessing sorting settings.
 */
@Service
class SortingSettingsService {
    /**
     * Gets the current sorting rule order.
     */
    fun getSortingRuleOrder(): List<SortingRuleType> {
        return getSettingsState().getSortingRuleTypes()
    }

    /**
     * Updates the sorting rule order.
     */
    fun updateSortingRuleOrder(newOrder: List<SortingRuleType>) {
        getSettingsState().updateSortingRuleOrder(newOrder)
    }

    /**
     * Gets whether elements of the same type should be sorted alphabetically.
     */
    fun getSortAlphabetically(): Boolean {
        return getSettingsState().sortAlphabetically
    }

    /**
     * Sets whether elements of the same type should be sorted alphabetically.
     */
    fun setSortAlphabetically(value: Boolean) {
        getSettingsState().sortAlphabetically = value
    }

    /**
     * Gets the comma-separated list of element names to exclude from sorting.
     */
    fun getExcludedElementNames(): String {
        return getSettingsState().excludedElementNames
    }

    /**
     * Sets the comma-separated list of element names to exclude from sorting.
     */
    fun setExcludedElementNames(value: String) {
        getSettingsState().excludedElementNames = value
    }

    /**
     * Gets the list of element names to exclude from sorting.
     * Converts the comma-separated string to a list of trimmed strings.
     */
    fun getExcludedElementNamesList(): List<String> {
        val excludedNames = getExcludedElementNames()
        return if (excludedNames.isBlank()) {
            emptyList()
        } else {
            excludedNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    /**
     * Gets the settings state.
     */
    private fun getSettingsState(): SortingSettingsState {
        return service<SortingSettingsState>()
    }

    companion object {
        /**
         * Gets the instance of the service.
         */
        fun getInstance(): SortingSettingsService {
            return ApplicationManager.getApplication().getService(SortingSettingsService::class.java)
        }
    }
}
