// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SyntaxTraverser;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Provides simple text inlays (info elements rendered inside source code) for a given language.
 * The order of hints which share the same offset is not guaranteed.
 *
 * @see InlayHintsProvider for more interactive inlays.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface InlayParameterHintsProvider extends LanguageExtension {
    ExtensionPointCacheKey<InlayParameterHintsProvider, ByLanguageValue<InlayParameterHintsProvider>> KEY =
        ExtensionPointCacheKey.create("InlayParameterHintsProvider", LanguageOneToOne.build());

    @Nullable
    static InlayParameterHintsProvider forLanguage(@Nonnull Language language) {
        return Application.get().getExtensionPoint(InlayParameterHintsProvider.class).getOrBuildCache(KEY).get(language);
    }


    /**
     * @param element element for which hints should be shown.
     * @return list of hints to be shown, hints offsets should be located within element's text range.
     */
    default @Nonnull List<InlayInfo> getParameterHints(@Nonnull PsiElement element) {
        return Collections.emptyList();
    }

    /**
     * @param file    file which holds element.
     * @param element element for which hints should be shown.
     * @return list of hints to be shown, hints offsets should be located within element's text range.
     */
    @Nonnull
    default List<InlayInfo> getParameterHints(@Nonnull PsiElement element, @Nonnull PsiFile file) {
        return getParameterHints(element);
    }

    /**
     * Provides information about hint for intention actions (can be {@link HintInfo.MethodInfo} or {@link HintInfo.OptionInfo})
     * which allow enabling/disabling hints at a given position.
     * <p>
     * Make sure that this method executed fast enough to run on EDT.
     *
     * @param element the element under the caret
     */
    default @Nullable HintInfo getHintInfo(@Nonnull PsiElement element) {
        return null;
    }

    /**
     * Provides information about hint for intention actions (can be {@link HintInfo.MethodInfo} or {@link HintInfo.OptionInfo})
     * which allow enabling/disabling hints at a given position.
     * <p>
     * Make sure that this method executed fast enough to run on EDT.
     *
     * @param element the element under the caret
     */
    default @Nullable HintInfo getHintInfo(@Nonnull PsiElement element, @Nonnull PsiFile file) {
        return getHintInfo(element);
    }

    /**
     * Exclude list - default list of patterns for which hints should not be shown.
     */
    @Nonnull
    Set<String> getDefaultBlackList();

    /**
     * Returns language which exclude list will be appended to the resulting one.
     * E.g. to prevent possible Groovy and Kotlin extensions from showing hints for excluded Java methods.
     */
    @Nullable
    default Language getBlackListDependencyLanguage() {
        return null;
    }

    /**
     * List of supported options, shown in settings dialog.
     */
    @Nonnull
    default List<Option> getSupportedOptions() {
        return List.of();
    }

    /**
     * If {@code false} no exclude list panel will be shown in "File | Settings | Editor | Inlay Hints | Language | Parameter Hints".
     */
    default boolean isBlackListSupported() {
        return true;
    }

    /**
     * Text explaining exclude list patterns.
     */
    @Nonnull
    default LocalizeValue getBlacklistExplanationHTML() {
        return LocalizeValue.of();
    }

    /**
     * Customise hints presentation.
     */
    default @Nonnull String getInlayPresentation(@Nonnull String inlayText) {
        return inlayText + ":";
    }

    /**
     * Whether provider should be queried for hints ({@link #getParameterHints(PsiElement)}) even if showing hints is disabled globally.
     */
    default boolean canShowHintsWhenDisabled() {
        return false;
    }

    /**
     * @return {@code true} if set of options is exhaustive and if all options are disabled, provider will collect no hints.
     */
    default boolean isExhaustive() {
        return false;
    }

    /**
     * @return Traverser for `root` element subtree.
     */
    @Nonnull
    default SyntaxTraverser<PsiElement> createTraversal(@Nonnull PsiElement root) {
        return SyntaxTraverser.psiTraverser(root);
    }

    @Nonnull
    LocalizeValue getPreviewFileText();

    @Nonnull
    default LocalizeValue getDescription() {
        return LocalizeValue.of();
    }

    /**
     * @param key bundle key of the option.
     * @return description of the given option or null (in this case it won't be shown).
     */
    @Nullable
    default String getProperty(String key) {
        return null;
    }
}
