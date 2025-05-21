package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isProtected
import org.jetbrains.kotlin.psi.psiUtil.isPublic

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

        return analyze(file) {
            parseDeclarationsRecursively(file.declarations)
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
                is KtProperty -> mapPropertyToKotlinElement(declaration)
                is KtNamedFunction -> mapFunctionToKotlinElement(declaration)
                is KtClass -> {
                    // Don't process nested declarations for sealed classes, they're part of the class
                    val classElement = mapClassToKotlinElement(declaration)
                    classElement
                }

                is KtObjectDeclaration -> {
                    // Only process object declarations if they're not inside a sealed class
                    val containingClass = declaration.containingClass()
                    val isInsideSealedClass = containingClass?.modifierList?.hasModifier(KtTokens.SEALED_KEYWORD) ?: false

                    if (!isInsideSealedClass) {
                        val element = mapObjectToKotlinElement(declaration)
                        element
                    } else {
                        // Skip object declarations inside sealed classes
                        null
                    }
                }

                is KtClassInitializer -> mapInitBlockToKotlinElement(declaration)
                else -> null // Skip unsupported declarations
            }

            if (element != null) {
                elements.add(element)
            }
        }

        return elements
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
     * Maps a KtProperty to a KotlinElement.Property
     */
    private fun mapPropertyToKotlinElement(property: KtProperty): KotlinElement.Property {
        val isViewModelProperty = property.containingClass()?.let {
            it.superTypeListEntries.any { entry ->
                entry.typeReference?.text?.contains("ViewModel") ?: false
            }
        } ?: false

        return KotlinElement.Property(
            name = property.name ?: "",
            isPrivate = property.isPrivate(),
            isPublic = property.isPublic,
            isAbstract = property.isAbstract(),
            isViewModelProperty = isViewModelProperty,
            startOffset = property.textRange.startOffset,
            endOffset = property.textRange.endOffset
        )
    }

    /**
     * Maps a KtNamedFunction to a KotlinElement.Function
     */
    private fun mapFunctionToKotlinElement(function: KtNamedFunction): KotlinElement.Function {
        val modifierList = function.modifierList

        val isOverride = modifierList?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false

        // Check for Compose annotations
        val annotations = function.annotationEntries.toList()
        val isComposable = annotations.any { it.shortName?.asString() == "Composable" }
        val isContentView = annotations.any { it.shortName?.asString() == "ContentView" } ||
            function.name?.contains("Content", ignoreCase = true) == true

        return KotlinElement.Function(
            name = function.name ?: "",
            isPrivate = function.isPrivate(),
            isPublic = function.isPublic,
            isProtected = function.isProtected(),
            isAbstract = function.isAbstract(),
            isOverride = isOverride,
            isComposable = isComposable,
            isContentView = isContentView,
            startOffset = function.textRange.startOffset,
            endOffset = function.textRange.endOffset
        )
    }

    /**
     * Maps a KtClassOrObject to a KotlinElement.ClassDeclaration
     */
    private fun mapClassToKotlinElement(clazz: KtClassOrObject): KotlinElement.ClassDeclaration {
        val isDataClass = clazz is KtClass && clazz.isData()
        val isSealedClass = clazz is KtClass && clazz.modifierList?.hasModifier(KtTokens.SEALED_KEYWORD) ?: false
        val isInnerClass = clazz.modifierList?.hasModifier(KtTokens.INNER_KEYWORD) ?: false

        return KotlinElement.ClassDeclaration(
            name = clazz.name ?: "",
            isDataClass = isDataClass,
            isSealedClass = isSealedClass,
            isInnerClass = isInnerClass,
            startOffset = clazz.textRange.startOffset,
            endOffset = clazz.textRange.endOffset
        )
    }

    /**
     * Maps a KtObjectDeclaration to a KotlinElement.CompanionObject if it's a companion object
     * or to a KotlinElement.ClassDeclaration if it's a regular object
     */
    private fun mapObjectToKotlinElement(objectDeclaration: KtObjectDeclaration): KotlinElement {
        return if (objectDeclaration.isCompanion()) {
            KotlinElement.CompanionObject(
                name = objectDeclaration.name ?: "companion",
                startOffset = objectDeclaration.textRange.startOffset,
                endOffset = objectDeclaration.textRange.endOffset
            )
        } else {
            // Map non-companion objects to ClassDeclaration
            KotlinElement.ClassDeclaration(
                name = objectDeclaration.name ?: "",
                isDataClass = false,
                isSealedClass = false,
                isInnerClass = false,
                startOffset = objectDeclaration.textRange.startOffset,
                endOffset = objectDeclaration.textRange.endOffset
            )
        }
    }

    /**
     * Maps a KtClassInitializer to a KotlinElement.InitBlock
     */
    private fun mapInitBlockToKotlinElement(initBlock: KtClassInitializer): KotlinElement.InitBlock {
        return KotlinElement.InitBlock(
            startOffset = initBlock.textRange.startOffset,
            endOffset = initBlock.textRange.endOffset
        )
    }
}
