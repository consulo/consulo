/*
 * Copyright 2013-2019 consulo.io
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
package consulo.desktop.awt.ui.impl.textBox;

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.validableComponent.DocumentSwingValidator;
import consulo.desktop.awt.ui.plaf.extend.textBox.SupportTextBoxWithExpandActionExtender;
import consulo.desktop.awt.uiOld.components.fields.ExpandableTextField;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2019-04-26
 */
public class DesktopTextBoxWithExpandAction {
    @RequiredUIAccess
    public static TextBoxWithExpandAction create(@Nullable Image editButtonImage, String dialogTitle, Function<String, List<String>> parser, Function<List<String>, String> joiner) {
        return new SupportedTextBoxWithExpandAction(parser, joiner, SupportTextBoxWithExpandActionExtender.INSTANCE);
    }

    private static class SupportedTextBoxWithExpandAction extends DocumentSwingValidator<String, ExpandableTextField> implements TextBoxWithExpandAction, TextBoxWithTextField {
        private class MyExpandableTextField extends ExpandableTextField implements FromSwingComponentWrapper {
            private MyExpandableTextField(@Nonnull Function<? super String, ? extends List<String>> parser,
                                          @Nonnull Function<? super List<String>, String> joiner,
                                          SupportTextBoxWithExpandActionExtender lookAndFeel) {
                super(parser, joiner, lookAndFeel);
            }

            @Nonnull
            @Override
            public Component toUIComponent() {
                return SupportedTextBoxWithExpandAction.this;
            }
        }

        private SupportedTextBoxWithExpandAction(Function<String, List<String>> parser, Function<List<String>, String> joiner, SupportTextBoxWithExpandActionExtender lookAndFeel) {
            ExpandableTextField field = new MyExpandableTextField(parser::apply, joiner::apply, lookAndFeel);
            TextFieldPlaceholderFunction.install(field);
            initialize(field);
            addDocumentListenerForValidator(field.getDocument());

            field.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                @SuppressWarnings("unchecked")
                @RequiredUIAccess
                protected void textChanged(DocumentEvent e) {
                    getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(SupportedTextBoxWithExpandAction.this, getValue()));
                }
            });
        }

        @Nonnull
        @Override
        public JTextField getTextField() {
            return toAWTComponent();
        }

        @Nullable
        @Override
        public String getValue() {
            return toAWTComponent().getText();
        }

        @RequiredUIAccess
        @Override
        public void setValue(String value, boolean fireListeners) {
            toAWTComponent().setText(value);
        }

        @Nonnull
        @Override
        public TextBoxWithExpandAction withDialogTitle(@Nonnull String text) {
            return this;
        }

        @Override
        public void setPlaceholder(@Nonnull LocalizeValue text) {
            JTextField field = toAWTComponent();
            field.putClientProperty("JTextField.placeholderText", text == LocalizeValue.empty() ? null : text.getValue());
        }

        @Override
        public void setVisibleLength(int columns) {
            toAWTComponent().setColumns(columns);
        }

        @Override
        public void selectAll() {
            toAWTComponent().selectAll();
        }

        @Override
        public void select(int from, int to) {
            toAWTComponent().setCaretPosition(from);
            toAWTComponent().moveCaretPosition(to);
        }

        @Override
        public void moveCaretTo(int index) {
            toAWTComponent().setCaretPosition(index);
            toAWTComponent().moveCaretPosition(index);
        }

        @Override
        public void setEditable(boolean editable) {
            toAWTComponent().setEditable(editable);
        }

        @Override
        public boolean isEditable() {
            return toAWTComponent().isEditable();
        }
    }
}
