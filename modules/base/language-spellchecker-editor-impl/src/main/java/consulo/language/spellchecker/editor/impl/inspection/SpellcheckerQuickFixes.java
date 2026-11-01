package consulo.language.spellchecker.editor.impl.inspection;

import consulo.language.spellchecker.editor.impl.quickfix.AcceptWordAsCorrect;
import consulo.language.spellchecker.editor.impl.quickfix.ChangeTo;
import consulo.language.spellchecker.editor.impl.quickfix.RenameTo;
import consulo.language.spellchecker.editor.impl.quickfix.SaveTo;
import consulo.language.spellchecker.editor.impl.quickfix.SpellCheckerQuickFix;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.psi.PsiElement;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2023-03-26
 */
public class SpellcheckerQuickFixes {
    private static final SpellCheckerQuickFix[] BATCH_FIXES = new SpellCheckerQuickFix[]{new AcceptWordAsCorrect()};

    public static LocalQuickFix[] getRegularFixes(
        PsiElement element,
        TextRange textRange,
        boolean useRename,
        String typo
    ) {
        return getDefaultRegularFixes(useRename, typo, element, textRange);
    }

    public static LocalQuickFix[] getDefaultRegularFixes(
        boolean useRename,
        String typo,
        @Nullable PsiElement element,
        TextRange range
    ) {
        List<LocalQuickFix> result = new ArrayList<>();

        if (useRename) {
            result.add(new RenameTo(typo));
        }
        else if (element != null) {
            result.add(new ChangeTo(typo, element, range));
        }

        if (element == null) {
            result.add(new SaveTo(typo));
            return result.toArray(LocalQuickFix.EMPTY_ARRAY);
        }

        result.add(new SaveTo(typo));
        return result.toArray(LocalQuickFix.EMPTY_ARRAY);
    }

    public static SpellCheckerQuickFix[] getDefaultBatchFixes() {
        return BATCH_FIXES;
    }
}
