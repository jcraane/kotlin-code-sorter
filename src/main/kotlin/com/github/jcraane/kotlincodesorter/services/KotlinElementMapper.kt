package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isProtected
import org.jetbrains.kotlin.psi.psiUtil.isPublic

/**
 * Responsible for mapping Kotlin PSI elements to KotlinElement model objects.
 */
class KotlinElementMapper {
    /**
     * Maps a KtProperty to a KotlinElement.Property
     */
    fun mapToKotlinElement(property: KtProperty): KotlinElement.Property {
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
    fun mapToKotlinElement(function: KtNamedFunction): KotlinElement.Function {
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
    fun mapToKotlinElement(clazz: KtClassOrObject): KotlinElement.ClassDeclaration {
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
    fun mapToKotlinElement(objectDeclaration: KtObjectDeclaration): KotlinElement {
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
    fun mapToKotlinElement(initBlock: KtClassInitializer): KotlinElement.InitBlock {
        return KotlinElement.InitBlock(
            startOffset = initBlock.textRange.startOffset,
            endOffset = initBlock.textRange.endOffset
        )
    }
}
