// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.application.options.codeStyle;

import consulo.application.ApplicationBundle;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractConfigurable;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import org.jetbrains.annotations.Nls;

public class OtherFileTypesCodeStyleConfigurable extends CodeStyleAbstractConfigurable {
  private final OtherFileTypesCodeStyleOptionsForm myOptionsForm;

  public OtherFileTypesCodeStyleConfigurable(CodeStyleSettings currSettings, CodeStyleSettings modelSettings) {
    super(currSettings, modelSettings, getDisplayNameText());
    myOptionsForm = new OtherFileTypesCodeStyleOptionsForm(modelSettings);
  }

  @Override
  protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
    return myOptionsForm;
  }

  @Nls
  public static String getDisplayNameText() {
    return ApplicationBundle.message("code.style.other.file.types");
  }
}
