package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * Service for parsing Kotlin files into KotlinElement objects using K2.
 */
class KotlinElementParser(private val mapper: KotlinElementMapper = KotlinElementMapper()) {
    private val log = logger<KotlinElementParser>()

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

        return analyze(file) {
            parseDeclarationsRecursively(file.declarations)
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
            parseDeclarationsRecursively(ktClass.declarations)
        }
    }

    /**
     * Recursively parses declarations and their nested declarations.
     *
     * @param declarations List of KtDeclaration objects to be parsed
     * @return List of KotlinElement objects
     */
    private fun parseDeclarationsRecursively(declarations: List<KtDeclaration>): List<KotlinElement> {
        val elements = mutableListOf<KotlinElement>()

        declarations.forEach { declaration ->
            val element = when (declaration) {
                is KtProperty -> mapper.mapToKotlinElement(declaration)
                is KtNamedFunction -> mapper.mapToKotlinElement(declaration)
                is KtClass -> mapper.mapToKotlinElement(declaration)
                is KtObjectDeclaration -> {
                    val containingClass = declaration.containingClass()
                    val isInsideSealedClass = containingClass?.modifierList?.hasModifier(KtTokens.SEALED_KEYWORD) ?: false
                    if (!isInsideSealedClass) mapper.mapToKotlinElement(declaration) else null
                }
                is KtClassInitializer -> mapper.mapToKotlinElement(declaration)
                else -> null // Skip unsupported declarations
            }

            if (element != null) {
                elements.add(element)
            }
        }

        return elements
    }
}

