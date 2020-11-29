/*
 * Copyright 2013-2020 consulo.io
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
import com.intellij.util.Function;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.validableComponent.DocumentSwingValidator;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-04-19
 */
public class DesktopIntBoxImpl extends DocumentSwingValidator<Integer, JBTextField> implements IntBox {
  private static class Listener extends DocumentAdapter {
    private final DesktopIntBoxImpl myTextField;
    private final Function<DocumentEvent, Integer> myPrevValueGetter;

    public Listener(DesktopIntBoxImpl textField, Function<DocumentEvent, Integer> prevValueGetter) {
      myTextField = textField;
      myPrevValueGetter = prevValueGetter;
    }

    @Override
    @RequiredUIAccess
    protected void textChanged(DocumentEvent e) {
      myTextField.valueChanged(myPrevValueGetter.apply(e));
    }
  }

  class MyJBTextField extends JBTextField implements FromSwingComponentWrapper {

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopIntBoxImpl.this;
    }

    @Override
    public void setText(String t) {
      super.setText(t);
    }
  }

  private Integer myMinValue;
  private Integer myMaxValue;

  public DesktopIntBoxImpl(int value) {
    MyJBTextField field = new MyJBTextField();
    field.setDocument(new PlainDocument() {
      @Override
      public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        char[] source = str.toCharArray();
        char[] result = new char[source.length];
        int j = 0;

        for (int i = 0; i < result.length; i++) {
          if (Character.isDigit(source[i])) {
            result[j++] = source[i];
          }
          else {
            Toolkit.getDefaultToolkit().beep();
          }
        }
        super.insertString(offs, new String(result, 0, j), a);
      }
    }); field.setText("0");

    TextFieldPlaceholderFunction.install(field);
    initialize(field);
    addDocumentListenerForValidator(field.getDocument());

    field.getDocument().addDocumentListener(new Listener(this, this::getPrevValue));
    setValue(value);
  }

  @Override
  protected Integer getPrevValue(DocumentEvent e) {
    Document document = e.getDocument();
    String text = null;
    try {
      text = document.getText(0, document.getLength());
    }
    catch (BadLocationException e1) {
      throw new IllegalArgumentException(e1);
    }
    return StringUtil.isEmpty(text) ? 0 : Integer.parseInt(text);
  }

  @Override
  public void setRange(int min, int max) {
    myMinValue = min;
    myMaxValue = max;
  }

  @Override
  public void setPlaceholder(@Nullable String text) {
    toAWTComponent().getEmptyText().setText(text);
  }

  @SuppressWarnings("unchecked")
  @RequiredUIAccess
  private void valueChanged(int value) {
    dataObject().getDispatcher(ValueListener.class).valueChanged(new ValueEvent(this, value));
  }

  @Nonnull
  @Override
  public Disposable addValueListener(@Nonnull ValueListener<Integer> valueListener) {
    return dataObject().addListener(ValueListener.class, valueListener);
  }

  @Override
  public Integer getValue() {
    String text = toAWTComponent().getText();
    return StringUtil.isEmpty(text) ? 0 : Integer.parseInt(text);
  }

  @RequiredUIAccess
  @Override
  public void setValue(Integer value, boolean fireListeners) {
    toAWTComponent().setText(String.valueOf(Objects.requireNonNull(value, "Value must be not null")));
  }
}
