package com.github.jcraane.kotlincodesorter.actions

import com.github.jcraane.kotlincodesorter.services.KotlinElementSorter
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile

/**
 * Action to sort Kotlin files according to the defined rules.
 */
class SortKotlinFileAction : AnAction() {
    private val sorter = KotlinElementSorter()

    /**
     * Performs the action when triggered.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return

        // Check if the file is a Kotlin file
        if (!isKotlinFile(file)) {
            Messages.showErrorDialog(project, "Not a Kotlin file", "Cannot Sort")
            return
        }

        // Sort the file
        val success = sorter.sortFile(project, file)

        // Show a notification based on the result
        if (success) {
            Messages.showInfoMessage(project, "File sorted successfully", "Sort Kotlin File")
        } else {
            Messages.showErrorDialog(project, "Failed to sort file", "Sort Kotlin File")
        }
    }

    /**
     * Updates the action's state.
     */
    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabled = file != null && isKotlinFile(file)
    }

    /**
     * Returns the thread in which update() is called.
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    /**
     * Determines if a file is a Kotlin file.
     */
    private fun isKotlinFile(file: PsiFile): Boolean {
        return file.fileType.name.equals("Kotlin", ignoreCase = true) ||
               file.name.endsWith(".kt") ||
               file.name.endsWith(".kts")
    }
}
