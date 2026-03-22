---
name: module-registration
description: >
  Use this skill whenever a new module is created or a new dependency needs to be added to the
  Consulo platform. Trigger on: "add module", "new module", "create module", "register module",
  "add dependency", "add to classpath", "module checklist", "add to distribution", "add to sandbox",
  or any request that involves introducing a new Maven module into the project.
  MUST be used proactively whenever the user creates a new module directory or pom.xml.
---

# Module Registration Checklist

When a new module is added (or a new dependency introduced into the platform), **all of the
following steps are required** — missing any one of them will cause build or runtime failures.

---

## 1. Root `pom.xml` — register the module

Add a `<module>` entry to the root `/pom.xml`:

```xml
<module>modules/base/my-new-module</module>
```

Place it in the correct alphabetical/logical group alongside similar modules.

---

## 2. Sandbox classpath — all three sandboxes (except sand-lang)

Add a `<dependency>` in **each** of the three sandbox `pom.xml` files:

- `sandbox/desktop-awt/pom.xml`
- `sandbox/desktop-swt/pom.xml`
- `sandbox/web/pom.xml`

**Do NOT add to** `sandbox/sand-language-plugin/pom.xml`.

```xml
<dependency>
    <groupId>${project.groupId}</groupId>
    <artifactId>consulo-my-new-module</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

## 3. Distribution `pom.xml` — include in the build

Add a `<dependency>` in `distribution/pom.xml`:

```xml
<dependency>
    <groupId>${project.groupId}</groupId>
    <artifactId>consulo-my-new-module</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

## 4. Assembly descriptor — register in the module list

Add an `<include>` entry in the appropriate assembly XML under `distribution/src/`:

| Module scope | File to edit |
|---|---|
| Core platform (most modules) | `platform.base.xml` **and** `platform.base_mac.xml` |
| Desktop AWT only | `platform.desktop.awt.xml` |
| Desktop SWT only | `platform.desktop.swt.xml` |
| Web only | `platform.web.xml` |

> ⚠️ **Important**: `platform.base.xml` and `platform.base_mac.xml` must always be updated
> together — the file itself contains a reminder comment to this effect.

```xml
<include>consulo:consulo-my-new-module</include>
```

Add it inside the appropriate `<dependencySet>` → `<includes>` block, grouped with
similar modules.

---

## 5. `module-info.java` — required for Java module system

Create `src/main/java/module-info.java` in the new module. This is required for **all**
modules — including those whose parent arch is `arch.managment-low-java`.

Module name convention: replace `-` with `.` in the artifactId and drop the `consulo-` prefix
becomes the module name prefix `consulo.`:

| artifactId | module name |
|---|---|
| `consulo-my-new-module` | `consulo.my.new.module` |

Typical structure:

```java
module consulo.my.new.module {
    requires consulo.some.api;
    requires consulo.other.api;

    exports consulo.myModule;
    exports consulo.myModule.internal to
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl;
}
```

Rules:
- All parameters/fields are non-null by default — do not add `@Nonnull`
- Use `requires transitive` only for APIs that this module re-exports to callers
- Qualified exports (`exports ... to`) for internal packages restricted to known consumers
- `provides ... with` for service implementations (SPI)
- `uses` for service interfaces consumed
