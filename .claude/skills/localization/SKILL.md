---
name: localization
description: >
  Use this skill whenever user-visible strings are added, edited, or reviewed in Consulo.
  Trigger on: "localize", "LocalizeValue", "localizeTODO", "user string", "label text",
  "message text", "YAML localize", "LOCALIZE-LIB", "generate-localize", "Localize class",
  "raw string in UI", or any code that adds a human-readable string to a UI component,
  dialog, action, notification, or configurable. MUST be used proactively whenever
  a string literal appears in a context that will be shown to the user.
  See also: consulo-ui skill for how LocalizeValue is passed to UI components.
---

# Consulo Localization

All user-visible strings **must** be registered in a YAML localization file and accessed
via a generated `*Localize` class. Raw string literals in UI code are not allowed.

---

## Rule: No Raw Strings for User-Visible Text

```java
// ❌ Forbidden — raw string in UI
Label.create(LocalizeValue.of("Enable feature"));

// ⚠️  Temporary only — use during development, replace before merging
Label.create(LocalizeValue.localizeTODO("Enable feature"));

// ✅ Correct — string from YAML via generated class
Label.create(MyModuleLocalize.enableFeatureLabel());
```

**Exception: logging.** Log messages must use plain strings, never `LocalizeValue`:

```java
// ✅ Correct logging
LOG.warn("Cannot load file: " + path);
LOG.error("Initialization failed", e);

// ❌ Wrong — LocalizeValue in log message
LOG.warn(MyModuleLocalize.cannotLoadFile(path).get());  // don't do this
```

---

## YAML Localization File

### Location

```
src/main/resources/LOCALIZE-LIB/en_US/<fully.qualified.LocalizeClassName>.yaml
```

Example:
```
modules/base/my-feature-api/src/main/resources/LOCALIZE-LIB/en_US/consulo.my.feature.MyFeatureLocalize.yaml
```

`en_US` is the base language and is always required. Other locales are optional overlays.

### Format

```yaml
# No-argument string
enable.feature.label:
    text: Enable feature

# String with one argument (uses {0})
file.not.found.error:
    text: File ''{0}'' was not found.

# String with two arguments
operation.progress:
    text: Processing {0} of {1} items

# Mnemonic underscores are preserved (for action names)
action.hide.all.windows:
    text: Hide All _Windows
```

- Keys use dot-separated lowercase words
- Argument placeholders: `{0}`, `{1}`, … (MessageFormat syntax)
- Use `''` for a literal single-quote inside text

---

## Enabling Generation in `pom.xml`

The module's `pom.xml` must run `generate-localize` to produce the Java class:

```xml
<plugin>
    <groupId>consulo.maven</groupId>
    <artifactId>maven-consulo-plugin</artifactId>
    <extensions>true</extensions>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate-localize</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

---

## Generated Class

The plugin generates a final class under:

```
target/generated-sources/localize/<package>/localize/<ClassName>.java
```

**YAML key → Java method naming**: replace `.` with camelCase.

Example — from `consulo.my.feature.MyFeatureLocalize.yaml`:

```java
public final class MyFeatureLocalize {
    public static final String ID = "consulo.my.feature.MyFeatureLocalize";

    // enable.feature.label → enableFeatureLabel()
    public static LocalizeValue enableFeatureLabel() { ... }

    // file.not.found.error (one arg) → fileNotFoundError(Object arg0)
    public static LocalizeValue fileNotFoundError(@Nonnull Object arg0) { ... }

    // operation.progress (two args)
    public static LocalizeValue operationProgress(@Nonnull Object arg0, @Nonnull Object arg1) { ... }
}
```

---

## Using `LocalizeValue` When a Component Does Not Accept It

Some older APIs still take `String` instead of `LocalizeValue`. Call `#get()` to extract
the current-locale string:

```java
// Component or method that only accepts String:
someOldApiMethod(MyFeatureLocalize.enableFeatureLabel().get());
```

Do not store the result of `.get()` in a field — it captures a single locale snapshot and
will not update if the user changes locale.

---

## Quick Checklist

When adding a new user-visible string:

1. Open (or create) the YAML file at `LOCALIZE-LIB/en_US/<Package>.<ClassName>.yaml`
2. Add the key + text entry
3. Ensure `generate-localize` goal is in `pom.xml` (check if sibling keys already work — if so, it's already configured)
4. Use the generated method: `MyModuleLocalize.myNewKey()`
5. For temporary strings during prototyping: `LocalizeValue.localizeTODO("text")` — but replace before the final commit
