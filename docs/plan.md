
# IntelliJ Plugin Plan: Custom Kotlin File Sorter

## Overview
This plugin will provide custom sorting capabilities for Kotlin files based on the specified class hierarchy and additional rules. It will integrate with IntelliJ IDEA's code formatting and arrangement features to provide a seamless experience for developers.

## Core Features

1. **Custom Sorting Action**: Add a menu action to sort Kotlin files according to the specified rules
2. **Settings Configuration**: Allow users to enable/disable specific rules or modify the sorting order
3. **Integration with Code Cleanup**: Make the sorting available as part of IntelliJ's code cleanup actions
4. **Visual Indicators**: Show warnings for code that doesn't follow the sorting rules

## Technical Architecture

### 1. Plugin Structure

```
kotlin-custom-sorter/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── com/plugin/kotlinsorter/
│   │   │   │   ├── actions/
│   │   │   │   │   └── SortKotlinFileAction.kt
│   │   │   │   ├── model/
│   │   │   │   │   ├── KotlinElement.kt
│   │   │   │   │   └── SortingRules.kt
│   │   │   │   ├── services/
│   │   │   │   │   ├── KotlinElementParser.kt
│   │   │   │   │   └── KotlinElementSorter.kt
│   │   │   │   ├── settings/
│   │   │   │   │   └── SortingSettingsComponent.kt
│   │   │   │   └── KotlinSorterPlugin.kt
│   │   ├── resources/
│   │   │   ├── META-INF/
│   │   │   │   └── plugin.xml
│   │   │   └── icons/
├── build.gradle.kts
└── README.md
```

### 2. Core Components

#### KotlinElement Model
- Abstract class representing elements in a Kotlin file
- Subclasses for different element types (properties, functions, classes, etc.)
- Each element will store its type, visibility, and other relevant attributes

#### KotlinElementParser
- Parses Kotlin files using IntelliJ's PSI (Program Structure Interface)
- Extracts elements and their attributes
- Creates a hierarchical structure of the file

#### KotlinElementSorter
- Implements the sorting algorithm based on the specified rules
- Handles the reordering of elements in the file

#### SortingRules
- Defines the sorting rules as specified in the requirements
- Provides methods to compare elements based on these rules

### 3. Sorting Algorithm

The sorting algorithm will implement the following hierarchy:

1. abstract val/var
2. public val/var
3. private val/var (viewModel at the top of the class)
4. companion object
5. init
6. override fun
7. abstract fun
8. public fun
9. protected fun
10. private fun
11. @Composable fun (ContentView() before the rest)
12. data class, sealed class, inner class

With additional rules:
- Alphabetical ordering for elements at the same level
- Public elements before private elements
- Classes (data, sealed, inner) ordered by name without specific ordering between different types

## Implementation Plan

### Phase 1: Core Functionality
1. ✅Set up the plugin project structure
2. ✅Implement the KotlinElement model and parser
3. ✅Implement the basic sorting algorithm
4. ✅Create a simple action to trigger the sorting

### Phase 2: Enhanced Features
1. ✅Add settings UI to configure sorting preferences
2. Implement integration with code cleanup
3. Add visual indicators for code that doesn't follow the rules
4. Add support for custom rule configurations

### Phase 3: Testing and Refinement
1. Write unit tests for the sorting algorithm
2. Test with various Kotlin file structures
3. Gather user feedback and refine the implementation
4. Performance optimization for large files

## Technical Challenges

1. **PSI Manipulation**: Working with IntelliJ's PSI can be complex, especially for reordering elements while preserving comments and formatting.

2. **Performance**: Sorting large files efficiently without causing UI freezes.

3. **Kotlin Language Features**: Handling all Kotlin language features correctly, including extension functions, properties with custom getters/setters, etc.

4. **User Experience**: Creating an intuitive UI for configuring sorting rules.

## Sample Implementation Details

### Sorting Logic Pseudocode

```kotlin
fun sortKotlinFile(file: KtFile): KtFile {
    // Parse the file into elements
    val elements = KotlinElementParser.parse(file)
    
    // Sort the elements
    val sortedElements = elements.sortedWith(compareBy(
        // Primary sort by element type according to hierarchy
        { element -> element.getHierarchyRank() },
        // Secondary sort alphabetically within same type
        { element -> element.name },
        // Tertiary sort by visibility (public first)
        { element -> if (element.isPublic) 0 else 1 }
    ))
    
    // Special case for viewModel properties
    moveViewModelPropertiesToTop(sortedElements)
    
    // Special case for ContentView composable
    moveContentViewComposableToTop(sortedElements)
    
    // Apply the sorted elements back to the file
    return KotlinElementSorter.applySort(file, sortedElements)
}
```

### Element Hierarchy Rank Implementation

```kotlin
fun KotlinElement.getHierarchyRank(): Int {
    return when {
        isProperty && isAbstract -> 1
        isProperty && isPublic -> 2
        isProperty && isPrivate -> 3
        isCompanionObject -> 4
        isInitBlock -> 5
        isFunction && isOverride -> 6
        isFunction && isAbstract -> 7
        isFunction && isPublic -> 8
        isFunction && isProtected -> 9
        isFunction && isPrivate -> 10
        isFunction && hasComposableAnnotation -> 11
        isClass -> 12
        else -> 13
    }
}
```

## Conclusion

This IntelliJ plugin will provide a powerful tool for maintaining consistent code organization in Kotlin files. By implementing the specified sorting rules, it will help teams maintain a standardized code style, improving readability and maintainability of their Kotlin codebase.
