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
package consulo.desktop.awt.settings;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.configurable.internal.ConfigurableForeignComponentSearch;
import consulo.configurable.internal.ConfigurableSearchHits;
import consulo.desktop.awt.ui.impl.DesktopSpotlightImpl;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ide.impl.idea.application.options.SkipSelfSearchComponent;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class DesktopConfigurableForeignComponentSearch implements ConfigurableForeignComponentSearch {
    private static final Pattern HTML_TAG = Pattern.compile("<[^<>]*>");

    @Override
    @RequiredUIAccess
    public boolean findHits(Component component, Predicate<LocalizeValue> matcher, ConfigurableSearchHits hits) {
        JComponent swingComponent = toSwing(component);
        if (swingComponent == null) {
            return false;
        }

        visit(swingComponent, matcher, hits);
        return true;
    }

    @Override
    @RequiredUIAccess
    public boolean collectTexts(Component component, List<LocalizeValue> tabPath, BiConsumer<LocalizeValue, List<LocalizeValue>> consumer) {
        JComponent swingComponent = toSwing(component);
        if (swingComponent == null) {
            return false;
        }

        collect(swingComponent, tabPath, consumer);
        return true;
    }

    private static @Nullable JComponent toSwing(Component component) {
        java.awt.Component awtComponent;
        try {
            awtComponent = TargetAWT.to(component);
        }
        catch (IllegalArgumentException e) {
            return null;
        }

        if (awtComponent instanceof FromSwingComponentWrapper || !(awtComponent instanceof JComponent jComponent)) {
            return null;
        }
        return jComponent;
    }

    private static boolean visit(JComponent component, Predicate<LocalizeValue> matcher, ConfigurableSearchHits hits) {
        component.putClientProperty(DesktopSpotlightImpl.SECTION_TARGET, null);

        if (!component.isVisible()) {
            return false;
        }

        boolean highlight = false;

        String text = getText(component);
        if (matches(text, matcher)) {
            highlight = true;
            hits.addTarget(TargetAWT.wrap(component));

            if (component instanceof JLabel label && label.getLabelFor() instanceof JComponent labelFor) {
                hits.addTarget(TargetAWT.wrap(labelFor));
            }
        }

        if (component instanceof JTabbedPane tabbedPane) {
            return visitTabs(tabbedPane, matcher, hits) || highlight;
        }

        boolean innerHighlight = false;
        for (java.awt.Component child : component.getComponents()) {
            if (child instanceof JComponent childComponent && visit(childComponent, matcher, hits)) {
                innerHighlight = true;
            }
        }

        if (!highlight && !innerHighlight
            && component.getBorder() instanceof TitledBorder titledBorder
            && matches(titledBorder.getTitle(), matcher)) {
            highlight = true;
            component.putClientProperty(DesktopSpotlightImpl.SECTION_TARGET, Boolean.TRUE);
            hits.addTarget(TargetAWT.wrap(component));
        }

        return highlight || innerHighlight;
    }

    private static boolean visitTabs(JTabbedPane tabbedPane, Predicate<LocalizeValue> matcher, ConfigurableSearchHits hits) {
        boolean highlight = false;

        int selectedIndex = tabbedPane.getSelectedIndex();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            boolean titleHit = matches(tabbedPane.getTitleAt(i), matcher);

            ConfigurableSearchHits inner = i == selectedIndex ? hits : new ConfigurableSearchHits();
            boolean innerHit = tabbedPane.getComponentAt(i) instanceof JComponent content && visit(content, matcher, inner);

            if (titleHit || i != selectedIndex && innerHit) {
                if (tabbedPane.getTabComponentAt(i) instanceof JComponent tabComponent) {
                    hits.addTarget(TargetAWT.wrap(tabComponent));
                }
                highlight = true;
            }
            else if (innerHit) {
                highlight = true;
            }
        }
        return highlight;
    }

    private static void collect(JComponent component, List<LocalizeValue> tabPath, BiConsumer<LocalizeValue, List<LocalizeValue>> consumer) {
        if (component instanceof SkipSelfSearchComponent || Boolean.TRUE.equals(UIUtil.getClientProperty(component, SkipSelfSearchComponent.KEY))) {
            return;
        }

        if (component.getBorder() instanceof TitledBorder titledBorder) {
            accept(titledBorder.getTitle(), tabPath, consumer);
        }

        accept(getText(component), tabPath, consumer);

        if (component instanceof JTabbedPane tabbedPane) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                String title = tabbedPane.getTitleAt(i);
                accept(title, tabPath, consumer);

                List<LocalizeValue> innerPath = new ArrayList<>(tabPath);
                if (title != null) {
                    innerPath.add(LocalizeValue.of(title));
                }

                if (tabbedPane.getComponentAt(i) instanceof JComponent content) {
                    collect(content, List.copyOf(innerPath), consumer);
                }
            }
            return;
        }

        for (java.awt.Component child : component.getComponents()) {
            if (child instanceof JComponent childComponent) {
                collect(childComponent, tabPath, consumer);
            }
        }
    }

    private static @Nullable String getText(JComponent component) {
        if (component instanceof JLabel label) {
            return label.getText();
        }
        if (component instanceof JCheckBox checkBox) {
            return checkBox.getText();
        }
        if (component instanceof JRadioButton radioButton) {
            return radioButton.getText();
        }
        if (component instanceof JButton button) {
            return button.getText();
        }
        return null;
    }

    private static boolean matches(@Nullable String text, Predicate<LocalizeValue> matcher) {
        return text != null && !text.isBlank() && matcher.test(LocalizeValue.of(text));
    }

    private static void accept(@Nullable String text, List<LocalizeValue> tabPath, BiConsumer<LocalizeValue, List<LocalizeValue>> consumer) {
        if (text == null) {
            return;
        }

        String plain = HTML_TAG.matcher(text).replaceAll(" ").trim();
        if (!plain.isEmpty()) {
            consumer.accept(LocalizeValue.of(plain), tabPath);
        }
    }
}
