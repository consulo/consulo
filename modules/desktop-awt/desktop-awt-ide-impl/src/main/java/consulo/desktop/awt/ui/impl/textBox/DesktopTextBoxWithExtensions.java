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
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.validableComponent.DocumentSwingValidator;
import consulo.desktop.awt.uiOld.components.fields.ExtendableTextComponent;
import consulo.desktop.awt.uiOld.components.fields.ExtendableTextField;
import consulo.localize.LocalizeValue;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-10-31
 */
public class DesktopTextBoxWithExtensions {
    public static class Supported extends DocumentSwingValidator<String, Supported.MyExtendableTextField> implements TextBoxWithExtensions, TextBoxWithTextField {
        public class MyExtendableTextField extends ExtendableTextField implements FromSwingComponentWrapper {
            private ColorValue myForegroundColor;

            public MyExtendableTextField(String text) {
                super(text);
            }

            public void setForegroundColor(ColorValue foregroundColor) {
                myForegroundColor = foregroundColor;

                updateForegroudColor();
            }

            @Override
            public void updateUI() {
                super.updateUI();

                updateForegroudColor();
            }

            private void updateForegroudColor() {
                if (myForegroundColor == null) {
                    setForeground(null);
                }
                else {
                    setForeground(TargetAWT.to(myForegroundColor));
                }
            }

            @Nonnull
            @Override
            public consulo.ui.Component toUIComponent() {
                return Supported.this;
            }
        }

        public Supported(String text) {
            initialize(new MyExtendableTextField(text));
            TextFieldPlaceholderFunction.install(toAWTComponent());

            addDocumentListenerForValidator(toAWTComponent().getDocument());

            toAWTComponent().getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                @SuppressWarnings("unchecked")
                @RequiredUIAccess
                protected void textChanged(DocumentEvent e) {
                    getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(Supported.this, getValue()));
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

        @Override
        public void setForegroundColor(@Nullable ColorValue foreground) {
            toAWTComponent().setForegroundColor(foreground);
        }

        @Override
        public void setPlaceholder(@Nonnull LocalizeValue text) {
            JTextField field = toAWTComponent();
            field.putClientProperty("JTextField.placeholderText", text == LocalizeValue.empty() ? null : text.getValue());
        }

        @Nullable
        @Override
        public ColorValue getForegroundColor() {
            return toAWTComponent().myForegroundColor;
        }

        private ExtendableTextComponent.Extension convert(Extension extension) {
            return new ExtendableTextComponent.Extension() {
                @Override
                public Image getIcon(boolean hovered) {
                    return hovered ? extension.getHoveredIcon() : extension.getIcon();
                }

                @Override
                public boolean isIconBeforeText() {
                    return extension.isLeft();
                }

                @Override
                public Consumer<AWTEvent> getActionOnClick() {
                    var clickListener = extension.getClickListener();
                    return clickListener == null ? null : (e) -> clickListener.onEvent(new ClickEvent(Supported.this, DesktopAWTInputDetails.convert(myComponent, e)));
                }
            };
        }

        @Nonnull
        @Override
        public TextBoxWithExtensions setExtensions(@Nonnull Extension... extensions) {
            List<ExtendableTextComponent.Extension> awtExtensions = new ArrayList<>(extensions.length);

            for (Extension extension : extensions) {
                awtExtensions.add(convert(extension));
            }

            toAWTComponent().setExtensions(awtExtensions);
            toAWTComponent().repaint();
            return this;
        }

        @Nonnull
        @Override
        public TextBoxWithExtensions addFirstExtension(@Nonnull Extension extension) {
            List<ExtendableTextComponent.Extension> awtExtensions = new ArrayList<>(toAWTComponent().getExtensions());
            awtExtensions.add(convert(extension));

            toAWTComponent().setExtensions(awtExtensions);
            toAWTComponent().repaint();
            return this;
        }

        @Nonnull
        @Override
        public TextBoxWithExtensions addLastExtension(@Nonnull Extension extension) {
            List<ExtendableTextComponent.Extension> awtExtensions = new ArrayList<>();
            awtExtensions.add(convert(extension));
            awtExtensions.addAll(toAWTComponent().getExtensions());

            toAWTComponent().setExtensions(awtExtensions);
            toAWTComponent().repaint();
            return this;
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
        public void setEditable(boolean editable) {
            toAWTComponent().setEditable(editable);
        }

        @Override
        public boolean isEditable() {
            return toAWTComponent().isEditable();
        }
    }
}
