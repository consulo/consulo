// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle;

import consulo.application.util.HtmlBuilder;
import consulo.application.util.HtmlChunk;
import consulo.language.codeStyle.CommonCodeStyleSettings.IndentOptions;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.ui.style.StandardColors;
import consulo.ui.util.ColorValueUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
     * @return The indent options source hint or {@code null} if not available.
     */
    @Nullable
    public abstract String getHint();

    @Nullable
    @Override
    public String getTooltip() {
        return createTooltip(getIndentInfo(myIndentOptions).get(), getHint());
    }

    @Nonnull
    public static LocalizeValue getIndentInfo(@Nonnull IndentOptions indentOptions) {
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

    @Nonnull
    public static String createTooltip(String indentInfo, String hint) {
        HtmlBuilder builder = new HtmlBuilder();
        builder.append(CodeStyleLocalize.indentStatusBarIndentTooltip().get()).append(HtmlChunk.nbsp()).append(indentInfo);
        if (hint != null) {
            builder.nbsp(2).append(HtmlChunk.span("color:" + ColorValueUtil.toHtmlColor(StandardColors.GRAY)).addText(hint));
        }
        return builder.wrapWithHtmlBody().toString();
    }

    @Nonnull
    @Override
    public String getStatusText(@Nonnull PsiFile psiFile) {
        String widgetText = getIndentInfo(myIndentOptions).get();
        IndentOptions projectIndentOptions = CodeStyle.getSettings(psiFile.getProject()).getLanguageIndentOptions(psiFile.getLanguage());
        if (!projectIndentOptions.equals(myIndentOptions)) {
            widgetText += "*";
        }
        return widgetText;
    }
}
