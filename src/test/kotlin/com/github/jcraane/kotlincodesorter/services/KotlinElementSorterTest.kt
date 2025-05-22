package com.github.jcraane.kotlincodesorter.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.idea.KotlinFileType

/**
 * Tests for the KotlinElementSorter class.
 * This test verifies that the sorter correctly sorts Kotlin code elements.
 */
class KotlinElementSorterTest : BasePlatformTestCase() {

    private val sorter = KotlinElementSorter()

    @OptIn(KaAllowAnalysisOnEdt::class)
    fun testSortKotlinFile() {
        val kotlinCode = """
            class TestClass {
                private fun secondFunction() {}
                private fun firstFunction() {}
                
                val secondProperty: String = ""
                val firstProperty: String = ""
            }
        """.trimIndent()

        allowAnalysisOnEdt {
            val psiFile = myFixture.configureByText(KotlinFileType.INSTANCE, kotlinCode)

            // Sort the file
            val success = sorter.sortFile(project, psiFile)

            // Verify that sorting was successful
            assertTrue("Sorting should be successful", success)

            // Get the sorted text
            val resultText = myFixture.editor.document.text
            println("[DEBUG_LOG] Result text: $resultText")

            // Check that firstFunction comes before secondFunction
            val firstFunctionIndex = resultText.indexOf("firstFunction")
            val secondFunctionIndex = resultText.indexOf("secondFunction")
            assertTrue("firstFunction should come before secondFunction",
                firstFunctionIndex in 0 until secondFunctionIndex)

            // Check that firstProperty comes before secondProperty
            val firstPropertyIndex = resultText.indexOf("firstProperty")
            val secondPropertyIndex = resultText.indexOf("secondProperty")
            assertTrue("firstProperty should come before secondProperty",
                firstPropertyIndex in 0 until secondPropertyIndex)
        }
    }
}
