// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.codeVision;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import org.jspecify.annotations.Nullable;

import java.awt.event.MouseEvent;

/**
 * Adds support for author code lenses to editor.
 * <p>
 * Language-specific implementations tell the VCS code vision provider which PSI elements
 * should receive a code-author lens and what happens when the user clicks on them.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface VcsCodeVisionLanguageContext extends LanguageExtension {
    ExtensionPointCacheKey<VcsCodeVisionLanguageContext, ByLanguageValue<VcsCodeVisionLanguageContext>> KEY =
        ExtensionPointCacheKey.create("VcsCodeVisionLanguageContext", LanguageOneToOne.build());

    static @Nullable VcsCodeVisionLanguageContext forLanguage(Language language) {
        return Application.get().getExtensionPoint(VcsCodeVisionLanguageContext.class).getOrBuildCache(KEY).get(language);
    }

    /**
     * @return true iff for this particular element a lens should be displayed
     */
    boolean isAccepted(PsiElement element);

    /**
     * Called when the user clicks on the code-author lens for the given element.
     */
    default void handleClick(MouseEvent mouseEvent, Editor editor, PsiElement element) {
    }

    /**
     * When the file's language differs from the language of this extension, this method tells in which
     * files elements still need to be searched (for multi-language files).
     */
    default boolean isCustomFileAccepted(PsiFile file) {
        return false;
    }

    /**
     * Returns the text range that will be used when computing the code author for the given element.
     * Defaults to the range from the name identifier (if any) to the end of the element.
     */
    @RequiredReadAction
    default TextRange computeEffectiveRange(PsiElement element) {
        PsiElement start = element instanceof PsiNameIdentifierOwner owner ? owner.getNameIdentifier() : null;
        if (start == null) start = element;
        return TextRange.create(start.getTextRange().getStartOffset(), element.getTextRange().getEndOffset());
    }
}
