package consulo.language.duplicateAnalysis.treeHash;

import consulo.language.Language;
import consulo.language.ast.TokenSet;
import consulo.language.duplicateAnalysis.*;
import consulo.language.duplicateAnalysis.util.DuplocatorUtil;
import consulo.language.duplicateAnalysis.util.PsiFragment;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class DuplicatesProfileBase extends DuplicatesProfile {
  @Nonnull
  @Override
  public DuplocateVisitor createVisitor(@Nonnull FragmentsCollector collector) {
    return new NodeSpecificHasherBase(DuplocatorSettings.getInstance(), collector, this);
  }

  public abstract int getNodeCost(@Nonnull PsiElement element);

  public TokenSet getLiterals() {
    return TokenSet.EMPTY;
  }

  @Override
  @Nonnull
  public ExternalizableDuplocatorState getDuplocatorState(@Nonnull Language language) {
    return DuplocatorUtil.registerAndGetState(language);
  }

  @Override
  public boolean isMyDuplicate(@Nonnull DupInfo info, int index) {
    PsiFragment[] fragments = info.getFragmentOccurences(index);
    if (fragments.length > 0) {
      PsiElement[] elements = fragments[0].getElements();
      if (elements.length > 0) {
        PsiElement first = elements[0];
        if (first != null) {
          Language language = first.getLanguage();
          return isMyLanguage(language);
        }
      }
    }
    return false;
  }
}
