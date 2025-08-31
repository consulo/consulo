package consulo.language.duplicateAnalysis.treeHash;

import consulo.language.duplicateAnalysis.NodeSpecificHasher;
import consulo.language.duplicateAnalysis.util.PsiFragment;
import consulo.language.psi.PsiElement;

import java.util.List;

/**
* @author oleg
*/
public class TreePsiFragment extends PsiFragment {
  private final NodeSpecificHasher myHasher;

  public TreePsiFragment(NodeSpecificHasher hasher, PsiElement root, int cost) {
    super(root, cost);
    myHasher = hasher;
  }

  public TreePsiFragment(NodeSpecificHasher hasher, List<? extends PsiElement> element, int from, int to) {
    super(element, from, to);
    myHasher = hasher;
  }

  @Override
  public boolean isEqual(PsiElement[] elements, int discardCost) {
    if (elements.length != myElementAnchors.length) {
      return false;
    }

    for (int i = 0; i < myElementAnchors.length; i++) {
      PsiElement one = myElementAnchors[i].retrieve();
      PsiElement two = elements[i];

      if (one == null || two == null || !myHasher.areTreesEqual(one, two, discardCost)) {
        return false;
      }
    }

    return true;
  }
}
