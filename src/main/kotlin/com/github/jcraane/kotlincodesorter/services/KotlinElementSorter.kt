package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.github.jcraane.kotlincodesorter.model.SortingRules
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

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
            if (file !is KtFile) {
                log.warn("File is not a Kotlin file: ${file.name}")
                return false
            }

            // Sort elements inside each class in the file
            return sortClassElements(project, file)
        } catch (e: Exception) {
            log.error("Error sorting file: ${file.name}", e)
            return false
        }
    }

    /**
     * Sorts elements within each class in the file.
     *
     * @param project The current project
     * @param file The Kotlin file
     * @return True if the sorting was successful, false otherwise
     */
    private fun sortClassElements(project: Project, file: PsiFile): Boolean {
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(file) ?: return false

        // Find all classes in the file
        val ktFile = file as KtFile
        val classes = ktFile.getChildrenOfType<KtClass>()
        if (classes.isEmpty()) {
            log.info("No classes found in file: ${file.name}")
            // The file might contain top-level elements that can be sorted
            val elements = parser.parse(file)
            if (elements.isEmpty()) {
                return false
            }
            val sortedElements = elements.sortedWith(SortingRules.kotlinElementComparator)
            return applySort(project, file, sortedElements)
        }

        var success = true

        // Sort elements within each class
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                for (ktClass in classes) {
                    val classBody = ktClass.findDescendantOfType<KtClassBody>() ?: continue
                    val classBodyRange = classBody.textRange

                    // Only parse elements within this class body
                    val elementsInClass = parser.parse(ktClass)
                    if (elementsInClass.isEmpty()) continue

                    // Sort the elements within the class
                    val sortedElements = elementsInClass.sortedWith(SortingRules.kotlinElementComparator)

                    // Create the sorted content for the class body
                    val newClassBodyContent = constructSortedClassBodyContent(document, sortedElements, classBody)

                    // Replace the class body content
                    document.replaceString(
                        classBody.textRange.startOffset + 1, // +1 to skip the opening brace
                        classBody.textRange.endOffset - 1,   // -1 to skip the closing brace
                        newClassBodyContent
                    )
                }

                // Commit the document changes
                documentManager.commitDocument(document)
            } catch (e: Exception) {
                log.error("Error applying sort to classes", e)
                success = false
            }
        }

        return success
    }

    /**
     * Constructs the sorted content for a class body.
     *
     * @param document The document
     * @param sortedElements The sorted elements
     * @param classBody The class body
     * @return The sorted class body content
     */
    private fun constructSortedClassBodyContent(
        document: Document,
        sortedElements: List<KotlinElement>,
        classBody: KtClassBody
    ): String {
        val sb = StringBuilder()
        val classBodyStart = classBody.textRange.startOffset + 1 // +1 to skip the opening brace

        // Extract the original text of each element
        for (element in sortedElements) {
            val elementText = document.getText(
                com.intellij.openapi.util.TextRange(element.startOffset, element.endOffset)
            )

            // Add leading whitespace for proper indentation
            if (sb.isNotEmpty()) {
                sb.append("\n\n    ")
            } else {
                sb.append("\n    ")
            }

            sb.append(elementText)
        }

        // Close with a newline
        sb.append("\n")
        
        return sb.toString()
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
                // Get the original text of all elements
                val elementTexts = mutableMapOf<KotlinElement, String>()
                for (element in sortedElements) {
                    val text = document.getText(
                        com.intellij.openapi.util.TextRange(element.startOffset, element.endOffset)
                    )
                    elementTexts[element] = text
                }

                // Find the start and end of the content we're sorting
                val startOffset = sortedElements.minByOrNull { it.startOffset }?.startOffset ?: return@runWriteCommandAction
                val endOffset = sortedElements.maxByOrNull { it.endOffset }?.endOffset ?: return@runWriteCommandAction

                // Build the new content with sorted elements
                val newContent = StringBuilder()
                for (element in sortedElements) {
                    newContent.append(elementTexts[element] ?: "")
                    // Add a newline between elements if needed
                    if (element != sortedElements.last()) {
                        newContent.append("\n")
                    }
                }

                // Replace the entire content with the sorted content
                document.replaceString(startOffset, endOffset, newContent.toString())

                // Commit the document changes
                documentManager.commitDocument(document)
            } catch (e: Exception) {
                log.error("Error applying sort", e)
            }
        }

        return true
    }
}
