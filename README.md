# Kotlin Code Sorter

![Build](https://github.com/jcraane/kotlin-code-sorter/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

An IntelliJ IDEA plugin that automatically sorts Kotlin code elements within files according to a predefined hierarchy, making your code more organized and consistent.

<!-- Plugin description -->
Kotlin Code Sorter is an IntelliJ IDEA plugin that helps you organize your Kotlin code by automatically sorting elements within files according to a predefined hierarchy. This makes your code more readable, maintainable, and consistent across your project.

The plugin sorts elements in the following order:
1. Properties (abstract, public, private)
2. Companion objects
3. Init blocks
4. Functions (override, abstract, public, protected, private, composable)
5. Class declarations (data class, sealed class, inner class)

Special cases:
- ViewModel properties are placed at the top of private properties
- ContentView composable functions are placed before other composable functions
- Elements of the same type and rank are sorted alphabetically by name
<!-- Plugin description end -->

## Features

- Sort Kotlin files with a single action (Code menu, editor popup menu, or keyboard shortcut)
- Support for both top-level elements and elements within classes
- Intelligent sorting based on element type, visibility, and special cases
- Preserves original formatting and comments
- Full support for the K2 compiler

## Usage

1. Open a Kotlin file in the editor
2. Trigger the sort action using one of these methods:
   - Select **Code > Sort Kotlin File** from the menu
   - Right-click in the editor and select **Sort Kotlin File** from the popup menu
   - Use the keyboard shortcut: **Alt+Shift+K**

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "kotlin-code-sort"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/jcraane/kotlin-code-sorter/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Building from Source

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

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
