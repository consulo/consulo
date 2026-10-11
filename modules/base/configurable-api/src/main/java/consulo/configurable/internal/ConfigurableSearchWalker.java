/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.configurable.internal;

import consulo.application.Application;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.RadioButton;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.util.TextWithMnemonic;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public final class ConfigurableSearchWalker {
    private ConfigurableSearchWalker() {
    }

    @RequiredUIAccess
    public static ConfigurableSearchHits findHits(Component root, ConfigurableSearchTextMatcher matcher) {
        ConfigurableSearchHits strict = new ConfigurableSearchHits();
        visit(root, value -> matches(value, matcher, true), strict);
        if (!strict.isEmpty()) {
            return strict;
        }

        ConfigurableSearchHits loose = new ConfigurableSearchHits();
        visit(root, value -> matches(value, matcher, false), loose);
        return loose;
    }

    @RequiredUIAccess
    public static void collectTexts(Component root, BiConsumer<LocalizeValue, List<LocalizeValue>> consumer) {
        collect(root, List.of(), consumer);
    }

    public static List<String> textsOf(LocalizeValue value) {
        List<String> texts = new ArrayList<>(2);
        addText(texts, value.get());

        Optional<LocalizeKey> key = value.getKey();
        if (key.isPresent()) {
            LocalizeManager manager = LocalizeManager.get();
            addText(texts, manager.getUnformattedText(key.get(), manager.getDefaultLocale()));
        }
        return texts;
    }

    private static void addText(List<String> texts, @Nullable String text) {
        if (text == null) {
            return;
        }

        String plain = TextWithMnemonic.parse(text).getText();
        if (!plain.isBlank() && !texts.contains(plain)) {
            texts.add(plain);
        }
    }

    private static boolean matches(LocalizeValue value, ConfigurableSearchTextMatcher matcher, boolean force) {
        for (String text : textsOf(value)) {
            if (matcher.matches(text, force)) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    private static void visit(Component component, Predicate<LocalizeValue> matcher, ConfigurableSearchHits hits) {
        if (component.isVisible()) {
            visitShown(component, matcher, hits);
        }
    }

    @RequiredUIAccess
    private static void visitShown(Component component, Predicate<LocalizeValue> matcher, ConfigurableSearchHits hits) {
        if (component instanceof Label label) {
            if (matcher.test(label.getText())) {
                hits.addTarget(label);

                Component target = label.getTarget();
                if (target != null) {
                    hits.addTarget(target);
                }
            }
        }
        else if (component instanceof CheckBox checkBox) {
            lightIfMatches(checkBox, checkBox.getLabelText(), matcher, hits);
        }
        else if (component instanceof RadioButton radioButton) {
            lightIfMatches(radioButton, radioButton.getLabelText(), matcher, hits);
        }
        else if (component instanceof Button button) {
            lightIfMatches(button, button.getText(), matcher, hits);
        }
        else if (component instanceof Hyperlink hyperlink) {
            lightIfMatches(hyperlink, hyperlink.getText(), matcher, hits);
        }
        else if (component instanceof TabbedLayout tabbedLayout) {
            visitTabs(tabbedLayout, matcher, hits);
        }
        else if (component instanceof LabeledLayout labeledLayout) {
            visitSection(labeledLayout, labeledLayout.getLabel(), matcher, hits);
        }
        else if (component instanceof FoldoutLayout foldoutLayout) {
            visitSection(foldoutLayout, foldoutLayout.getTitle(), matcher, hits);
        }
        else if (component instanceof Layout<?> layout) {
            forEachChild(layout, child -> visit(child, matcher, hits));
        }
        else {
            Application.get().getInstance(ConfigurableForeignComponentSearch.class).findHits(component, matcher, hits);
        }
    }

    private static void lightIfMatches(
        Component component,
        LocalizeValue text,
        Predicate<LocalizeValue> matcher,
        ConfigurableSearchHits hits
    ) {
        if (matcher.test(text)) {
            hits.addTarget(component);
        }
    }

    @RequiredUIAccess
    private static void visitSection(
        Layout<?> section,
        LocalizeValue title,
        Predicate<LocalizeValue> matcher,
        ConfigurableSearchHits hits
    ) {
        ConfigurableSearchHits inner = new ConfigurableSearchHits();
        forEachChild(section, child -> visit(child, matcher, inner));

        if (!inner.isEmpty()) {
            hits.addAll(inner);
        }
        else if (matcher.test(title)) {
            hits.addTarget(section);
        }
    }

    @RequiredUIAccess
    private static void visitTabs(TabbedLayout tabbedLayout, Predicate<LocalizeValue> matcher, ConfigurableSearchHits hits) {
        Map<Tab, ConfigurableSearchHits> contents = new LinkedHashMap<>();
        Set<Tab> titleHits = new HashSet<>();

        tabbedLayout.forEachTab((tab, content) -> {
            ConfigurableSearchHits inner = new ConfigurableSearchHits();
            visitShown(content, matcher, inner);
            contents.put(tab, inner);

            if (matcher.test(tab.getTitle())) {
                titleHits.add(tab);
            }
        });

        Tab selected = tabbedLayout.getSelectedTab();
        Tab shown = selected;
        if (shown == null || !hasHit(shown, contents, titleHits)) {
            for (Tab tab : contents.keySet()) {
                if (hasHit(tab, contents, titleHits)) {
                    shown = tab;
                    break;
                }
            }
        }

        if (shown != null && shown != selected) {
            hits.addTabToSelect(shown);
        }

        for (Map.Entry<Tab, ConfigurableSearchHits> entry : contents.entrySet()) {
            Tab tab = entry.getKey();
            ConfigurableSearchHits inner = entry.getValue();

            if (tab == shown) {
                hits.addAll(inner);
            }

            if (titleHits.contains(tab) || tab != shown && !inner.isEmpty()) {
                hits.addTabTarget(tab);
            }
        }
    }

    private static boolean hasHit(Tab tab, Map<Tab, ConfigurableSearchHits> contents, Set<Tab> titleHits) {
        ConfigurableSearchHits inner = contents.get(tab);
        return titleHits.contains(tab) || inner != null && !inner.isEmpty();
    }

    @RequiredUIAccess
    private static void collect(Component component, List<LocalizeValue> tabPath, BiConsumer<LocalizeValue, List<LocalizeValue>> consumer) {
        if (component instanceof Label label) {
            accept(label.getText(), tabPath, consumer);
        }
        else if (component instanceof CheckBox checkBox) {
            accept(checkBox.getLabelText(), tabPath, consumer);
        }
        else if (component instanceof RadioButton radioButton) {
            accept(radioButton.getLabelText(), tabPath, consumer);
        }
        else if (component instanceof Button button) {
            accept(button.getText(), tabPath, consumer);
        }
        else if (component instanceof Hyperlink hyperlink) {
            accept(hyperlink.getText(), tabPath, consumer);
        }
        else if (component instanceof TabbedLayout tabbedLayout) {
            tabbedLayout.forEachTab((tab, content) -> {
                LocalizeValue title = tab.getTitle();
                accept(title, tabPath, consumer);

                List<LocalizeValue> innerPath = new ArrayList<>(tabPath);
                innerPath.add(title);
                collect(content, List.copyOf(innerPath), consumer);
            });
        }
        else if (component instanceof Layout<?> layout) {
            if (layout instanceof LabeledLayout labeledLayout) {
                accept(labeledLayout.getLabel(), tabPath, consumer);
            }
            else if (layout instanceof FoldoutLayout foldoutLayout) {
                accept(foldoutLayout.getTitle(), tabPath, consumer);
            }

            forEachChild(layout, child -> collect(child, tabPath, consumer));
        }
        else {
            Application.get().getInstance(ConfigurableForeignComponentSearch.class).collectTexts(component, tabPath, consumer);
        }
    }

    private static void accept(LocalizeValue value, List<LocalizeValue> tabPath, BiConsumer<LocalizeValue, List<LocalizeValue>> consumer) {
        if (value != LocalizeValue.empty() && !value.get().isBlank()) {
            consumer.accept(value, tabPath);
        }
    }

    @RequiredUIAccess
    private static void forEachChild(Layout<?> layout, Consumer<Component> consumer) {
        try {
            layout.forEachChild(consumer);
        }
        catch (AbstractMethodError ignored) {
        }
    }
}
