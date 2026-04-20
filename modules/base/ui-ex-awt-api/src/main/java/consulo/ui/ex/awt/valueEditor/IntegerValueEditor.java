// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.valueEditor;

import consulo.ui.ex.localize.UILocalize;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

public class IntegerValueEditor extends TextFieldValueEditor<Integer> {
  private int myMinValue;
  private int myMaxValue;
  private boolean myCanBeEmpty;

  public IntegerValueEditor(JTextField field, @Nullable String valueName, Integer defaultValue) {
    super(field, valueName, defaultValue);
  }

  @Override
  public Integer parseValue(@Nullable String text) throws ValueValidationException {
    try {
      if (StringUtil.isEmpty(text)) {
        if (!myCanBeEmpty) {
          throw new ValueValidationException(UILocalize.integerFieldValueExpected().get());
        }
        return getDefaultValue();
      }
      int value = Integer.parseInt(text);
      if (value < myMinValue || value > myMaxValue) {
        throw new ValueValidationException((UILocalize.integerFieldValueOutOfRange(value, myMinValue, myMaxValue).get()));
      }
      return value;
    }
    catch (NumberFormatException nfe) {
      throw new ValueValidationException((UILocalize.integerFieldValueNotANumber(text).get()));
    }
  }

  @Override
  public String valueToString(Integer value) {
    if (myCanBeEmpty && value.equals(getDefaultValue())) {
      return "";
    }
    return String.valueOf(value);
  }

  @Override
  public boolean isValid(Integer value) {
    return value >= myMinValue && value <= myMaxValue;
  }

  public int getMinValue() {
    return myMinValue;
  }

  public int getMaxValue() {
    return myMaxValue;
  }

  public void setMinValue(int minValue) {
    myMinValue = minValue;
  }

  public void setMaxValue(int maxValue) {
    myMaxValue = maxValue;
  }

  public boolean isCanBeEmpty() {
    return myCanBeEmpty;
  }

  public void setCanBeEmpty(boolean canBeEmpty) {
    myCanBeEmpty = canBeEmpty;
  }
}
