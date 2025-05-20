package com.github.jcraane.kotlincodesorter.services

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * Service for parsing Kotlin files and extracting elements that can be sorted.
 */
class KotlinElementParser {
    private val log = logger<KotlinElementParser>()

    /**
     * Parses a Kotlin file and extracts all sortable elements.
     *
     * @param file The Kotlin file to parse
     * @return A list of KotlinElement objects representing the sortable elements in the file
     */
    fun parse(file: PsiFile): List<KotlinElement> {
        val elements = mutableListOf<KotlinElement>()

        // For the initial implementation, we'll return an empty list
        // This will be expanded once we have access to Kotlin PSI classes
        log.info("Parsing file: ${file.name}")

        return elements
    }

    /**
     * Determines if a PsiElement is a property.
     */
    private fun isProperty(element: PsiElement): Boolean {
        return element.javaClass.simpleName.contains("Property")
    }

    /**
     * Determines if a PsiElement is a function.
     */
    private fun isFunction(element: PsiElement): Boolean {
        return element.javaClass.simpleName.contains("Function")
    }

    /**
     * Determines if a PsiElement is a companion object.
     */
    private fun isCompanionObject(element: PsiElement): Boolean {
        return element.javaClass.simpleName.contains("Object") &&
               element.text.contains("companion")
    }

    /**
     * Determines if a PsiElement is an init block.
     */
    private fun isInitBlock(element: PsiElement): Boolean {
        return element.javaClass.simpleName.contains("Initializer") ||
               element.text.startsWith("init")
    }

    /**
     * Determines if a PsiElement is a class declaration.
     */
    private fun isClass(element: PsiElement): Boolean {
        return element.javaClass.simpleName.contains("Class")
    }

    /**
     * Creates a Property element from a PsiElement.
     */
    private fun createPropertyElement(property: PsiElement): KotlinElement.Property {
        val name = property.text.substringAfter("val ").substringAfter("var ").substringBefore(":")
        val isViewModelProperty = name.endsWith("ViewModel") ||
                                  property.text.contains("ViewModel")

        return KotlinElement.Property(
            name = name.trim(),
            isPublic = !property.text.contains("private") && !property.text.contains("protected"),
            isPrivate = property.text.contains("private"),
            isProtected = property.text.contains("protected"),
            isAbstract = property.text.contains("abstract"),
            isOverride = property.text.contains("override"),
            isViewModelProperty = isViewModelProperty,
            startOffset = property.textRange.startOffset,
            endOffset = property.textRange.endOffset
        )
    }

    /**
     * Creates a Function element from a PsiElement.
     */
    private fun createFunctionElement(function: PsiElement): KotlinElement.Function {
        val text = function.text
        val name = text.substringAfter("fun ").substringBefore("(")
        val isComposable = text.contains("@Composable")
        val isContentView = name.trim() == "ContentView" && isComposable

        return KotlinElement.Function(
            name = name.trim(),
            isPublic = !text.contains("private") && !text.contains("protected"),
            isPrivate = text.contains("private"),
            isProtected = text.contains("protected"),
            isAbstract = text.contains("abstract"),
            isOverride = text.contains("override"),
            isComposable = isComposable,
            isContentView = isContentView,
            startOffset = function.textRange.startOffset,
            endOffset = function.textRange.endOffset
        )
    }

    /**
     * Creates a CompanionObject element from a PsiElement.
     */
    private fun createCompanionObjectElement(companionObject: PsiElement): KotlinElement.CompanionObject {
        return KotlinElement.CompanionObject(
            startOffset = companionObject.textRange.startOffset,
            endOffset = companionObject.textRange.endOffset
        )
    }

    /**
     * Creates an InitBlock element from a PsiElement.
     */
    private fun createInitBlockElement(initBlock: PsiElement): KotlinElement.InitBlock {
        return KotlinElement.InitBlock(
            startOffset = initBlock.textRange.startOffset,
            endOffset = initBlock.textRange.endOffset
        )
    }

    /**
     * Creates a ClassDeclaration element from a PsiElement.
     */
    private fun createClassElement(klass: PsiElement): KotlinElement.ClassDeclaration {
        val text = klass.text
        val name = text.substringAfter("class ").substringBefore("(").substringBefore("{").trim()

        return KotlinElement.ClassDeclaration(
            name = name,
            isPublic = !text.contains("private") && !text.contains("protected"),
            isPrivate = text.contains("private"),
            isProtected = text.contains("protected"),
            isData = text.contains("data class"),
            isSealed = text.contains("sealed class"),
            isInner = text.contains("inner class"),
            startOffset = klass.textRange.startOffset,
            endOffset = klass.textRange.endOffset
        )
    }
}
