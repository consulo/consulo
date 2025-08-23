package consulo.language.duplicateAnalysis.treeHash;

import consulo.application.progress.ProgressManager;
import consulo.language.Language;
import consulo.language.duplicateAnalysis.NodeSpecificHasher;
import consulo.language.duplicateAnalysis.TreeHasher;
import consulo.language.duplicateAnalysis.util.PsiFragment;
import consulo.language.psi.LeafPsiElement;
import consulo.language.psi.PsiAnchor;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

import java.util.List;

public abstract class AbstractTreeHasher implements TreeHasher {
  protected final boolean myForIndexing;
  protected final FragmentsCollector myCallBack;

  public AbstractTreeHasher(FragmentsCollector cb, boolean forIndexing) {
    myCallBack = cb;
    myForIndexing = forIndexing;
  }

  @Override
  public final void hash(@Nonnull PsiElement root, @Nonnull NodeSpecificHasher hasher) {
    hash(root, null, hasher);
  }

  protected abstract TreeHashResult hash(@Nonnull PsiElement root, PsiFragment upper, @Nonnull NodeSpecificHasher hasher);

  /**
   * Computes element hash using children hashes.
   * Creates only single PsiFragment.
   */
  protected TreeHashResult computeElementHash(@Nonnull PsiElement root, PsiFragment upper, NodeSpecificHasher hasher) {
    if (myForIndexing) {
      return TreeHashingUtils.computeElementHashForIndexing(this, myCallBack, root, upper, hasher);
    }
    ProgressManager.checkCanceled();
    List<PsiElement> children = hasher.getNodeChildren(root);
    int size = children.size();
    int[] childHashes = new int[size];
    int[] childCosts = new int[size];

    PsiFragment fragment = buildFragment(hasher, root, getCost(root));

    if (upper != null) {
      fragment.setParent(upper);
    }

    if (size == 0 && !(root instanceof LeafPsiElement)) {
      return new TreeHashResult(hasher.getNodeHash(root), hasher.getNodeCost(root), fragment);
    }

    for (int i = 0; i < size; i++) {
      TreeHashResult res = hash(children.get(i), fragment, hasher);
      childHashes[i] = res.getHash();
      childCosts[i] = res.getCost();
    }

    int c = hasher.getNodeCost(root) + vector(childCosts);
    int h1 = hasher.getNodeHash(root);

    int discardCost = getDiscardCost(root);

    for (int i = 0; i < size; i++) {
      if (childCosts[i] <= discardCost && ignoreChildHash(children.get(i))) {
        childHashes[i] = 0;
      }
    }
    int h = h1 + vector(childHashes);

    if (myCallBack != null) {
      myCallBack.add(h, c, fragment);
    }

    return new TreeHashResult(h, c, fragment);
  }

  protected TreePsiFragment buildFragment(NodeSpecificHasher hasher, PsiElement root,int cost) {
    if (myForIndexing) {
      return new TreePsiFragment(hasher, root, cost) {
        @Override
        protected PsiAnchor createAnchor(PsiElement element) {
          return PsiAnchor.createHard(element);
        }

        @Override
        protected Language calcLanguage(PsiElement element) {
          return null; // for performance
        }
      };
    }
    return new TreePsiFragment(hasher, root, cost);
  }

  protected TreePsiFragment buildFragment(NodeSpecificHasher hasher, List<? extends PsiElement> elements, int from, int to) {
    if (myForIndexing) {
      return new TreePsiFragment(hasher, elements, from, to) {
        @Override
        protected PsiAnchor createAnchor(PsiElement element) {
          return PsiAnchor.createHard(element);
        }

        @Override
        protected Language calcLanguage(PsiElement element) {
          return null; // for performance
        }
      };
    }
    return new TreePsiFragment(hasher, elements, from, to);
  }

  protected abstract int getDiscardCost(PsiElement root);

  protected boolean ignoreChildHash(PsiElement element) {
    return false;
  }

  protected TreeHashResult hashCodeBlock(List<? extends PsiElement> statements,
                                         PsiFragment upper,
                                         NodeSpecificHasher hasher) {
    return hashCodeBlock(statements, upper, hasher, false);
  }

  /**
   * Creates PsiFragments using given statements with their hashes
   */
  protected TreeHashResult hashCodeBlock(List<? extends PsiElement> statements,
                                         PsiFragment upper,
                                         NodeSpecificHasher hasher,
                                         boolean forceHash) {
    int statementsSize = statements.size();
    if (statementsSize == 1) {
      return hash(statements.get(0), upper, hasher);
    }
    if (statementsSize > 0) {
      // Here we compute all the possible code fragments using statements
      if (statementsSize < 20 || forceHash) {   //todo should be configurable
        PsiFragment[] frags = new PsiFragment[statementsSize];

        PsiFragment fragment = buildFragment(hasher, statements, 0, statementsSize - 1);
        fragment.setParent(upper);

        // Fill all the statements costs and hashes
        int[] hashes = new int[statementsSize];
        int[] costs = new int[statementsSize];
        for (int i = 0; i < statementsSize; i++) {
          TreeHashResult res = hash(statements.get(i), null, hasher);
          hashes[i] = res.getHash();
          costs[i] = res.getCost();
          frags[i] = res.getFragment();
        }

        if (myCallBack != null) {
          PsiFragment[] parents = new PsiFragment[statementsSize]; //parent(end) = [beg, end]
          for (int beg = 0; beg < statementsSize; beg++) {
            int hash = 0;
            int cost = 0;
            for (int end = beg; end < statementsSize && end - beg < 20; end++) {
              hash = 31 * hash + hashes[end];
              cost += costs[end];
              PsiFragment curr =
                beg == end
                ? frags[beg]
                : beg == 0 && end == statementsSize - 1 ? fragment : buildFragment(hasher, statements, beg, end);
              if (beg > 0) {
                curr.setParent(parents[end]); //[beg, end].setParent([beg - 1, end])
              }
              parents[end] = curr;
              if (end > beg) {
                parents[end - 1].setParent(curr);//[beg, end - 1].setParent([beg, end])
              }
              myCallBack.add(hash, cost, curr);
            }
          }
        }
        return new TreeHashResult(vector(hashes, 31), vector(costs), fragment);
      }
    }
    return new TreeHashResult(1, 0, buildFragment(hasher, statements, 0, statementsSize - 1));
  }

  protected int getCost(PsiElement root){
    return 0;
  }

  public static int vector(int[] args) {
    return vector(args, 1);
  }

  public static int vector(int[] args, int mult) {
    int sum = 0;
    for (int arg : args) {
      sum = mult * sum + arg;
    }
    return sum;
  }

  public boolean shouldAnonymize(PsiElement root, NodeSpecificHasher hasher) {
    return false;
  }
}
