// Copyright 2000-2017 JetBrains s.r.o.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package consulo.language.codeStyle.ui.setting;

import consulo.language.codeStyle.CodeStyleConstraints;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.ui.ex.awt.IntegerField;
import consulo.ui.ex.awt.valueEditor.ValueEditor;
import consulo.language.codeStyle.setting.IndentOptionsEditor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ContinuationOption implements CodeStyleConstraints {
  private
  @Nullable
  IntegerField myField;
  private boolean mySupported;
  private final String myName;
  private final Function<CommonCodeStyleSettings.IndentOptions, Integer> myGetter;
  private final BiConsumer<CommonCodeStyleSettings.IndentOptions, Integer> mySetter;
  private final int myDefaultValue;
  private JLabel myLabel;

  public ContinuationOption(String name, Function<CommonCodeStyleSettings.IndentOptions, Integer> getter, BiConsumer<CommonCodeStyleSettings.IndentOptions, Integer> setter, int defaultValue) {
    myName = name;
    myGetter = getter;
    mySetter = setter;
    myDefaultValue = defaultValue;
  }

  public void addToEditor(@Nonnull IndentOptionsEditor editor) {
    if (mySupported) {
      myLabel = new JLabel(myName);
      myField = editor.createIndentTextField("Continuation indent", MIN_INDENT_SIZE, MAX_INDENT_SIZE, myDefaultValue);
      editor.add(myLabel, myField);
    }
  }


  public void setSupported(boolean supported) {
    mySupported = supported;
  }

  public void setEnabled(boolean isEnabled) {
    if (mySupported && myField != null && myLabel != null) {
      myField.setEnabled(isEnabled);
      myLabel.setEnabled(isEnabled);
    }
  }

  public boolean isModified(@Nonnull CommonCodeStyleSettings.IndentOptions options) {
    return mySupported && myField != null && !myField.getValue().equals(myGetter.apply(options));
  }

  public void reset(@Nonnull CommonCodeStyleSettings.IndentOptions options) {
    if (mySupported && myField != null) {
      myField.setValue(myGetter.apply(options));
      setDefaultValueToDisplay(options.CONTINUATION_INDENT_SIZE);
    }
  }

  public void apply(@Nonnull CommonCodeStyleSettings.IndentOptions options) {
    if (mySupported && myField != null) {
      mySetter.accept(options, myField.getValue());
    }
  }

  public void addListener(@Nonnull ValueEditor.Listener<Integer> listener) {
    assert myField != null;
    myField.getValueEditor().addListener(listener);
  }

  public void setDefaultValueToDisplay(int value) {
    if (mySupported && myField != null) {
      myField.setDefaultValueText(Integer.toString(value));
    }
  }

  public void setVisible(boolean visible) {
    if (myField != null && myLabel != null) {
      myLabel.setVisible(visible);
      myField.setVisible(visible);
    }
  }
}
