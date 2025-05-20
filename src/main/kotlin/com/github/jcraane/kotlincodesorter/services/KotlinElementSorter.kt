package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.github.jcraane.kotlincodesorter.model.SortingRules
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

/**
 * Service for sorting Kotlin elements in a file.
 */
class KotlinElementSorter {
    private val log = logger<KotlinElementSorter>()
    private val parser = KotlinElementParser()

    /**
     * Sorts the elements in a Kotlin file according to the defined rules.
     *
     * @param project The current project
     * @param file The Kotlin file to sort
     * @return True if the sorting was successful, false otherwise
     */
    fun sortFile(project: Project, file: PsiFile): Boolean {
        log.info("Sorting file: ${file.name}")

        try {
            // Parse the file to get the elements
            val elements = parser.parse(file)

            // If no elements were found, return false
            if (elements.isEmpty()) {
                log.warn("No elements found in file: ${file.name}")
                return false
            }

            // Sort the elements
            val sortedElements = elements.sortedWith(SortingRules.kotlinElementComparator)

            // Apply the sorted elements to the file
            return applySort(project, file, sortedElements)
        } catch (e: Exception) {
            log.error("Error sorting file: ${file.name}", e)
            return false
        }
    }

    /**
     * Applies the sorted elements to the file.
     *
     * @param project The current project
     * @param file The file to modify
     * @param sortedElements The sorted elements to apply
     * @return True if the operation was successful, false otherwise
     */
    private fun applySort(project: Project, file: PsiFile, sortedElements: List<KotlinElement>): Boolean {
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(file) ?: return false

        // Ensure all changes are made in a single command
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                // Apply the changes from last to first to avoid offset issues
                val sortedByOffsetDesc = sortedElements.sortedByDescending { it.startOffset }

                for (i in 0 until sortedByOffsetDesc.size - 1) {
                    val current = sortedByOffsetDesc[i]
                    val next = sortedByOffsetDesc[i + 1]

                    // Skip if the elements are already in the correct order
                    if (current.endOffset <= next.startOffset) {
                        continue
                    }

                    // Get the text of the current element
                    val currentText = document.getText(
                        com.intellij.openapi.util.TextRange(current.startOffset, current.endOffset)
                    )

                    // Delete the current element
                    document.deleteString(current.startOffset, current.endOffset)

                    // Insert the current element at the correct position
                    document.insertString(next.startOffset, currentText)
                }

                // Commit the document changes
                documentManager.commitDocument(document)
            } catch (e: Exception) {
                log.error("Error applying sort", e)
            }
        }

        return true
    }
}
