package com.github.jcraane.kotlincodesorter.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.XCollection

/**
 * Persistent state component for storing sorting settings.
 */
@Service
@State(
    name = "com.github.jcraane.kotlincodesorter.settings.SortingSettingsState",
    storages = [Storage("kotlinCodeSorterSettings.xml")]
)
class SortingSettingsState : PersistentStateComponent<SortingSettingsState> {
    /**
     * The order of sorting rules.
     * This is stored as a list of enum names to make it serializable.
     */
    @XCollection(elementTypes = [String::class])
    var sortingRuleOrder: MutableList<String> = SortingRuleType.defaultOrder()
        .map { it.name }
        .toMutableList()

    /**
     * Converts the stored string list to a list of SortingRuleType.
     */
    fun getSortingRuleTypes(): List<SortingRuleType> {
        return sortingRuleOrder.mapNotNull { ruleName ->
            try {
                SortingRuleType.valueOf(ruleName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    /**
     * Updates the sorting rule order from a list of SortingRuleType.
     */
    fun updateSortingRuleOrder(newOrder: List<SortingRuleType>) {
        sortingRuleOrder = newOrder.map { it.name }.toMutableList()
    }

    override fun getState(): SortingSettingsState = this

    override fun loadState(state: SortingSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        /**
         * Returns the default settings state.
         */
        fun getDefault(): SortingSettingsState = SortingSettingsState()
    }
}
