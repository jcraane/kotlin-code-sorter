package com.github.jcraane.kotlincodesorter.settings

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.github.jcraane.kotlincodesorter.model.SortingRules
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Test

class SortingSettingsTest : BasePlatformTestCase() {

    private lateinit var settingsService: SortingSettingsService

    override fun setUp() {
        super.setUp()
        // Initialize the settings service
        settingsService = SortingSettingsService()
        // Set the settings service in SortingRules
        SortingRules.settingsService = settingsService
        // Reset settings to default before each test
        settingsService.updateSortingRuleOrder(SortingRuleType.defaultOrder())
    }

    override fun tearDown() {
        // Reset settings to default after each test
        settingsService.updateSortingRuleOrder(SortingRuleType.defaultOrder())
        // Reset the settings service in SortingRules
        SortingRules.resetSettingsService()
        super.tearDown()
    }

    fun testDefaultSortingOrder() {
        // Create test elements
        val abstractProperty = createProperty("abstractProp", isAbstract = true)
        val publicProperty = createProperty("publicProp", isPublic = true)
        val privateProperty = createProperty("privateProp", isPrivate = true)
        val companionObject = createCompanionObject("Companion")
        val initBlock = createInitBlock()
        val overrideFunction = createFunction("overrideFunc", isOverride = true)
        val abstractFunction = createFunction("abstractFunc", isAbstract = true)
        val publicFunction = createFunction("publicFunc", isPublic = true)
        val protectedFunction = createFunction("protectedFunc", isProtected = true)
        val privateFunction = createFunction("privateFunc", isPrivate = true)
        val composableFunction = createFunction("composableFunc", isComposable = true)
        val classDeclaration = createClassDeclaration("TestClass")

        // Get ranks with default order
        val abstractPropertyRank = SortingRules.getElementRank(abstractProperty)
        val publicPropertyRank = SortingRules.getElementRank(publicProperty)
        val privatePropertyRank = SortingRules.getElementRank(privateProperty)
        val companionObjectRank = SortingRules.getElementRank(companionObject)
        val initBlockRank = SortingRules.getElementRank(initBlock)
        val overrideFunctionRank = SortingRules.getElementRank(overrideFunction)
        val abstractFunctionRank = SortingRules.getElementRank(abstractFunction)
        val publicFunctionRank = SortingRules.getElementRank(publicFunction)
        val protectedFunctionRank = SortingRules.getElementRank(protectedFunction)
        val privateFunctionRank = SortingRules.getElementRank(privateFunction)
        val composableFunctionRank = SortingRules.getElementRank(composableFunction)
        val classDeclarationRank = SortingRules.getElementRank(classDeclaration)

        // Verify default order
        assertTrue(abstractPropertyRank < publicPropertyRank)
        assertTrue(publicPropertyRank < privatePropertyRank)
        assertTrue(privatePropertyRank < companionObjectRank)
        assertTrue(companionObjectRank < initBlockRank)
        assertTrue(initBlockRank < overrideFunctionRank)
        assertTrue(overrideFunctionRank < abstractFunctionRank)
        assertTrue(abstractFunctionRank < publicFunctionRank)
        assertTrue(publicFunctionRank < protectedFunctionRank)
        assertTrue(protectedFunctionRank < privateFunctionRank)
        assertTrue(privateFunctionRank < composableFunctionRank)
        assertTrue(composableFunctionRank < classDeclarationRank)
    }

    fun testCustomSortingOrder() {
        // Create a custom order with functions first, then properties
        val customOrder = listOf(
            SortingRuleType.OVERRIDE_FUNCTION,
            SortingRuleType.ABSTRACT_FUNCTION,
            SortingRuleType.PUBLIC_FUNCTION,
            SortingRuleType.PROTECTED_FUNCTION,
            SortingRuleType.PRIVATE_FUNCTION,
            SortingRuleType.COMPOSABLE_FUNCTION,
            SortingRuleType.ABSTRACT_PROPERTY,
            SortingRuleType.PUBLIC_PROPERTY,
            SortingRuleType.PRIVATE_PROPERTY,
            SortingRuleType.COMPANION_OBJECT,
            SortingRuleType.INIT_BLOCK,
            SortingRuleType.CLASS_DECLARATION
        )

        // Update settings with custom order
        settingsService.updateSortingRuleOrder(customOrder)

        // Create test elements
        val abstractProperty = createProperty("abstractProp", isAbstract = true)
        val publicProperty = createProperty("publicProp", isPublic = true)
        val privateProperty = createProperty("privateProp", isPrivate = true)
        val companionObject = createCompanionObject("Companion")
        val initBlock = createInitBlock()
        val overrideFunction = createFunction("overrideFunc", isOverride = true)
        val abstractFunction = createFunction("abstractFunc", isAbstract = true)
        val publicFunction = createFunction("publicFunc", isPublic = true)
        val protectedFunction = createFunction("protectedFunc", isProtected = true)
        val privateFunction = createFunction("privateFunc", isPrivate = true)
        val composableFunction = createFunction("composableFunc", isComposable = true)
        val classDeclaration = createClassDeclaration("TestClass")

        // Get ranks with custom order
        val abstractPropertyRank = SortingRules.getElementRank(abstractProperty)
        val publicPropertyRank = SortingRules.getElementRank(publicProperty)
        val privatePropertyRank = SortingRules.getElementRank(privateProperty)
        val companionObjectRank = SortingRules.getElementRank(companionObject)
        val initBlockRank = SortingRules.getElementRank(initBlock)
        val overrideFunctionRank = SortingRules.getElementRank(overrideFunction)
        val abstractFunctionRank = SortingRules.getElementRank(abstractFunction)
        val publicFunctionRank = SortingRules.getElementRank(publicFunction)
        val protectedFunctionRank = SortingRules.getElementRank(protectedFunction)
        val privateFunctionRank = SortingRules.getElementRank(privateFunction)
        val composableFunctionRank = SortingRules.getElementRank(composableFunction)
        val classDeclarationRank = SortingRules.getElementRank(classDeclaration)

        // Verify custom order
        assertTrue(overrideFunctionRank < abstractFunctionRank)
        assertTrue(abstractFunctionRank < publicFunctionRank)
        assertTrue(publicFunctionRank < protectedFunctionRank)
        assertTrue(protectedFunctionRank < privateFunctionRank)
        assertTrue(privateFunctionRank < composableFunctionRank)
        assertTrue(composableFunctionRank < abstractPropertyRank)
        assertTrue(abstractPropertyRank < publicPropertyRank)
        assertTrue(publicPropertyRank < privatePropertyRank)
        assertTrue(privatePropertyRank < companionObjectRank)
        assertTrue(companionObjectRank < initBlockRank)
        assertTrue(initBlockRank < classDeclarationRank)
    }

    // Helper methods to create test elements
    private fun createProperty(
        name: String,
        isPrivate: Boolean = false,
        isPublic: Boolean = false,
        isAbstract: Boolean = false,
        isViewModelProperty: Boolean = false
    ): KotlinElement.Property {
        return KotlinElement.Property(
            name = name,
            isPrivate = isPrivate,
            isPublic = isPublic,
            isAbstract = isAbstract,
            isViewModelProperty = isViewModelProperty,
            startOffset = 0,
            endOffset = 0
        )
    }

    private fun createFunction(
        name: String,
        isPrivate: Boolean = false,
        isPublic: Boolean = false,
        isProtected: Boolean = false,
        isAbstract: Boolean = false,
        isOverride: Boolean = false,
        isComposable: Boolean = false,
        isContentView: Boolean = false
    ): KotlinElement.Function {
        return KotlinElement.Function(
            name = name,
            isPrivate = isPrivate,
            isPublic = isPublic,
            isProtected = isProtected,
            isAbstract = isAbstract,
            isOverride = isOverride,
            isComposable = isComposable,
            isContentView = isContentView,
            startOffset = 0,
            endOffset = 0
        )
    }

    private fun createCompanionObject(name: String): KotlinElement.CompanionObject {
        return KotlinElement.CompanionObject(
            name = name,
            startOffset = 0,
            endOffset = 0
        )
    }

    private fun createInitBlock(): KotlinElement.InitBlock {
        return KotlinElement.InitBlock(
            startOffset = 0,
            endOffset = 0
        )
    }

    private fun createClassDeclaration(
        name: String,
        isDataClass: Boolean = false,
        isSealedClass: Boolean = false,
        isInnerClass: Boolean = false
    ): KotlinElement.ClassDeclaration {
        return KotlinElement.ClassDeclaration(
            name = name,
            isDataClass = isDataClass,
            isSealedClass = isSealedClass,
            isInnerClass = isInnerClass,
            startOffset = 0,
            endOffset = 0
        )
    }
}
