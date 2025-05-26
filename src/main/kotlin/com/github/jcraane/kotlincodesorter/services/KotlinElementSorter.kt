package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.ClassSortingData
import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.github.jcraane.kotlincodesorter.model.SortingData
import com.github.jcraane.kotlincodesorter.model.SortingRules
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
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
     * Prepares sorting data for a Kotlin file by analyzing its structure and elements.
     * This method performs only read operations and does not modify the file.
     *
     * @param project The current project
     * @param file The Kotlin file to analyze
     * @return SortingData containing all information needed for sorting
     */
    @OptIn(KaAllowAnalysisOnEdt::class)
    fun prepareSortingData(project: Project, file: PsiFile): SortingData {
        log.info("Preparing sorting data for file: ${file.name}")

        if (file !is KtFile) {
            log.warn("File is not a Kotlin file: ${file.name}")
            return SortingData()
        }

        // Find all classes in the file
        val classes = file.getChildrenOfType<KtClass>()

        // If there are no classes, collect top-level elements
        if (classes.isEmpty()) {
            log.info("No classes found in file: ${file.name}")
            val elements = allowAnalysisOnEdt {
                parser.parse(file)
            }
            if (elements.isEmpty()) {
                return SortingData()
            }
            val sortedElements = elements.sortedWith(SortingRules.kotlinElementComparator)
            return SortingData(topLevelElements = sortedElements)
        }

        // Collect data for each class
        val classDataList = mutableListOf<ClassSortingData>()
        for (ktClass in classes) {
            val classBody = ktClass.findDescendantOfType<KtClassBody>() ?: continue

            // Parse elements within this class body
            val elementsInClass = allowAnalysisOnEdt {
                parser.parse(ktClass)
            }
            if (elementsInClass.isEmpty()) continue

            // Sort the elements within the class
            val sortedElements = elementsInClass.sortedWith(SortingRules.kotlinElementComparator)

            classDataList.add(ClassSortingData(ktClass, classBody, sortedElements))
        }

        return SortingData(classData = classDataList)
    }

    /**
     * Applies sorting to a Kotlin file using the provided sorting data.
     * This method performs write operations to modify the file.
     *
     * @param project The current project
     * @param file The Kotlin file to modify
     * @param sortingData The sorting data to apply
     * @return True if the sorting was successful, false otherwise
     */
    fun applySorting(project: Project, file: PsiFile, sortingData: SortingData): Boolean {
        log.info("Applying sorting to file: ${file.name}")

        if (file !is KtFile) {
            log.warn("File is not a Kotlin file: ${file.name}")
            return false
        }

        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(file) ?: return false

        try {
            // Handle top-level elements
            if (sortingData.topLevelElements.isNotEmpty()) {
                return applySort(project, file, sortingData.topLevelElements)
            }

            // Handle class elements
            if (sortingData.classData.isNotEmpty()) {
                var success = true

                WriteCommandAction.runWriteCommandAction(project) {
                    try {
                        for (classData in sortingData.classData) {
                            // Create the sorted content for the class body
                            val newClassBodyContent = constructSortedClassBodyContent(
                                document,
                                classData.elements,
                                classData.classBody
                            )

                            // Replace the class body content
                            document.replaceString(
                                classData.classBody.textRange.startOffset + 1, // +1 to skip the opening brace
                                classData.classBody.textRange.endOffset - 1,   // -1 to skip the closing brace
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

            return false
        } catch (e: Exception) {
            log.error("Error applying sorting to file: ${file.name}", e)
            return false
        }
    }

    /**
     * Sorts the elements in a Kotlin file according to the defined rules.
     * This method is kept for backward compatibility and uses the new prepareSortingData/applySorting methods.
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

            // Use the new methods to separate read and write operations
            val sortingData = prepareSortingData(project, file)
            return applySorting(project, file, sortingData)
        } catch (e: Exception) {
            log.error("Error sorting file: ${file.name}", e)
            return false
        }
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
        classBody: KtClassBody,
    ): String {
        val sb = StringBuilder()

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
                val startOffset =
                    sortedElements.minByOrNull { it.startOffset }?.startOffset ?: return@runWriteCommandAction
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
