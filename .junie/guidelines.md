# Kotlin Code Sorter Development Guidelines

This document provides essential information for developers working on the Kotlin Code Sorter IntelliJ plugin.

## Build/Configuration Instructions

### Prerequisites
- JDK 21 or higher
- Gradle 8.13 or compatible version

### Building the Plugin
1. Clone the repository
2. Build the plugin using Gradle:
   ```bash
   ./gradlew build
   ```

3. Run the plugin in a development instance of IntelliJ IDEA:
   ```bash
   ./gradlew runIde
   ```

4. To build the plugin distribution:
   ```bash
   ./gradlew buildPlugin
   ```
   The plugin ZIP file will be generated in `build/distributions/`.

### K2 Compiler Support
The plugin supports the K2 compiler. To test with K2 enabled:
```bash
./gradlew runIde -Didea.kotlin.plugin.use.k2=true
```

## Testing Information

### Running Tests
1. Run all tests:
   ```bash
   ./gradlew test
   ```

2. Run a specific test class:
   ```bash
   ./gradlew test --tests "com.github.jcraane.kotlincodesorter.services.KotlinElementParserK2Test"
   ```

### Writing Tests
Tests extend `BasePlatformTestCase` from the IntelliJ Platform Test Framework. This provides access to the IntelliJ Platform test fixtures.

#### Key Components:
- `myFixture`: Provides access to the test fixture
- `project`: The test project instance

#### Example Test:
```kotlin
class MyTest : BasePlatformTestCase() {
    fun testSomething() {
        // Create test data
        val kotlinCode = """
            class MyClass {
                val property: String = "value"
            }
        """.trimIndent()

        // Configure the fixture with the test data
        val psiFile = myFixture.configureByText(KotlinFileType.INSTANCE, kotlinCode)
        
        // Perform operations and assertions
        // ...
    }
}
```

#### K2 Compiler Tests
When testing with the K2 compiler, use the `@OptIn(KaAllowAnalysisOnEdt::class)` annotation and wrap test code in `allowAnalysisOnEdt {}` block:

```kotlin
@OptIn(KaAllowAnalysisOnEdt::class)
fun testWithK2() {
    allowAnalysisOnEdt {
        // Test code here
    }
}
```

### Test Data
- Place test data files in `src/test/testData/`
- The `@TestDataPath` annotation on test classes specifies the test data path

## Additional Development Information

### Project Structure
- `src/main/kotlin/com/github/jcraane/kotlincodesorter/`: Main source code
  - `actions/`: IntelliJ actions (e.g., SortKotlinFileAction)
  - `model/`: Data models (e.g., KotlinElement)
  - `services/`: Core services (parsers, sorters, mappers)
- `src/main/resources/`: Resources
  - `META-INF/plugin.xml`: Plugin configuration
  - `messages/`: Localized messages
- `src/test/`: Test code
  - `kotlin/`: Test classes
  - `testData/`: Test data files

### Code Style
- Follow Kotlin coding conventions
- Use meaningful names for classes, methods, and variables
- Include comments for complex logic
- Write tests for new functionality

### Debugging
- Use `println("[DEBUG_LOG] message")` in tests for debugging
- Run the plugin in debug mode with:
  ```bash
  ./gradlew runIde --debug-jvm
  ```

### Plugin Development Resources
- [IntelliJ Platform Plugin SDK Documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Kotlin PSI Documentation](https://plugins.jetbrains.com/docs/intellij/kotlin.html#kotlin-psi)
- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
