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
package consulo.ui.desktop.internal.textBox;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.ui.roots.ScalableIconComponent;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.disposer.Disposable;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.util.AWTFocusAdapterAsFocusListener;
import consulo.ui.desktop.internal.util.AWTKeyAdapterAsKeyListener;
import consulo.ui.desktop.internal.validableComponent.DocumentSwingValidator;
import consulo.ui.desktop.laf.extend.textBox.SupportTextBoxWithExtensionsExtender;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ClickListener;
import consulo.ui.event.FocusListener;
import consulo.ui.event.KeyListener;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-10-31
 */
public class DesktopTextBoxWithExtensions {
  private static class Supported extends DocumentSwingValidator<String, ExtendableTextField> implements TextBoxWithExtensions, TextBoxWithTextField {
    public Supported(String text) {
      initialize(new ExtendableTextField(text));
      TextFieldPlaceholderFunction.install(toAWTComponent());

      addDocumentListenerForValidator(toAWTComponent().getDocument());

      toAWTComponent().getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        @SuppressWarnings("unchecked")
        @RequiredUIAccess
        protected void textChanged(DocumentEvent e) {
          getListenerDispatcher(ValueListener.class).valueChanged(new ValueEvent(Supported.this, getValue()));
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
    public void setValue(String value, boolean fireEvents) {
      toAWTComponent().setText(value);
    }

    @Nonnull
    @Override
    public TextBoxWithExtensions setExtensions(@Nonnull Extension... extensions) {
      List<ExtendableTextComponent.Extension> awtExtensions = new ArrayList<>(extensions.length);

      for (Extension extension : extensions) {
        ExtendableTextComponent.Extension ex = new ExtendableTextComponent.Extension() {
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
            return clickListener == null ? null : () -> clickListener.clicked(new ClickEvent(Supported.this));
          }
        };

        awtExtensions.add(ex);
      }
      toAWTComponent().setExtensions(awtExtensions);
      toAWTComponent().repaint();
      return this;
    }

    @Nonnull
    @Override
    public TextBox setPlaceholder(@Nullable String text) {
      toAWTComponent().getEmptyText().setText(text);
      return this;
    }

    @Nonnull
    @Override
    public TextBox setVisibleLength(int columns) {
      toAWTComponent().setColumns(columns);
      return this;
    }

    @Override
    public void selectAll() {
      toAWTComponent().selectAll();
    }
  }

  private static class Unsupported extends DocumentSwingValidator<String, JPanel> implements TextBoxWithExtensions, TextBoxWithTextField {
    private JBTextField myTextField;

    private Unsupported(String text) {
      initialize(new JPanel(new BorderLayout()) {
        @Override
        public void requestFocus() {
          myTextField.requestFocus();
        }

        @Override
        public boolean requestFocusInWindow() {
          return myTextField.requestFocusInWindow();
        }
      });

      myTextField = new JBTextField(text);
      TextFieldPlaceholderFunction.install(myTextField);

      myTextField.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        @SuppressWarnings("unchecked")
        @RequiredUIAccess
        protected void textChanged(DocumentEvent e) {
          getListenerDispatcher(ValueListener.class).valueChanged(new ValueEvent(Unsupported.this, getValue()));
        }
      });

      addDocumentListenerForValidator(myTextField.getDocument());

      JPanel panel = toAWTComponent();

      panel.setBackground(myTextField.getBackground());
      panel.add(myTextField, BorderLayout.CENTER);

      myTextField.setBorder(JBUI.Borders.empty(0, 4));
      panel.setBorder(JBUI.Borders.customLine(UIUtil.getBorderColor(), 1));
    }

    @Nonnull
    @Override
    public JBTextField getTextField() {
      return myTextField;
    }

    @Nonnull
    @Override
    public TextBoxWithExtensions setExtensions(@Nonnull Extension... extensions) {
      JPanel panel = toAWTComponent();

      List<Component> toRemove = new ArrayList<>();
      for (int i = 0; i < panel.getComponentCount(); i++) {
        Component component = panel.getComponent(i);
        if (component != myTextField) toRemove.add(component);
      }

      for (Component component : toRemove) {
        panel.remove(component);
      }

      for (Extension extension : extensions) {
        ScalableIconComponent icon = new ScalableIconComponent(extension.getIcon());

        if (extension.isLeft()) {
          panel.add(icon, BorderLayout.WEST);
        }
        else {
          panel.add(icon, BorderLayout.EAST);
        }

        icon.revalidate();
      }

      return this;
    }

    @Override
    public Disposable addKeyListener(@Nonnull KeyListener listener) {
      AWTKeyAdapterAsKeyListener adapter = new AWTKeyAdapterAsKeyListener(this, listener);
      myTextField.addKeyListener(adapter);
      return () -> myTextField.removeKeyListener(adapter);
    }

    @Nonnull
    @Override
    public Disposable addFocusListener(@Nonnull FocusListener listener) {
      AWTFocusAdapterAsFocusListener adapter = new AWTFocusAdapterAsFocusListener(this, listener);
      myTextField.addFocusListener(adapter);
      return () -> myTextField.removeFocusListener(adapter);
    }

    @Override
    public boolean hasFocus() {
      return myTextField.hasFocus();
    }

    @Nonnull
    @Override
    public TextBox setPlaceholder(@Nullable String text) {
      myTextField.getEmptyText().setText(text);
      return this;
    }

    @Nonnull
    @Override
    public TextBox setVisibleLength(int columns) {
      myTextField.setColumns(columns);
      return this;
    }

    @Override
    public void selectAll() {
      myTextField.selectAll();
    }

    @Nullable
    @Override
    public String getValue() {
      return myTextField.getText();
    }

    @RequiredUIAccess
    @Override
    public void setValue(String value, boolean fireEvents) {
      myTextField.setText(value);
    }
  }

  public static TextBoxWithExtensions create(String text) {
    Object o = UIManager.get(SupportTextBoxWithExtensionsExtender.class);
    if (o instanceof SupportTextBoxWithExtensionsExtender) {
      return new Supported(text);
    }
    return new Unsupported(text);
  }
}
