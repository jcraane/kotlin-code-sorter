package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

/**
 * Service for parsing Kotlin files into KotlinElement objects.
 */
class KotlinElementParser {
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

        return parseElements(file)
    }

    /**
     * Parses a Kotlin class and extracts all elements within it.
     *
     * @param ktClass The Kotlin class to parse
     * @return A list of KotlinElement objects
     */
    fun parse(ktClass: KtClass): List<KotlinElement> {
        return parseElementsInClass(ktClass)
    }

    /**
     * Parses all elements in a Kotlin file.
     *
     * @param file The Kotlin file
     * @return A list of KotlinElement objects
     */
    private fun parseElements(file: KtFile): List<KotlinElement> {
        val elements = mutableListOf<KotlinElement>()

        // Parse top-level elements
        file.declarations.forEach { declaration ->
            when (declaration) {
                is KtProperty -> elements.add(parseProperty(declaration))
                is KtFunction -> elements.add(parseFunction(declaration))
                is KtClass -> elements.add(parseClass(declaration))
                is KtObjectDeclaration -> {
                    if (declaration.isCompanion()) {
                        elements.add(parseCompanionObject(declaration))
                    } else {
                        elements.add(parseClass(declaration))
                    }
                }
                // Add other top-level element types as needed
            }
        }

        return elements
    }

    /**
     * Parses all elements in a Kotlin class.
     *
     * @param ktClass The Kotlin class
     * @return A list of KotlinElement objects
     */
    private fun parseElementsInClass(ktClass: KtClass): List<KotlinElement> {
        val elements = mutableListOf<KotlinElement>()
        val body = ktClass.findDescendantOfType<KtClassBody>() ?: return elements

        // Parse declarations in the class body
        body.declarations.forEach { declaration ->
            when (declaration) {
                is KtProperty -> elements.add(parseProperty(declaration))
                is KtFunction -> elements.add(parseFunction(declaration))
                is KtClass -> elements.add(parseClass(declaration))
                is KtObjectDeclaration -> {
                    if (declaration.isCompanion()) {
                        elements.add(parseCompanionObject(declaration))
                    } else {
                        elements.add(parseClass(declaration))
                    }
                }

                is KtClassInitializer -> elements.add(parseInitBlock(declaration))
                // Add other class member types as needed
            }
        }

        return elements
    }

    /**
     * Parses a Kotlin property.
     *
     * @param property The Kotlin property
     * @return A KotlinElement.Property object
     */
    private fun parseProperty(property: KtProperty): KotlinElement.Property {
        val isPrivate = property.hasModifier(KtTokens.PRIVATE_KEYWORD)
        val isPublic = !isPrivate && !property.hasModifier(KtTokens.PROTECTED_KEYWORD)
        val isAbstract = property.hasModifier(KtTokens.ABSTRACT_KEYWORD)
        val isViewModelProperty = property.name?.endsWith("ViewModel") == true

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
     * Parses a Kotlin function.
     *
     * @param function The Kotlin function
     * @return A KotlinElement.Function object
     */
    private fun parseFunction(function: KtFunction): KotlinElement.Function {
        val isPrivate = function.hasModifier(KtTokens.PRIVATE_KEYWORD)
        val isPublic = !isPrivate && !function.hasModifier(KtTokens.PROTECTED_KEYWORD)
        val isProtected = function.hasModifier(KtTokens.PROTECTED_KEYWORD)
        val isAbstract = function.hasModifier(KtTokens.ABSTRACT_KEYWORD)
        val isOverride = function.hasModifier(KtTokens.OVERRIDE_KEYWORD)

        // Check for Composable annotation
        val isComposable = function.annotationEntries.any {
            it.shortName?.asString() == "Composable"
        }

        // Check if it's a ContentView function
        val isContentView = function.name == "ContentView" && isComposable

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
     * Parses a Kotlin class.
     *
     * @param clazz The Kotlin class
     * @return A KotlinElement.ClassDeclaration object
     */
    private fun parseClass(clazz: KtClassOrObject): KotlinElement.ClassDeclaration {
        val isDataClass = clazz is KtClass && clazz.isData()
        val isSealedClass = clazz is KtClass && clazz.isSealed()
        val isInnerClass = clazz.hasModifier(KtTokens.INNER_KEYWORD)

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
     * Parses a Kotlin companion object.
     *
     * @param companionObject The Kotlin companion object
     * @return A KotlinElement.CompanionObject object
     */
    private fun parseCompanionObject(companionObject: KtObjectDeclaration): KotlinElement.CompanionObject {
        return KotlinElement.CompanionObject(
            name = companionObject.name ?: "Companion",
            startOffset = companionObject.textRange.startOffset,
            endOffset = companionObject.textRange.endOffset
        )
    }

    /**
     * Parses a Kotlin init block.
     *
     * @param initBlock The Kotlin init block
     * @return A KotlinElement.InitBlock object
     */
    private fun parseInitBlock(initBlock: KtClassInitializer): KotlinElement.InitBlock {
        return KotlinElement.InitBlock(
            startOffset = initBlock.textRange.startOffset,
            endOffset = initBlock.textRange.endOffset
        )
    }
}
