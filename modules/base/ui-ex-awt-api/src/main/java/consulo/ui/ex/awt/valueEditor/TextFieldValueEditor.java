// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.valueEditor;

import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.InvalidDataException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;

public abstract class TextFieldValueEditor<T> extends AbstractValueEditor<T> {
  private final JTextField myField;

  public TextFieldValueEditor(@Nonnull JTextField field, @Nullable String valueName, @Nonnull T defaultValue) {
    super(valueName, defaultValue);
    myField = field;
    myField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@Nonnull DocumentEvent e) {
        String errorText = validateTextOnChange(myField.getText(), e);
        highlightState(StringUtil.isEmpty(errorText));
        if (StringUtil.isNotEmpty(errorText)) {
          setErrorText(errorText);
        }
      }
    });
  }

  @SuppressWarnings("unused")
  protected String validateTextOnChange(String text, DocumentEvent e) {
    try {
      T newValue = parseValue(text);
      fireValueChanged(newValue);
      return null;
    }
    catch (ValueValidationException ex) {
      return ex.getMessage();
    }
  }

  private void highlightState(boolean isValid) {
    myField.putClientProperty("JComponent.outline", isValid ? null : "error");
  }

  @SuppressWarnings("unused")
  protected void setErrorText(@Nonnull String errorText) {
    // TODO: to be implemented later
  }


  @Override
  public String getValueText() {
    return myField.getText();
  }

  @Override
  public void setValueText(@Nonnull String text) {
    myField.setText(text);
  }

}
