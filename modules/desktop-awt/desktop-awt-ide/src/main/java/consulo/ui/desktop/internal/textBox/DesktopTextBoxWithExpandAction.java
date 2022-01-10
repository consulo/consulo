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

import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.fields.ExpandableTextField;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.validableComponent.DocumentSwingValidator;
import consulo.ui.desktop.laf.extend.LafExtendUtil;
import consulo.ui.desktop.laf.extend.textBox.SupportTextBoxWithExpandActionExtender;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.EventListener;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2019-04-26
 */
public class DesktopTextBoxWithExpandAction {
  @RequiredUIAccess
  public static TextBoxWithExpandAction create(@Nullable Image editButtonImage, String dialogTitle, Function<String, List<String>> parser, Function<List<String>, String> joiner) {
    SupportTextBoxWithExpandActionExtender extender = LafExtendUtil.getExtender(SupportTextBoxWithExpandActionExtender.class);
    if (extender != null) {
      return new SupportedTextBoxWithExpandAction(parser, joiner, extender);
    }

    return new FallbackTextBoxWithExpandAction(editButtonImage, dialogTitle, parser, joiner);
  }

  private static class SupportedTextBoxWithExpandAction extends DocumentSwingValidator<String, ExpandableTextField> implements TextBoxWithExpandAction, TextBoxWithTextField {
    private SupportedTextBoxWithExpandAction(Function<String, List<String>> parser, Function<List<String>, String> joiner, SupportTextBoxWithExpandActionExtender lookAndFeel) {
      ExpandableTextField field = new ExpandableTextField(parser::apply, joiner::apply, lookAndFeel);
      TextFieldPlaceholderFunction.install(field);
      initialize(field);
      addDocumentListenerForValidator(field.getDocument());

      field.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        @SuppressWarnings("unchecked")
        @RequiredUIAccess
        protected void textChanged(DocumentEvent e) {
          getListenerDispatcher(ValueListener.class).valueChanged(new ValueEvent(SupportedTextBoxWithExpandAction.this, getValue()));
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
    public TextBoxWithExpandAction setDialogTitle(@Nonnull String text) {
      return this;
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

  private static class FallbackTextBoxWithExpandAction extends DocumentSwingValidator<String, ComponentWithBrowseButton<JComponent>> implements TextBoxWithTextField, TextBoxWithExpandAction {
    private DesktopTextBoxImpl myTextBox;

    private String myDialogTitle;

    @RequiredUIAccess
    private FallbackTextBoxWithExpandAction(Image editButtonImage, String dialogTitle, Function<String, List<String>> parser, Function<List<String>, String> joiner) {
      myTextBox = new DesktopTextBoxImpl("");
      myDialogTitle = StringUtil.notNullize(dialogTitle);

      JTextField awtTextField = myTextBox.toAWTComponent();

      initialize(new ComponentWithBrowseButton<>(awtTextField, e -> Messages.showTextAreaDialog(awtTextField, myDialogTitle, myDialogTitle, parser::apply, joiner::apply)));

      addDocumentListenerForValidator(awtTextField.getDocument());

      if (editButtonImage != null) {
        toAWTComponent().setButtonIcon(editButtonImage);
      }
    }

    @Nonnull
    @Override
    public JTextField getTextField() {
      return myTextBox.getTextField();
    }

    @Nonnull
    @Override
    public <T extends EventListener> Disposable addListener(@Nonnull Class<T> eventClass, @Nonnull T listener) {
      return myTextBox.addListener(eventClass, listener);
    }

    @Nonnull
    @Override
    public <T extends EventListener> T getListenerDispatcher(@Nonnull Class<T> eventClass) {
      return myTextBox.getListenerDispatcher(eventClass);
    }

    @Override
    public boolean hasFocus() {
      return myTextBox.hasFocus();
    }

    @Nullable
    @Override
    public String getValue() {
      return myTextBox.getValue();
    }

    @RequiredUIAccess
    @Override
    public void setValue(String value, boolean fireListeners) {
      myTextBox.setValue(value, fireListeners);
    }

    @Nonnull
    @Override
    public TextBoxWithExpandAction setDialogTitle(@Nonnull String text) {
      myDialogTitle = text;
      return this;
    }

    @Override
    public void setPlaceholder(@Nullable LocalizeValue text) {
      myTextBox.setPlaceholder(text);
    }

    @Override
    public void setVisibleLength(int columns) {
      myTextBox.setVisibleLength(columns);
    }

    @Override
    public void selectAll() {
      myTextBox.selectAll();
    }

    @Override
    public void setEditable(boolean editable) {
      myTextBox.setEditable(editable);
    }

    @Override
    public boolean isEditable() {
      return myTextBox.isEditable();
    }
  }
}
