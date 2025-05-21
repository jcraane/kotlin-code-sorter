package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.idea.KotlinFileType

class KotlinElementParserSimpleTest : BasePlatformTestCase() {
    @OptIn(KaAllowAnalysisOnEdt::class)
    fun testParseSimpleInterface() {
        val kotlinCode = """
            interface SimpleInterface {
                fun doSomething()
                val property: String
            }
        """.trimIndent()

        allowAnalysisOnEdt {
            val psiFile = myFixture.configureByText(KotlinFileType.INSTANCE, kotlinCode)
            val parser = KotlinElementParser()
            val elements = parser.parse(psiFile)

            println("[DEBUG_LOG] Parsed elements: $elements")

            // Assert that we have the correct number of elements
            assertEquals(1, elements.size)

            // Find and assert the interface declaration
            val interfaceDeclaration = elements.find { it is KotlinElement.ClassDeclaration && it.name == "SimpleInterface" } as KotlinElement.ClassDeclaration?
            assertNotNull("Interface declaration should be found", interfaceDeclaration)

            // Print debug information
            println("[DEBUG_LOG] Interface declaration: $interfaceDeclaration")
        }
    }
}
