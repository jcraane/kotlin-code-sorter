package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.idea.KotlinFileType

class KotlinElementParserK2Test : BasePlatformTestCase() {
    @OptIn(KaAllowAnalysisOnEdt::class)
    fun testGetDeclarationsForClass() {
        val kotlinCode = """
            class MyClass {
                companion object {

                }

                private val test: String = ""

                data class Test(val s: String) {

                }

                val publiek: String = "hallo"

                fun test() {
                    println("Hello World")
                }

                @Composable
                fun SetContent() {

                }
            }
        """.trimIndent()

        allowAnalysisOnEdt {
            val psiFile = myFixture.configureByText(KotlinFileType.INSTANCE, kotlinCode)
            val parser = KotlinElementParser()
            val elements = parser.parse(psiFile)
            println(elements)

            // Assert that we have the correct number of elements
            assertEquals(1, elements.size)

            // Find and assert the class declaration
            val classDeclaration = elements.find { it is KotlinElement.ClassDeclaration && it.name == "MyClass" } as KotlinElement.ClassDeclaration?
            assertNotNull("Class declaration should be found", classDeclaration)
            assertFalse("Should not be a data class", classDeclaration!!.isDataClass)
            assertFalse("Should not be a sealed class", classDeclaration.isSealedClass)
            assertFalse("Should not be an inner class", classDeclaration.isInnerClass)
        }
    }

    @OptIn(KaAllowAnalysisOnEdt::class)
    fun testGetDeclarationsForFile() {
        val kotlinCode = """
            companion object {

            }

            private val test: String = ""

            data class Test(val s: String) {

            }

            val publiek: String = "hallo"

            fun test() {
                println("Hello World")
            }

            @Composable
            fun SetContent() {

            }
        """.trimIndent()

        allowAnalysisOnEdt {
            val psiFile = myFixture.configureByText(KotlinFileType.INSTANCE, kotlinCode)
            val parser = KotlinElementParser()
            val elements = parser.parse(psiFile)
            println(elements)

            // Assert that we have the correct number of elements
            assertEquals(6, elements.size)

            // Find and assert the companion object
            val companionObject = elements.find { it is KotlinElement.CompanionObject } as KotlinElement.CompanionObject?
            assertNotNull("Companion object should be found", companionObject)

            // Find and assert the private property
            val privateProperty = elements.find { it is KotlinElement.Property && it.name == "test" } as KotlinElement.Property?
            assertNotNull("Private property should be found", privateProperty)
            assertTrue("Property should be private", privateProperty!!.isPrivate)
            assertFalse("Property should not be public", privateProperty.isPublic)
            assertFalse("Property should not be abstract", privateProperty.isAbstract)

            // Find and assert the data class
            val dataClass = elements.find { it is KotlinElement.ClassDeclaration && it.name == "Test" } as KotlinElement.ClassDeclaration?
            assertNotNull("Data class should be found", dataClass)
            assertTrue("Should be a data class", dataClass!!.isDataClass)
            assertFalse("Should be a sealed class", dataClass.isSealedClass)
            assertFalse("Should be an inner class", dataClass.isInnerClass)

            // Find and assert the public property
            val publicProperty = elements.find { it is KotlinElement.Property && it.name == "publiek" } as KotlinElement.Property?
            assertNotNull("Public property should be found", publicProperty)
            assertFalse("Property should not be private", publicProperty!!.isPrivate)
            assertTrue("Property should be public", publicProperty.isPublic)

            // Find and assert the regular function
            val regularFunction = elements.find { it is KotlinElement.Function && it.name == "test" } as KotlinElement.Function?
            assertNotNull("Regular function should be found", regularFunction)
            assertFalse("Function should not be composable", regularFunction!!.isComposable)

            // Find and assert the composable function
            val composableFunction = elements.find { it is KotlinElement.Function && it.name == "SetContent" } as KotlinElement.Function?
            assertNotNull("Composable function should be found", composableFunction)
            assertTrue("Function should be composable", composableFunction!!.isComposable)
        }
    }

}
