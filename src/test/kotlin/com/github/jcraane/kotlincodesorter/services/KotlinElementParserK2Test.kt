package com.github.jcraane.kotlincodesorter.services

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
            val parser = KotlinElementParserK2()
            val elements = parser.parse(psiFile)
            println(elements)
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
            val parser = KotlinElementParserK2()
            val elements = parser.parse(psiFile)
            println(elements)
        }
    }

}
