package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtInitializerList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass

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
            val declarations = if (file.declarations.size == 1) {
                (file.declarations.first() as? KtClassOrObject)?.declarations ?: emptyList()
            } else {
                file.declarations
            }

            mapKtDeclarationsToKotlinElements(declarations)
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
            val declarations = if (ktClass.declarations.size == 1) {
                (ktClass.declarations.first() as? KtClassOrObject)?.declarations ?: emptyList()
            } else {
                ktClass.declarations
            }

            mapKtDeclarationsToKotlinElements(declarations)
        }
    }

    /**
     * Maps KtDeclaration objects to KotlinElement objects using the K2 analysis API.
     *
     * @param declarations List of KtDeclaration objects to be mapped
     * @return List of KotlinElement objects
     */
    fun mapKtDeclarationsToKotlinElements(declarations: List<KtDeclaration>): List<KotlinElement> {
        return declarations.mapNotNull { declaration ->
            when (declaration) {
                is KtProperty -> mapPropertyToKotlinElement(declaration)
                is KtNamedFunction -> mapFunctionToKotlinElement(declaration)
                is KtClass -> mapClassToKotlinElement(declaration)
                is KtObjectDeclaration -> mapObjectToKotlinElement(declaration)
                is KtInitializerList -> mapInitBlockToKotlinElement(declaration)
                else -> null // Skip unsupported declarations
            }
        }
    }

    /**
     * Maps a KtProperty to a KotlinElement.Property
     */
    private fun mapPropertyToKotlinElement(property: KtProperty): KotlinElement.Property {
        val modifierList = property.modifierList

        val isPrivate = modifierList?.hasModifier(KtTokens.PRIVATE_KEYWORD) ?: false
        val isPublic = !isPrivate && !(modifierList?.hasModifier(KtTokens.PROTECTED_KEYWORD) ?: true)
        val isAbstract = modifierList?.hasModifier(KtTokens.ABSTRACT_KEYWORD) ?: false

        val isViewModelProperty = property.containingClass()?.let {
            it.superTypeListEntries.any { entry ->
                entry.typeReference?.text?.contains("ViewModel") ?: false
            }
        } ?: false

        return KotlinElement.Property(
            name = property.name ?: "",
            isPrivate = isPrivate,
            isPublic = isPublic,
            isAbstract = isAbstract,
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

        val isPrivate = modifierList?.hasModifier(KtTokens.PRIVATE_KEYWORD) ?: false
        val isProtected = modifierList?.hasModifier(KtTokens.PROTECTED_KEYWORD) ?: false
        val isPublic = !isPrivate && !isProtected
        val isAbstract = modifierList?.hasModifier(KtTokens.ABSTRACT_KEYWORD) ?: false
        val isOverride = modifierList?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false

        // Check for Compose annotations
        val annotations = function.annotationEntries.toList()
        val isComposable = annotations.any { it.shortName?.asString() == "Composable" }
        val isContentView = annotations.any { it.shortName?.asString() == "ContentView" } ||
            function.name?.contains("Content", ignoreCase = true) == true

        return KotlinElement.Function(
            name = function.name ?: "",
            isPrivate = isPrivate,
            isPublic = isPublic,
            isProtected = isProtected,
            isAbstract = isAbstract,
            isOverride = isOverride,
            isComposable = isComposable,
            isContentView = isContentView,
            startOffset = function.textRange.startOffset,
            endOffset = function.textRange.endOffset
        )
    }

    /**
     * Maps a KtClass to a KotlinElement.ClassDeclaration
     */
    private fun mapClassToKotlinElement(ktClass: KtClass): KotlinElement.ClassDeclaration {
        val isDataClass = ktClass.isData()
        val isSealedClass = ktClass.modifierList?.hasModifier(KtTokens.SEALED_KEYWORD) ?: false
        val isInnerClass = ktClass.modifierList?.hasModifier(KtTokens.INNER_KEYWORD) ?: false

        return KotlinElement.ClassDeclaration(
            name = ktClass.name ?: "",
            isDataClass = isDataClass,
            isSealedClass = isSealedClass,
            isInnerClass = isInnerClass,
            startOffset = ktClass.textRange.startOffset,
            endOffset = ktClass.textRange.endOffset
        )
    }

    /**
     * Maps a KtObjectDeclaration to a KotlinElement.CompanionObject if it's a companion object
     */
    private fun mapObjectToKotlinElement(objectDeclaration: KtObjectDeclaration): KotlinElement? {
        return if (objectDeclaration.isCompanion()) {
            KotlinElement.CompanionObject(
                name = objectDeclaration.name ?: "companion",
                startOffset = objectDeclaration.textRange.startOffset,
                endOffset = objectDeclaration.textRange.endOffset
            )
        } else {
            null // Skip non-companion objects
        }
    }

    /**
     * Maps a KtInitializerList to a KotlinElement.InitBlock
     */
    private fun mapInitBlockToKotlinElement(initBlock: KtInitializerList): KotlinElement.InitBlock {
        return KotlinElement.InitBlock(
            startOffset = initBlock.textRange.startOffset,
            endOffset = initBlock.textRange.endOffset
        )
    }
}
