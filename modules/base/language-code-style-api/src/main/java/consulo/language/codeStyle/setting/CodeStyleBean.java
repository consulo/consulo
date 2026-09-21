// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.setting;

import consulo.language.codeStyle.*;
import consulo.language.Language;

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
    private CodeStyleSettings myRootSettings;

    public CodeStyleBean() {
        myRootSettings = new CodeStyleSettings();
    }

    public void setRootSettings(CodeStyleSettings settings) {
        myRootSettings = settings;
    }

    protected abstract Language getLanguage();

    public int getRightMargin() {
        return myRootSettings.getRightMargin(getLanguage());
    }

    public void setRightMargin(int rightMargin) {
        myRootSettings.setRightMargin(getLanguage(), rightMargin);
    }

    public WrapOnTyping getWrapOnTyping() {
        return WrapOnTyping.fromValue(getCommonSettings().WRAP_ON_TYPING);
    }

    public void setWrapOnTyping(WrapOnTyping value) {
        getCommonSettings().WRAP_ON_TYPING = value.getValue();
    }

    protected final CommonCodeStyleSettings getCommonSettings() {
        return myRootSettings.getCommonSettings(getLanguage());
    }

    protected final CommonCodeStyleSettings.IndentOptions getIndentOptions(boolean isWritable) {
        CommonCodeStyleSettings.IndentOptions indentOptions = getCommonSettings().getIndentOptions();
        if (indentOptions == null && isWritable) {
            indentOptions = getCommonSettings().initIndentOptions();
        }
        return indentOptions != null ? indentOptions : myRootSettings.OTHER_INDENT_OPTIONS;
    }

    protected final <T extends CustomCodeStyleSettings> T getCustomSettings(Class<T> settingsClass) {
        return myRootSettings.getCustomSettings(settingsClass);
    }

    protected static WrapType intToWrapType(int wrap) {
        return WrapType.byLegacyRepresentation(wrap);
    }

    protected static int wrapTypeToInt(WrapType wrapType) {
        return wrapType.getLegacyRepresentation();
    }
}
