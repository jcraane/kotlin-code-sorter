package com.github.jcraane.kotlincodesorter

import com.github.jcraane.kotlincodesorter.model.KotlinElement
import com.github.jcraane.kotlincodesorter.services.KotlinElementParser
import com.github.jcraane.kotlincodesorter.services.KotlinElementSorter
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import com.github.jcraane.kotlincodesorter.services.MyProjectService
import org.jetbrains.kotlin.idea.KotlinFileType

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testXMLFile() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>")
        val xmlFile = assertInstanceOf(psiFile, XmlFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, xmlFile.virtualFile))

        assertNotNull(xmlFile.rootTag)

        xmlFile.rootTag?.let {
            assertEquals("foo", it.name)
            assertEquals("bar", it.value.text)
        }
    }

    fun testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
    }

    fun testProjectService() {
        val projectService = project.service<MyProjectService>()

        assertNotSame(projectService.getRandomNumber(), projectService.getRandomNumber())
    }

    override fun getTestDataPath() = "src/test/testData/rename"

    fun testSortKotlinClass() {
        // Configure the fixture with our test class
        val kotlinCode = """
            class MyClass {
                companion object {

                }

                private val test: String = ""

                data class Test(val s: String) {

                }

                val publiek: String = "hallo"
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText(KotlinFileType.INSTANCE, kotlinCode)

        // Create an instance of KotlinElementSorter
        val sorter = KotlinElementSorter()

        // Sort the file
        val success = sorter.sortFile(project, psiFile)

        // Verify that sorting was successful
        assertTrue("Sorting should be successful", success)

        // Get the sorted text
        val sortedText = psiFile.text

        // Verify that the sorted text has the correct structure
        // Public properties should come before private properties
        val publicPropertyIndex = sortedText.indexOf("val publiek: String = \"hallo\"")
        val privatePropertyIndex = sortedText.indexOf("private val test: String = \"\"")
        assertTrue("Public property should come before private property",
                   publicPropertyIndex < privatePropertyIndex)

        // Companion object should come after properties
        val companionObjectIndex = sortedText.indexOf("companion object")
        assertTrue("Companion object should come after properties",
                   companionObjectIndex > privatePropertyIndex)

        // Data class should come after companion object
        val dataClassIndex = sortedText.indexOf("data class Test")
        assertTrue("Data class should come after companion object",
                   dataClassIndex > companionObjectIndex)

        // Print the sorted text for debugging
        println("[DEBUG_LOG] Sorted text:\n$sortedText")
    }

    fun testComplexKotlinCodeSorting() {
        // Configure the fixture with an unsorted complex class
        val kotlinCode = """
        class ComplexClass {
            abstract val abstractProperty: String

            public val publicProperty: String = "public"

            private val viewModel: ViewModel = ViewModel()

            private val zPrivateProperty: String = "private"

            companion object {
                val companionProp: String = "test"

                fun companionFunction() {}
            }

            init {
                // Initialization code
            }

            override fun toString(): String = "ComplexClass"

            abstract fun bAbstract()

            @Composable
            fun ContentView() {
            }

            public fun bPublic() {}

            public fun cPublic() {}

            protected fun aProtected() {}

            private fun zPrivate() {}

            data class ADataClass(val name: String)

            inner class BInnerClass {}

            sealed class CSealed {
                object One : CSealed()
                object Two : CSealed()
            }
        }

        abstract class AbstractExample {
            abstract val abstractProperty: String

            abstract fun abstractFunction()
        }
    """.trimIndent()

        val psiFile = myFixture.configureByText(KotlinFileType.INSTANCE, kotlinCode)

        // Create an instance of KotlinElementSorter
        val sorter = KotlinElementSorter()

        // Sort the file
        val success = sorter.sortFile(project, psiFile)

        // Verify that sorting was successful
        assertTrue("Sorting should be successful", success)

        // Get the sorted text
        val sortedText = psiFile.text

        // Print the sorted text for debugging
        println("[DEBUG_LOG] Sorted text:\n$sortedText")

        // Verify that elements are sorted according to the rules:

        // 1. Abstract properties should be first
        val abstractPropertyIndex = sortedText.indexOf("abstract val abstractProperty: String")

        // 2. Public properties should be next
        val publicPropertyIndex = sortedText.indexOf("public val publicProperty: String = \"public\"")

        // 3. Private viewModel property should be next (at the top of private properties)
        val viewModelIndex = sortedText.indexOf("private val viewModel: ViewModel = ViewModel()")

        // 4. Other private properties (alphabetically)
        val privatePropertyIndex = sortedText.indexOf("private val zPrivateProperty: String = \"private\"")

        // 5. Companion object should be after properties
        val companionObjectIndex = sortedText.indexOf("companion object {")

        // 6. Init block should be after companion
        val initIndex = sortedText.indexOf("init {")

        // 7. @Composable function (ContentView) should be before other functions
        val composableFunIndex = sortedText.indexOf("@Composable")

        // 8. Override methods
        val overrideFunIndex = sortedText.indexOf("override fun toString()")

        // 9. Abstract functions
        val abstractFunIndex = sortedText.indexOf("abstract fun bAbstract()")

        // 10. Public functions (alphabetically)
        val publicFunBIndex = sortedText.indexOf("public fun bPublic()")
        val publicFunCIndex = sortedText.indexOf("public fun cPublic()")

        // 11. Protected functions
        val protectedFunIndex = sortedText.indexOf("protected fun aProtected()")

        // 12. Private functions
        val privateFunIndex = sortedText.indexOf("private fun zPrivate()")

        // 13. Classes should be at the end ordered by name
        val dataClassIndex = sortedText.indexOf("data class ADataClass")
        val innerClassIndex = sortedText.indexOf("inner class BInnerClass")
        val sealedClassIndex = sortedText.indexOf("sealed class CSealed")

        // Verify property ordering
        assertTrue("Abstract property should come before public property",
            abstractPropertyIndex < publicPropertyIndex)
        assertTrue("Public property should come before private viewModel",
            publicPropertyIndex < viewModelIndex)
        assertTrue("viewModel should be the first private property",
            viewModelIndex < privatePropertyIndex)

        // Verify properties before companion
        assertTrue("Properties should come before companion object",
            privatePropertyIndex < companionObjectIndex)

        // Verify companion before init
        assertTrue("Companion object should come before init block",
            companionObjectIndex < initIndex)

        // Verify functions ordering
        assertTrue("@Composable function should come after other functions",
            composableFunIndex > overrideFunIndex)
        assertTrue("Override functions should come before abstract functions",
            overrideFunIndex < abstractFunIndex)
        assertTrue("Abstract functions should come before public functions",
            abstractFunIndex < publicFunBIndex)

        // Verify public function alphabetical ordering
        assertTrue("Public functions should be ordered alphabetically",
            publicFunBIndex < publicFunCIndex)

        // Verify public functions before protected functions
        assertTrue("Public functions should come before protected functions",
            publicFunCIndex < protectedFunIndex)

        // Verify protected functions before private functions
        assertTrue("Protected functions should come before private functions",
            protectedFunIndex < privateFunIndex)

        // Verify classes order (alphabetically by name, regardless of type)
        assertTrue("Classes should come after functions",
            privateFunIndex < dataClassIndex)
        assertTrue("Classes should be ordered alphabetically by name",
            dataClassIndex < innerClassIndex && innerClassIndex < sealedClassIndex)
    }

    fun testComposableFunction() {
        val kotlinCode = """
            class ClassWithComposable {
                @Composable
                fun ContentView() {
                }

                public fun bPublic() {}

                private fun test() {

                }
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText(KotlinFileType.INSTANCE, kotlinCode)

        val sorter = KotlinElementSorter()
        val success = sorter.sortFile(project, psiFile)
        assertTrue("Sorting should be successful", success)

        val sortedText = psiFile.text
        println(sortedText)

        val composableFunIndex = sortedText.indexOf("@Composable")
        val publicFunIndex = sortedText.indexOf("public")
        val privateFunIndex = sortedText.indexOf("private")

        assertTrue("@Composable function should come ater other functions",
            composableFunIndex > publicFunIndex)
        assertTrue("Public functions should come before private functions",
            publicFunIndex < privateFunIndex)
    }
}
