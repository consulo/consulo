package consulo.language.duplicateAnalysis;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

import java.util.List;

public class TreeComparator {
  private TreeComparator() {
  }

  public static boolean areEqual(@Nonnull PsiElement x, @Nonnull PsiElement y, NodeSpecificHasher hasher, int discardCost) {
    int costX = hasher.getNodeCost(x);
    int costY = hasher.getNodeCost(y);
    if (costX == -1 || costY == -1) return false;
    if (costX < discardCost || costY < discardCost) {
      return true;
    }

    if (hasher.areNodesEqual(x, y)) {
      if (!hasher.checkDeep(x, y)) return true;
      List<PsiElement> xSons = hasher.getNodeChildren(x);
      List<PsiElement> ySons = hasher.getNodeChildren(y);

      if (xSons.size() == ySons.size()) {
        for (int i = 0; i < ySons.size(); i++) {
          if (!areEqual(xSons.get(i), ySons.get(i), hasher, discardCost)) {
            return false;
          }
        }

        return true;
      }
    }

    return false;
  }
}
