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
            if (sortingData.topLevelElements.isNotEmpty()) {
                return applyTopLevelSort(project, file, sortingData.topLevelElements)
            }

            if (sortingData.classData.isNotEmpty()) {
                return applyClassSort(project, document, documentManager, sortingData.classData)
            }

            return false
        } catch (e: Exception) {
            log.error("Error applying sorting to file: ${file.name}", e)
            return false
        }
    }

    /**
     * Applies sorting to class elements.
     */
    private fun applyClassSort(
        project: Project,
        document: Document,
        documentManager: PsiDocumentManager,
        classData: List<ClassSortingData>
    ): Boolean {
        var success = true

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                for (classData in classData) {
                    val newClassBodyContent = constructSortedClassBodyContent(
                        document,
                        classData.elements,
                    )

                    document.replaceString(
                        classData.classBody.textRange.startOffset + 1, // +1 to skip the opening brace
                        classData.classBody.textRange.endOffset - 1,   // -1 to skip the closing brace
                        newClassBodyContent
                    )
                }

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
     */
    private fun constructSortedClassBodyContent(
        document: Document,
        sortedElements: List<KotlinElement>,
    ): String {
        val sb = StringBuilder()

        for (element in sortedElements) {
            val elementText = document.getText(
                TextRange(element.startOffset, element.endOffset)
            )

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
     * Applies the sorted elements to the file at top level.
     */
    private fun applyTopLevelSort(project: Project, file: PsiFile, sortedElements: List<KotlinElement>): Boolean {
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(file) ?: return false

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val elementTexts = getElementTexts(document, sortedElements)

                val startOffset = sortedElements.minByOrNull { it.startOffset }?.startOffset ?: return@runWriteCommandAction
                val endOffset = sortedElements.maxByOrNull { it.endOffset }?.endOffset ?: return@runWriteCommandAction

                val newContent = buildSortedContent(sortedElements, elementTexts)

                document.replaceString(startOffset, endOffset, newContent)
                documentManager.commitDocument(document)
            } catch (e: Exception) {
                log.error("Error applying sort", e)
            }
        }

        return true
    }

    /**
     * Gets the text of each element from the document.
     */
    private fun getElementTexts(document: Document, elements: List<KotlinElement>): Map<KotlinElement, String> {
        val elementTexts = mutableMapOf<KotlinElement, String>()

        for (element in elements) {
            val text = document.getText(TextRange(element.startOffset, element.endOffset))
            elementTexts[element] = text
        }

        return elementTexts
    }

    /**
     * Builds the content string with sorted elements.
     */
    private fun buildSortedContent(
        sortedElements: List<KotlinElement>,
        elementTexts: Map<KotlinElement, String>
    ): String {
        val newContent = StringBuilder()

        for (element in sortedElements) {
            newContent.append(elementTexts[element] ?: "")

            if (element != sortedElements.last()) {
                newContent.append("\n")
            }
        }

        return newContent.toString()
    }
}
