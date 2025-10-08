// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.intention;

import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * Intention action that is used as a title of [IntentionActionWithChoice].
 * <p>
 * Note, that this action should be non-selectable in any UI, since it does
 * not have any implementation for invoke.
 */
@SuppressWarnings("ComparableType") // FIXME [VISTALL] need understand why comparable used here
public class ChoiceTitleIntentionAction extends AbstractEmptyIntentionAction implements CustomizableIntentionAction, SyntheticIntentionAction, LocalQuickFix, Comparable<IntentionAction> {
    private final LocalizeValue myTitle;

    public ChoiceTitleIntentionAction(@Nonnull LocalizeValue title) {
        myTitle = title;
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return myTitle;
    }

    @Override
    public boolean isShowIcon() {
        return false;
    }

    @Override
    public boolean isSelectable() {
        return false;
    }

    @Override
    public boolean isShowSubmenu() {
        return false;
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
    }

    @Nonnull
    @Override
    public LocalizeValue getText() {
        return myTitle;
    }

    @Override
    public int compareTo(@Nonnull IntentionAction other) {
        if (!getText().equals(other.getText())) {
            return getText().compareTo(other.getText());
        }

        if (other instanceof ChoiceVariantIntentionAction) {
            return -1;
        }

        return 0;
    }
}
