/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.ui.plaf.flatLaf.component;

import com.formdev.flatlaf.FlatClientProperties;
import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.textBox.TextBoxWithTextField;
import consulo.desktop.awt.ui.impl.textBox.TextFieldPlaceholderFunction;
import consulo.desktop.awt.ui.impl.validableComponent.DocumentSwingValidator;
import consulo.desktop.awt.uiOld.components.fields.ExtendableTextComponent;
import consulo.localize.LocalizeValue;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ClickListener;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.lang.Couple;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-14
 */
public class FlatLafTextBoxWithExtensionsImpl extends DocumentSwingValidator<String, FlatLafTextBoxWithExtensionsImpl.TextField> implements TextBoxWithExtensions, TextBoxWithTextField {
    public class TextField extends JBTextField implements FromSwingComponentWrapper {
        private ColorValue myForegroundColor;

        public TextField(String text) {
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
            return FlatLafTextBoxWithExtensionsImpl.this;
        }
    }

    private static final class ExtensionButton extends JButton {
        private final Extension myExtension;

        private ExtensionButton(FlatLafTextBoxWithExtensionsImpl field, Extension extension) {
            setIcon(TargetAWT.to(extension.getIcon()));
            setSelectedIcon(TargetAWT.to(extension.getHoveredIcon()));
            ClickListener clickListener = extension.getClickListener();
            if (clickListener != null) {
                addActionListener(e -> clickListener.clicked(new ClickEvent(field)));
            }
            myExtension = extension;
        }
    }

    public FlatLafTextBoxWithExtensionsImpl(String text) {
        initialize(new TextField(text));
        TextFieldPlaceholderFunction.install(toAWTComponent());

        addDocumentListenerForValidator(toAWTComponent().getDocument());

        toAWTComponent().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            @RequiredUIAccess
            protected void textChanged(DocumentEvent e) {
                getListenerDispatcher(ValueListener.class).valueChanged(new ValueEvent(FlatLafTextBoxWithExtensionsImpl.this, getValue()));
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
            public Runnable getActionOnClick() {
                ClickListener clickListener = extension.getClickListener();
                return clickListener == null ? null : () -> clickListener.clicked(new ClickEvent(FlatLafTextBoxWithExtensionsImpl.this));
            }
        };
    }

    @Nonnull
    @Override
    public TextBoxWithExtensions setExtensions(@Nonnull Extension... extensions) {
        List<Extension> left = new ArrayList<>();
        List<Extension> right = new ArrayList<>();

        for (Extension extension : extensions) {
            if (extension.isLeft()) {
                left.add(extension);
            }
            else {
                right.add(extension);
            }
        }

        rebuildExtensions(left, right);
        return this;
    }

    @Nonnull
    @Override
    public TextBoxWithExtensions addFirstExtension(@Nonnull Extension extension) {
        Couple<List<Extension>> extensions = getAllExtensions();

        if (extension.isLeft()) {
            extensions.getFirst().addFirst(extension);
        }
        else {
            extensions.getSecond().addFirst(extension);
        }

        rebuildExtensions(extensions.getFirst(), extensions.getSecond());
        return this;
    }

    @Nonnull
    @Override
    public TextBoxWithExtensions addLastExtension(@Nonnull Extension extension) {
        Couple<List<Extension>> extensions = getAllExtensions();

        if (extension.isLeft()) {
            extensions.getFirst().addLast(extension);
        }
        else {
            extensions.getSecond().addLast(extension);
        }

        rebuildExtensions(extensions.getFirst(), extensions.getSecond());
        return this;
    }

    private Couple<List<Extension>> getAllExtensions() {
        List<Extension> left = new ArrayList<>();
        List<Extension> right = new ArrayList<>();
        fillExtensions(FlatClientProperties.TEXT_FIELD_LEADING_COMPONENT, left);
        fillExtensions(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, right);
        return Couple.of(left, right);
    }

    private void fillExtensions(String clientProperty, List<Extension> list) {
        TextField field = toAWTComponent();

        Object object = field.getClientProperty(clientProperty);
        if (object instanceof JToolBar toolBar) {
            int count = toolBar.getComponentCount();

            for (int i = 0; i < count; i++) {
                Component component = toolBar.getComponent(i);

                if (component instanceof ExtensionButton extensionButton) {
                    list.add(extensionButton.myExtension);
                }
            }
        }
    }

    private void rebuildExtensions(List<Extension> leftExtensions, List<Extension> rightExtensions) {
        setOrResetExtensions(FlatClientProperties.TEXT_FIELD_LEADING_COMPONENT, leftExtensions);
        setOrResetExtensions(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, rightExtensions);
    }

    private void setOrResetExtensions(String clientProperty, List<Extension> extensions) {
        TextField field = toAWTComponent();

        if (extensions.isEmpty()) {
            field.putClientProperty(clientProperty, null);
            return;
        }

        JToolBar toolBar = new JToolBar();
        for (Extension extension : extensions) {
            toolBar.add(new ExtensionButton(this, extension));
        }
        field.putClientProperty(clientProperty, toolBar);
    }

    @Override
    public void setPlaceholder(@Nonnull LocalizeValue text) {
        toAWTComponent().getEmptyText().setText(text.getValue());
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