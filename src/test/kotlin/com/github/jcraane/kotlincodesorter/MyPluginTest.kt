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

    fun testParseKotlinClass() {
        // Configure the fixture with our example class
        val kotlinCode = """
            class MyClass {
                companion object {

                }

                private val test: String = ""
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText(KotlinFileType.INSTANCE, kotlinCode)

        // Create an instance of KotlinElementParser
        val parser = KotlinElementParser()

        // Parse the file
        val elements = parser.parse(psiFile)

        // Verify that elements are found
        assertFalse("Elements should be found in the file", elements.isEmpty())

        // Verify that the companion object is found
        val companionObjects = elements.filterIsInstance<KotlinElement.CompanionObject>()
        assertEquals("One companion object should be found", 1, companionObjects.size)

        // Verify that the property is found
        val properties = elements.filterIsInstance<KotlinElement.Property>()
        assertEquals("One property should be found", 1, properties.size)
        assertEquals("Property name should be 'test'", "test", properties[0].name)
        assertTrue("Property should be private", properties[0].isPrivate)
    }

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
}
