# RetroHue

[![Build with Gradle](https://github.com/walker84837/RetroHue/actions/workflows/test.yml/badge.svg)](https://github.com/walker84837/RetroHue/actions/workflows/test.yml)

> A lightweight Java library that converts Minecraft's legacy ChatColor codes into MiniMessage tags

**RetroHue** converts legacy codes (using `§` or alternative prefixes like `&`) into Adventure's MiniMessage-compatible tags or `Component` objects.

## Table of Contents

1. [Features](#features)  
2. [Installation](#installation)  
3. [Usage](#usage)  
4. [Why RetroHue?](#why-retrohue)  
5. [Roadmap](#roadmap)  
6. [License](#license)  

## Features

- **Legacy code support**  
  Converts Minecraft color codes (`0–9`, `a–f`) and format codes (`k`, `l`, `m`, `n`, `o`, `r`) into MiniMessage tags.  
- **Customizable prefix**  
  Configure any code identifier (default `§`, alternative `&`, etc.).  
- **Proper nesting & resets**  
  Automatically manages tag order and handles resets (`§r`) correctly.  
- **Convenient API**  
  - Convert an entire string to MiniMessage markup.  
  - Convert directly to an Adventure `Component`.  
  - Convert a single code (e.g. `&a`) into `Optional<NamedTextColor>`.  
- **Minimal dependencies**  
  Only requires [Adventure's MiniMessage](https://github.com/KyoriPowered/adventure) libraries.


## Usage

```java
import org.winlogon.retrohue.RetroHue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

RetroHue retroHue = new RetroHue();

// convert legacy string to MiniMessage markup
String mm = retroHue.convertToMiniMessage("§aHello §lWorld§r!");

// convert to Adventure Component
Component comp = retroHue.convertToComponent("&bWelcome &nPlayer&r!");

// convert a single color code to NamedTextColor
Optional<NamedTextColor> colorOpt = retroHue.convertColorCode("&c");
colorOpt.ifPresent(c -> System.out.println("Color: " + c.name()));
```

## Why RetroHue?

TL;DR:

* Predictable behavior
* Instantly adapt existing configs or user input that use legacy color codes
* Standardize your text processing by converting all legacy codes at once
* Use rich formatting and deserialization while preserving legacy styles

Backstory & Motivation:

While working on my plugins, I decided to add support for legacy color codes and MiniMessage tags in configuration files. Using Adventure's component serializers seemed like the most modern approach.

The process worked like this:

1. Pass a string (e.g., from a config) to the legacy component serializer, which converts legacy-formatted text into a component.
2. Use the MiniMessage parser to convert that component into a MiniMessage-formatted string.
3. Deserialize the MiniMessage string back into a component, now containing the converted color codes.

However, I ran into an issue: valid MiniMessage tags were being escaped by the deserializer. A quick fix would be to replace `\<` and `\>` with `<` and `>`, but that's brittle and relies on internal behavior.

Instead, I decided to roll my own parser to convert legacy codes to MiniMessage tags. To make this reusable, I extracted the logic into a small, modular library that I now use across all my plugins.

## Installation

Add the repository and dependency to your build:

### Repository

<details>
<summary><strong>Maven</strong></summary>

```xml
<repository>
  <id>winlogon-code</id>
  <name>winlogon's libs</name>
  <url>https://maven.winlogon.org/releases</url>
</repository>
````

</details>

<details>
<summary><strong>Gradle (Kotlin DSL)</strong></summary>

```kotlin
repositories {
    maven {
        name = "winlogonCode"
        url = uri("https://maven.winlogon.org/releases")
    }
}
```

</details>

### Dependency

<details>
<summary><strong>Maven (pom.xml)</strong></summary>

```xml
<dependency>
  <groupId>org.winlogon</groupId>
  <artifactId>retrohue</artifactId>
  <version>0.1.0</version>
</dependency>
```

</details>

<details>
<summary><strong>Gradle (Kotlin DSL)</strong></summary>

```kotlin
implementation("org.winlogon:retrohue:0.1.0")
// or `compileOnly("org.winlogon:retrohue:0.1.0")` if using Paper's dependency loader
```

</details>

## Roadmap

Pull requests are warmly welcome!

* [ ] Hex color support
  * Convert hex codes to nearest `NamedTextColor`
  * Direct hex-to-`NamedTextColor` conversion
* [ ] Exclude (and/or remove?) obfuscated color codes
* [ ] Check for number of allocations

Contributions and suggestions are welcome via GitHub issues and pull requests!

## License

Distributed under the **Apache License 2.0**. See [LICENSE](https://www.apache.org/licenses/LICENSE-2.0) for details.
