package com.github.jcraane.kotlincodesorter.model

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody

/**
 * Data class to hold information needed for sorting Kotlin code.
 * This class is used to transfer data between the read phase (analysis)
 * and the write phase (modification).
 */
data class SortingData(
    // For file-level sorting
    val topLevelElements: List<KotlinElement> = emptyList(),

    // For class-level sorting
    val classData: List<ClassSortingData> = emptyList()
)

/**
 * Data class to hold sorting information for a specific class.
 */
data class ClassSortingData(
    val ktClass: KtClass,
    val classBody: KtClassBody,
    val elements: List<KotlinElement>
)
