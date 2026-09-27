// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.HtmlBuilder;
import consulo.application.util.HtmlChunk;
import consulo.language.codeStyle.CommonCodeStyleSettings.IndentOptions;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.ui.style.StandardColors;
import consulo.ui.util.ColorValueUtil;

public abstract class IndentStatusBarUIContributor implements CodeStyleStatusBarUIContributor {
    private final IndentOptions myIndentOptions;

    public IndentStatusBarUIContributor(IndentOptions options) {
        myIndentOptions = options;
    }

    public IndentOptions getIndentOptions() {
        return myIndentOptions;
    }

    /**
     * Returns a short, usually one-word, string to indicate the source of the given indent options.
     *
     * @return The indent options source hint or {@link LocalizeValue#empty()} if not available.
     */
    public abstract LocalizeValue getHint();

    @Override
    public LocalizeValue getTooltip() {
        return createTooltip(getIndentInfo(myIndentOptions), getHint());
    }

    public static LocalizeValue getIndentInfo(IndentOptions indentOptions) {
        return indentOptions.USE_TAB_CHARACTER
            ? CodeStyleLocalize.indentStatusBarTab()
            : CodeStyleLocalize.indentStatusBarSpaces(indentOptions.INDENT_SIZE);
    }

    /**
     * @return True if "Configure indents for [Language]" action should be available when the provider is active (returns its own indent
     * options), false otherwise.
     */
    public boolean isShowFileIndentOptionsEnabled() {
        return true;
    }

    public static LocalizeValue createTooltip(LocalizeValue indentInfo, LocalizeValue hint) {
        HtmlBuilder builder = new HtmlBuilder();
        builder.append(CodeStyleLocalize.indentStatusBarIndentTooltip()).append(HtmlChunk.nbsp()).append(indentInfo);
        if (hint.isNotEmpty()) {
            builder.nbsp(2).append(HtmlChunk.span("color:" + ColorValueUtil.toHtmlColor(StandardColors.GRAY)).addText(hint));
        }
        return LocalizeValue.of(builder.wrapWithHtmlBody());
    }

    @Override
    @RequiredReadAction
    public LocalizeValue getStatusText(PsiFile psiFile) {
        LocalizeValue widgetText = getIndentInfo(myIndentOptions);
        IndentOptions projectIndentOptions = CodeStyle.getSettings(psiFile.getProject()).getLanguageIndentOptions(psiFile.getLanguage());
        if (!projectIndentOptions.equals(myIndentOptions)) {
            widgetText = LocalizeValue.join(widgetText, LocalizeValue.of('*'));
        }
        return widgetText;
    }
}
