package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.ClassSortingData
import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.github.jcraane.kotlincodesorter.model.SortingData
import com.github.jcraane.kotlincodesorter.model.SortingRules
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
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
     * Sorts the elements in a Kotlin file according to the defined rules.
     * This method is kept for backward compatibility and uses the prepareSortingData/applySorting methods.
     *
     * @param project The current project
     * @param file The Kotlin file to sort
     * @return True if the sorting was successful, false otherwise
     */
    fun sortFile(project: Project, file: PsiFile): Boolean {
        log.info("Sorting file: ${file.name}")

        if (file !is KtFile) {
            log.warn("File is not a Kotlin file: ${file.name}")
            return false
        }

        try {
            val sortingData = prepareSortingData(project, file)
            return applySorting(project, file, sortingData)
        } catch (e: Exception) {
            log.error("Error sorting file: ${file.name}", e)
            return false
        }
    }

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

        val classes = file.getChildrenOfType<KtClass>().toList()

        return if (classes.isEmpty()) {
            prepareTopLevelElementsSortingData(file)
        } else {
            prepareClassSortingData(classes)
        }
    }

    /**
     * Prepares sorting data for top-level elements in a file.
     */
    @OptIn(KaAllowAnalysisOnEdt::class)
    private fun prepareTopLevelElementsSortingData(file: KtFile): SortingData {
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

    /**
     * Prepares sorting data for classes in a file.
     */
    @OptIn(KaAllowAnalysisOnEdt::class)
    private fun prepareClassSortingData(classes: List<KtClass>): SortingData {
        val classDataList = mutableListOf<ClassSortingData>()

        for (ktClass in classes) {
            val classBody = ktClass.findDescendantOfType<KtClassBody>() ?: continue

            val elementsInClass = allowAnalysisOnEdt {
                parser.parse(ktClass)
            }

            if (elementsInClass.isEmpty()) continue

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
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    // Apply top-level sorting if needed
                    if (sortingData.topLevelElements.isNotEmpty()) {
                        applySortToElements(document, sortingData.topLevelElements)
                    }

                    // Apply class sorting if needed
                    if (sortingData.classData.isNotEmpty()) {
                        for (classData in sortingData.classData) {
                            applySortToClassBody(document, classData)
                        }
                    }

                    documentManager.commitDocument(document)
                } catch (e: Exception) {
                    log.error("Error applying sort", e)
                }
            }

            return true
        } catch (e: Exception) {
            log.error("Error applying sorting to file: ${file.name}", e)
            return false
        }
    }

    /**
     * Applies sorting to class body elements.
     */
    private fun applySortToClassBody(document: Document, classData: ClassSortingData) {
        val elementTexts = getElementTexts(document, classData.elements)
        val newContent = buildClassBodyContent(classData.elements, elementTexts)

        // Replace the content within the class body, excluding braces
        document.replaceString(
            classData.classBody.textRange.startOffset + 1, // +1 to skip the opening brace
            classData.classBody.textRange.endOffset - 1,   // -1 to skip the closing brace
            newContent
        )
    }

    /**
     * Applies sorting to a list of elements.
     */
    private fun applySortToElements(document: Document, elements: List<KotlinElement>) {
        if (elements.isEmpty()) return

        val elementTexts = getElementTexts(document, elements)

        val startOffset = elements.minByOrNull { it.startOffset }?.startOffset ?: return
        val endOffset = elements.maxByOrNull { it.endOffset }?.endOffset ?: return

        val newContent = buildTopLevelContent(elements, elementTexts)

        document.replaceString(startOffset, endOffset, newContent)
    }

    /**
     * Gets the text of each element from the document.
     */
    private fun getElementTexts(document: Document, elements: List<KotlinElement>): Map<KotlinElement, String> {
        return elements.associateWith { element ->
            document.getText(TextRange(element.startOffset, element.endOffset))
        }
    }

    /**
     * Builds the content string with sorted elements for top-level content.
     */
    private fun buildTopLevelContent(
        elements: List<KotlinElement>,
        elementTexts: Map<KotlinElement, String>
    ): String {
        val newContent = StringBuilder()

        for (element in elements) {
            newContent.append(elementTexts[element] ?: "")

            if (element != elements.last()) {
                newContent.append("\n")
            }
        }

        return newContent.toString()
    }

    /**
     * Builds the content string with sorted elements for class body content.
     */
    private fun buildClassBodyContent(
        elements: List<KotlinElement>,
        elementTexts: Map<KotlinElement, String>
    ): String {
        val sb = StringBuilder()

        for (element in elements) {
            val elementText = elementTexts[element] ?: continue

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
}
