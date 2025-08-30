/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.filter;

import com.google.common.primitives.Chars;
import consulo.codeEditor.EditorEx;
import consulo.language.editor.ui.SoftWrapsEditorCustomization;
import consulo.language.editor.ui.awt.DefaultTextCompletionValueDescriptor;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.editor.ui.awt.TextFieldWithCompletion;
import consulo.language.editor.ui.awt.ValuesCompletionProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.util.Collection;
import java.util.List;

class MultilinePopupBuilder {
    private static final char[] SEPARATORS = {'|', '\n'};

    @Nonnull
    private final EditorTextField myTextField;

    MultilinePopupBuilder(
        @Nonnull Project project,
        @Nonnull Collection<String> values,
        @Nonnull String initialValue,
        boolean supportsNegativeValues
    ) {
        myTextField = createTextField(project, values, supportsNegativeValues, initialValue);
    }

    @Nonnull
    private static EditorTextField createTextField(
        @Nonnull Project project,
        Collection<String> values,
        boolean supportsNegativeValues,
        @Nonnull String initialValue
    ) {
        TextFieldWithCompletion textField =
            new TextFieldWithCompletion(
                project,
                new MyCompletionProvider(values, supportsNegativeValues),
                initialValue,
                false,
                true,
                false
            ) {
                @Override
                protected EditorEx createEditor() {
                    EditorEx editor = super.createEditor();
                    SoftWrapsEditorCustomization.ENABLED.accept(editor);
                    return editor;
                }
            };
        textField.setBorder(new CompoundBorder(JBUI.Borders.empty(2), textField.getBorder()));
        return textField;
    }

    @Nonnull
    JBPopup createPopup() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(myTextField, BorderLayout.CENTER);
        ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, myTextField)
            .setCancelOnClickOutside(true)
            .setAdText(KeymapUtil.getShortcutsText(CommonShortcuts.CTRL_ENTER.getShortcuts()) + " to finish")
            .setRequestFocus(true)
            .setResizable(true)
            .setMayBeParent(true);

        final JBPopup popup = builder.createPopup();
        popup.setMinimumSize(new JBDimension(200, 90));
        AnAction okAction = new DumbAwareAction() {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                unregisterCustomShortcutSet(popup.getContent());
                popup.closeOk(e.getInputEvent());
            }
        };
        okAction.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, popup.getContent());
        return popup;
    }

    @Nonnull
    List<String> getSelectedValues() {
        return ContainerUtil.mapNotNull(StringUtil.tokenize(myTextField.getText(), new String(SEPARATORS)), value -> {
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        });
    }

    private static class MyCompletionProvider extends ValuesCompletionProvider.ValuesCompletionProviderDumbAware<String> {
        MyCompletionProvider(@Nonnull Collection<String> values, boolean supportsNegativeValues) {
            super(new DefaultTextCompletionValueDescriptor.StringValueDescriptor(),
                supportsNegativeValues ? Lists.append(Chars.asList(SEPARATORS), '-') : Chars.asList(SEPARATORS), values, false
            );
        }

        @Nullable
        @Override
        public String getAdvertisement() {
            return "Select one or more values separated with | or new lines";
        }
    }
}
