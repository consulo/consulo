---
name: consulo-ui
description: >
  Use this skill whenever UI code is being written, reviewed, or refactored in Consulo.
  Trigger on: "UI", "component", "label", "button", "checkbox", "layout", "dialog", "panel",
  "swing", "AWT", "SWT", "JComponent", "JPanel", "JLabel", "JButton", "VerticalLayout",
  "HorizontalLayout", "DockLayout", "CheckBox", "TextBox", "ComboBox", "RequiredUIAccess",
  "UIAccess", "LocalizeValue", or any request involving user interface code.
  MUST be used proactively whenever any UI component is created or modified.
---

# Consulo UI Framework

Consulo has its own unified UI framework (`consulo.ui` package) that works across all
frontends (Desktop AWT, Desktop SWT, Web). **Always use this unified API** — not AWT/Swing
directly — except inside designated frontend implementation modules.

---

## Where Each API Is Allowed

| Module type | Unified UI (`consulo.ui.*`) | AWT / SWT / Vaadin |
|---|---|---|
| API modules (`*-api`) | ✅ required | ❌ forbidden |
| Impl modules (`*-impl`) | ✅ required | ❌ forbidden |
| Frontend modules (see below) | ✅ preferred | ✅ allowed when needed |

**Frontend modules** (the only place AWT/SWT/Vaadin is allowed):
- `modules/desktop-awt/desktop-awt-ide-impl`
- `modules/desktop-swt/desktop-swt-ide-impl`
- `modules/web/web-ide-impl` (Vaadin)

> Old code scattered across the codebase still uses AWT directly — this is legacy and not
> preferred. Do not copy those patterns; do not add new AWT imports to non-frontend modules.

---

## Component Creation

All components are created via static factory methods on the interface itself. Each factory
delegates to `UIInternal.get()` which routes to the active platform implementation.

```java
// Text
Label label = Label.create(LocalizeValue.localizeTODO("Some text"));
HtmlLabel html = HtmlLabel.create(LocalizeValue.localizeTODO("<b>Bold</b>"));
Hyperlink link = Hyperlink.create(LocalizeValue.localizeTODO("Click here"));

// Input
Button button = Button.create(LocalizeValue.localizeTODO("OK"));
CheckBox cb = CheckBox.create(LocalizeValue.localizeTODO("Enable feature"));
TextBox text = TextBox.create("initial value");
IntBox intBox = IntBox.create();
ComboBox<MyEnum> combo = ComboBox.create(model);

// Toggles
RadioButton<MyValue> radio = RadioButton.create(LocalizeValue.localizeTODO("Option A"), value);
ToggleSwitch toggle = ToggleSwitch.create();
```

---

## Layouts

```java
// Stack components top-to-bottom
VerticalLayout root = VerticalLayout.create();
root.add(label);
root.add(button);

// Stack components left-to-right
HorizontalLayout row = HorizontalLayout.create(5); // 5px gap

// Dock to edges: left/right/top/bottom/center
DockLayout dock = DockLayout.create();
dock.left(checkbox).right(intBox);

// Label + component pair
LabeledLayout labeled = LabeledLayout.create(LocalizeValue.localizeTODO("Name:"), textBox);

// Tabs
TabbedLayout tabs = TabbedLayout.create();
tabs.addTab(LocalizeValue.localizeTODO("General"), generalPanel);

// Split pane
TwoComponentSplitLayout split = TwoComponentSplitLayout.createTopBottom(top, bottom);

// Scrollable container
ScrollableLayout scroll = ScrollableLayout.create(inner, true, true);

// Collapsible section
FoldoutLayout foldout = FoldoutLayout.create(LocalizeValue.localizeTODO("Advanced"), inner);
```

---

## Text — Always Use `LocalizeValue`

**Never use raw `String` for user-visible text.** All text in UI components must be wrapped
in `LocalizeValue`:

| Method | When to use |
|---|---|
| `LocalizeValue.localizeTODO("text")` | Temporary — not yet extracted to a localize class; acceptable for new code during development |
| `MyModuleLocalize.someKey()` | Proper — text is externalized in a generated localize class |
| `LocalizeValue.empty()` | Empty/no text |

---

## UI Thread — `@RequiredUIAccess` and `UIAccess`

- Annotate every method that reads or writes UI state with `@RequiredUIAccess`
- Use `UIAccess` to schedule work onto the UI thread from a background thread:

```java
@RequiredUIAccess
protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
    VerticalLayout root = VerticalLayout.create();
    // ...
    return root;
}
```

```java
// From a background thread, run something on the UI thread:
UIAccess uiAccess = ...; // obtained from Application or passed in
uiAccess.give(() -> {
    label.setText(LocalizeValue.localizeTODO("Done"));
});
```

---

## Styling

```java
// Pre-defined text styles
label.setTextAttribute(TextAttribute.REGULAR);
label.setTextAttribute(TextAttribute.REGULAR_BOLD);
label.setTextAttribute(TextAttribute.ERROR);       // red text
label.setTextAttribute(TextAttribute.GRAYED);      // disabled-looking
label.setTextAttribute(TextAttribute.GRAY);

// Colors (use semantic names, not raw RGB)
// StandardColors: WHITE, BLACK, RED, GREEN, BLUE, GRAY, ORANGE, …
// ComponentColors: TEXT_FOREGROUND, BORDER, LAYOUT, DISABLED_TEXT
```

---

## Real-World Pattern

```java
@RequiredUIAccess
protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
    VerticalLayout root = VerticalLayout.create();

    CheckBox enableBox = CheckBox.create(ApplicationLocalize.checkboxCaretBlinkingMs());
    propertyBuilder.add(enableBox, settings::isEnabled, settings::setEnabled);

    IntBox delayBox = IntBox.create();
    propertyBuilder.add(delayBox, settings::getDelay, settings::setDelay);
    enableBox.addValueListener(event -> delayBox.setEnabled(event.getValue()));

    root.add(DockLayout.create().left(enableBox).right(delayBox));
    return root;
}
```

---

## What NOT to Do

```java
// ❌ AWT in a non-frontend module
JPanel panel = new JPanel();
panel.add(new JLabel("text"));

// ❌ Raw string in a UI component
Label.create(LocalizeValue.of("static non-localizable text")); // only for truly non-localizable
Button.create(LocalizeValue.localizeTODO("OK"));  // ✅ correct during development

// ❌ UI manipulation off the UI thread without UIAccess
label.setText(value); // must be on EDT / called with @RequiredUIAccess
```
