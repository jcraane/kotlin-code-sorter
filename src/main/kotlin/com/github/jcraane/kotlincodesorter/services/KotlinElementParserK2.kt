package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * Service for parsing Kotlin files into KotlinElement objects using K2.
 */
class KotlinElementParserK2 {
    private val log = logger<KotlinElementParserK2>()

    /**
     * Parses a Kotlin file and extracts all elements.
     *
     * @param file The Kotlin file to parse
     * @return A list of KotlinElement objects
     */
    fun parse(file: PsiFile): List<KotlinElement> {
        if (file !is KtFile) {
            log.warn("Not a Kotlin file: ${file.name}")
            return emptyList()
        }

        //(file.declarations.first() as? KtClass)?.declarations?.last()?.annotationEntries?.first()?.text
        return analyze(file) {
            val declarations = if (file.declarations.size == 1) {
                (file.declarations.first() as? KtClassOrObject)?.declarations ?: emptyList()
            } else {
                file.declarations
            }

            println(declarations)
            emptyList()
        }
    }

    /**
     * Parses a Kotlin class and extracts all elements within it.
     *
     * @param ktClass The Kotlin class to parse
     * @return A list of KotlinElement objects
     */
    fun parse(ktClass: KtClassOrObject): List<KotlinElement> {
        return analyze(ktClass) {
            emptyList()
        }
    }

    /**
     * Parses all elements in a Kotlin file.
     *
     * @param file The Kotlin file
     * @return A list of KotlinElement objects
     */
/*
    private fun KaSession.parseElements(file: KtFile): List<KotlinElement> {
        val elements = this.parseElements(file)
        return elements
    }
*/

    /**
     * Parses all elements in a Kotlin class.
     *
     * @param ktClass The Kotlin class
     * @return A list of KotlinElement objects
     */
/*
    private fun KaSession.parseElementsInClass(ktClass: KtClassOrObject): List<KotlinElement> {
        val elements = mutableListOf<KotlinElement>()

        val classSymbol = ktClass.getClassOrObjectSymbol()

        classSymbol.getMemberScope().getAllSymbols().forEach { symbol ->
            when (symbol) {
                is KtPropertySymbol -> elements.add(parseProperty(symbol))
                is KtFunctionSymbol -> elements.add(parseFunction(symbol))
                is KtClassOrObjectSymbol -> {
                    if (symbol.isCompanionObject) {
                        elements.add(parseCompanionObject(symbol))
                    } else {
                        elements.add(parseClass(symbol))
                    }
                }
                is KtClassInitializerSymbol -> elements.add(parseInitBlock(symbol))
                // Add other class member types as needed
            }
        }

        return elements
    }
*/

}
