// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.setting;

import consulo.language.codeStyle.WrapType;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.CustomCodeStyleSettings;
import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * Wraps language code style options defined in {@code CommonCodeStyleSettings} and {@code CustomCodeStyleSettings} with getter/setter
 * methods for external serialization.
 *
 * @see CommonCodeStyleSettings
 * @see CustomCodeStyleSettings
 */
@SuppressWarnings("unused")
public abstract class CodeStyleBean implements Serializable {

  @Nonnull
  private CodeStyleSettings myRootSettings;

  public CodeStyleBean() {
    myRootSettings = new CodeStyleSettings();
  }

  public void setRootSettings(@Nonnull CodeStyleSettings settings) {
    myRootSettings = settings;
  }

  @Nonnull
  protected abstract Language getLanguage();

  public int getRightMargin() {
    return myRootSettings.getRightMargin(getLanguage());
  }

  public void setRightMargin(int rightMargin) {
    myRootSettings.setRightMargin(getLanguage(), rightMargin);
  }

  @Nonnull
  public CommonCodeStyleSettings.WrapOnTyping getWrapOnTyping() {
    for (CommonCodeStyleSettings.WrapOnTyping c : CommonCodeStyleSettings.WrapOnTyping.values()) {
      if (c.intValue == getCommonSettings().WRAP_ON_TYPING) return c;
    }
    return CommonCodeStyleSettings.WrapOnTyping.NO_WRAP;
  }

  public void setWrapOnTyping(@Nonnull CommonCodeStyleSettings.WrapOnTyping value) {
    getCommonSettings().WRAP_ON_TYPING = value.intValue;
  }

  @Nonnull
  protected final CommonCodeStyleSettings getCommonSettings() {
    return myRootSettings.getCommonSettings(getLanguage());
  }

  @Nonnull
  protected final CommonCodeStyleSettings.IndentOptions getIndentOptions(boolean isWritable) {
    CommonCodeStyleSettings.IndentOptions indentOptions = getCommonSettings().getIndentOptions();
    if (indentOptions == null && isWritable) {
      indentOptions = getCommonSettings().initIndentOptions();
    }
    return indentOptions != null ? indentOptions : myRootSettings.OTHER_INDENT_OPTIONS;
  }

  @Nonnull
  protected final <T extends CustomCodeStyleSettings> T getCustomSettings(@Nonnull Class<T> settingsClass) {
    return myRootSettings.getCustomSettings(settingsClass);
  }

  protected static WrapType intToWrapType(int wrap) {
    return WrapType.byLegacyRepresentation(wrap);
  }

  protected static int wrapTypeToInt(@Nonnull WrapType wrapType) {
    return wrapType.getLegacyRepresentation();
  }
}
