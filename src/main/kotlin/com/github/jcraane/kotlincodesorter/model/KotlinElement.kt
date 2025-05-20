package com.github.jcraane.kotlincodesorter.model

/**
 * Represents an element in a Kotlin file that can be sorted.
 * This is the base class for all Kotlin elements that will be sorted.
 */
sealed class KotlinElement(
    val name: String,
    val isPublic: Boolean,
    val isPrivate: Boolean,
    val isProtected: Boolean,
    val isAbstract: Boolean,
    val isOverride: Boolean,
    val startOffset: Int,
    val endOffset: Int
) {
    /**
     * Represents a property in a Kotlin file.
     */
    class Property(
        name: String,
        isPublic: Boolean,
        isPrivate: Boolean,
        isProtected: Boolean,
        isAbstract: Boolean,
        isOverride: Boolean,
        val isViewModelProperty: Boolean,
        startOffset: Int,
        endOffset: Int
    ) : KotlinElement(name, isPublic, isPrivate, isProtected, isAbstract, isOverride, startOffset, endOffset)

    /**
     * Represents a function in a Kotlin file.
     */
    class Function(
        name: String,
        isPublic: Boolean,
        isPrivate: Boolean,
        isProtected: Boolean,
        isAbstract: Boolean,
        isOverride: Boolean,
        val isComposable: Boolean,
        val isContentView: Boolean,
        startOffset: Int,
        endOffset: Int
    ) : KotlinElement(name, isPublic, isPrivate, isProtected, isAbstract, isOverride, startOffset, endOffset)

    /**
     * Represents a companion object in a Kotlin file.
     */
    class CompanionObject(
        startOffset: Int,
        endOffset: Int
    ) : KotlinElement("companion", true, false, false, false, false, startOffset, endOffset)

    /**
     * Represents an init block in a Kotlin file.
     */
    class InitBlock(
        startOffset: Int,
        endOffset: Int
    ) : KotlinElement("init", false, false, false, false, false, startOffset, endOffset)

    /**
     * Represents a class declaration in a Kotlin file.
     */
    class ClassDeclaration(
        name: String,
        isPublic: Boolean,
        isPrivate: Boolean,
        isProtected: Boolean,
        val isData: Boolean,
        val isSealed: Boolean,
        val isInner: Boolean,
        startOffset: Int,
        endOffset: Int
    ) : KotlinElement(name, isPublic, isPrivate, isProtected, false, false, startOffset, endOffset)
}
