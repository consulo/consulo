package consulo.language.duplicateAnalysis.treeHash;

import consulo.language.duplicateAnalysis.util.PsiFragment;

/**
* @author oleg
*/
public class TreeHashResult {
  int myHash;
  int myCost;
  PsiFragment myFragment;

  public TreeHashResult(int hash, int cost, PsiFragment fragment) {
    myHash = hash;
    myCost = cost;
    myFragment = fragment;
  }

  public int getHash() {
    return myHash;
  }

  public int getCost() {
    return myCost;
  }

  public PsiFragment getFragment() {
    return myFragment;
  }
}
