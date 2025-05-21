package com.github.jcraane.kotlincodesorter.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.BorderLayout

class SortingSettingsConfigurable : Configurable {
    private val settingsService = SortingSettingsService.getInstance()
    private var currentRuleOrder: MutableList<SortingRuleType> = mutableListOf()
    private var modified = false
    private var panel: JPanel? = null
    private var contentPanel: JPanel? = null

    override fun getDisplayName(): String = "Kotlin Code Sorter"

    override fun createComponent(): JComponent {
        // Initialize with current settings
        currentRuleOrder = settingsService.getSortingRuleOrder().toMutableList()

        // Create a container panel with BorderLayout
        panel = JPanel(BorderLayout())

        // Create and add the content panel
        updateContentPanel()

        return panel!!
    }

    private fun updateContentPanel() {
        // Remove old content if exists
        contentPanel?.let { panel?.remove(it) }

        // Create new content
        contentPanel = panel {
            group("Sorting Rules Order") {
                row {
                    label("Configure the order of sorting rules. Use the up/down buttons to change the order.")
                }

                // Create a row for each sorting rule with up/down buttons
                for (index in currentRuleOrder.indices) {
                    val rule = currentRuleOrder[index]
                    row("${rule.displayName}:") {
                        // Up button
                        button("↑") {
                            if (index > 0) {
                                val temp = currentRuleOrder[index]
                                currentRuleOrder[index] = currentRuleOrder[index - 1]
                                currentRuleOrder[index - 1] = temp
                                modified = true
                                updateContentPanel()
                            }
                        }.enabled(index > 0)

                        // Down button
                        button("↓") {
                            if (index < currentRuleOrder.size - 1) {
                                val temp = currentRuleOrder[index]
                                currentRuleOrder[index] = currentRuleOrder[index + 1]
                                currentRuleOrder[index + 1] = temp
                                modified = true
                                updateContentPanel()
                            }
                        }.enabled(index < currentRuleOrder.size - 1)
                    }
                }

                row {
                    button("Reset to Default") {
                        currentRuleOrder = SortingRuleType.defaultOrder().toMutableList()
                        modified = true
                        updateContentPanel()
                    }
                }
            }
        }

        // Add the new content panel
        panel?.add(contentPanel!!, BorderLayout.CENTER)

        // Refresh UI
        panel?.revalidate()
        panel?.repaint()
    }

    override fun isModified(): Boolean = modified

    override fun apply() {
        settingsService.updateSortingRuleOrder(currentRuleOrder)
        modified = false
    }

    override fun reset() {
        currentRuleOrder = settingsService.getSortingRuleOrder().toMutableList()
        modified = false
        updateContentPanel()
    }
}
