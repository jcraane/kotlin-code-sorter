package com.github.jcraane.kotlincodesorter.model

/**
 * Model representing a Kotlin element in a file.
 * Used for sorting and organizing code.
 */
sealed class KotlinElement {
    /**
     * The name of the element.
     */
    abstract val name: String

    /**
     * The start offset of the element in the file.
     */
    abstract val startOffset: Int

    /**
     * The end offset of the element in the file.
     */
    abstract val endOffset: Int

    /**
     * Represents a property in a Kotlin file.
     */
    data class Property(
        override val name: String,
        val isPrivate: Boolean,
        val isPublic: Boolean,
        val isAbstract: Boolean,
        val isViewModelProperty: Boolean,
        override val startOffset: Int,
        override val endOffset: Int
    ) : KotlinElement()

    /**
     * Represents a function in a Kotlin file.
     */
    data class Function(
        override val name: String,
        val isPrivate: Boolean,
        val isPublic: Boolean,
        val isProtected: Boolean,
        val isAbstract: Boolean,
        val isOverride: Boolean,
        val isComposable: Boolean,
        val isContentView: Boolean,
        override val startOffset: Int,
        override val endOffset: Int
    ) : KotlinElement()

    /**
     * Represents a class declaration in a Kotlin file.
     */
    data class ClassDeclaration(
        override val name: String,
        val isDataClass: Boolean,
        val isSealedClass: Boolean,
        val isInnerClass: Boolean,
        override val startOffset: Int,
        override val endOffset: Int
    ) : KotlinElement()

    /**
     * Represents a companion object in a Kotlin file.
     */
    data class CompanionObject(
        override val name: String,
        override val startOffset: Int,
        override val endOffset: Int
    ) : KotlinElement()

    /**
     * Represents an init block in a Kotlin file.
     */
    data class InitBlock(
        override val startOffset: Int,
        override val endOffset: Int
    ) : KotlinElement() {
        override val name: String = "init"
    }
}
