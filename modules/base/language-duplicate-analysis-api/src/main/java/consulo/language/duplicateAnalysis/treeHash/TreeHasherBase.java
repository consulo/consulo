package consulo.language.duplicateAnalysis.treeHash;

import consulo.language.duplicateAnalysis.DuplicatesProfile;
import consulo.language.duplicateAnalysis.NodeSpecificHasher;
import consulo.language.duplicateAnalysis.PsiElementRole;
import consulo.language.duplicateAnalysis.equivalence.EquivalenceDescriptor;
import consulo.language.duplicateAnalysis.equivalence.EquivalenceDescriptorProvider;
import consulo.language.duplicateAnalysis.equivalence.MultiChildDescriptor;
import consulo.language.duplicateAnalysis.equivalence.SingleChildDescriptor;
import consulo.language.duplicateAnalysis.util.DuplocatorUtil;
import consulo.language.duplicateAnalysis.util.PsiFragment;
import consulo.language.psi.LeafPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.Couple;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
class TreeHasherBase extends AbstractTreeHasher {
  private final FragmentsCollector myCallback;
  private final int myDiscardCost;
  private final DuplicatesProfile myProfile;

  TreeHasherBase(@Nullable FragmentsCollector callback,
                 @Nonnull DuplicatesProfile profile,
                 int discardCost, boolean forIndexing) {
    super(callback, forIndexing);
    myCallback = callback;
    myDiscardCost = discardCost;
    myProfile = profile;
  }

  @Override
  protected int getDiscardCost(PsiElement root) {
    if (myDiscardCost >= 0) {
      return myDiscardCost;
    }
    return myProfile.getDuplocatorState(myProfile.getLanguage(root)).getDiscardCost();
  }

  @Override
  protected TreeHashResult hash(@Nonnull PsiElement root, PsiFragment upper, @Nonnull NodeSpecificHasher hasher) {
    TreeHashResult result = computeHash(root, upper, hasher);

    // todo: try to optimize (ex. compute cost and hash separately)
    int discardCost = getDiscardCost(root);
    if (result.getCost() < discardCost) {
      return new TreeHashResult(0, result.getCost(), result.getFragment());
    }

    return result;
  }

  private TreeHashResult computeHash(PsiElement root, PsiFragment upper, NodeSpecificHasher hasher) {
    EquivalenceDescriptorProvider descriptorProvider = EquivalenceDescriptorProvider.getInstance(root);

    if (descriptorProvider != null) {
      EquivalenceDescriptor descriptor = descriptorProvider.buildDescriptor(root);

      if (descriptor != null) {
        return computeHash(root, upper, descriptor, hasher);
      }
    }

    if (root instanceof PsiFile) {
      List<PsiElement> children = hasher.getNodeChildren(root);
      if (children.size() <= 20) {
        return hashCodeBlock(children, upper, hasher, true);
      }
    }

    NodeSpecificHasherBase ssrNodeSpecificHasher = (NodeSpecificHasherBase)hasher;

    if (shouldBeAnonymized(root, ssrNodeSpecificHasher)) {
      return computeElementHash(root, upper, hasher);
    }

    if (myForIndexing) {
      return computeElementHash(root, upper, hasher);
    }

    PsiElement element = DuplocatorUtil.getOnlyChild(root, ssrNodeSpecificHasher.getNodeFilter());
    if (element != root) {
      TreeHashResult result = hash(element, upper, hasher);
      int cost = hasher.getNodeCost(root);
      return new TreeHashResult(result.getHash(), result.getCost() + cost, result.getFragment());
    }

    return computeElementHash(element, upper, hasher);
  }

  @Override
  public boolean shouldAnonymize(PsiElement root, NodeSpecificHasher hasher) {
    return shouldBeAnonymized(root, (NodeSpecificHasherBase)hasher);
  }

  @Override
  protected TreeHashResult computeElementHash(@Nonnull PsiElement root, PsiFragment upper, NodeSpecificHasher hasher) {
    if (myForIndexing) {
      return TreeHashingUtils.computeElementHashForIndexing(this, myCallBack, root, upper, hasher);
    }

    List<PsiElement> children = hasher.getNodeChildren(root);
    int size = children.size();
    int[] childHashes = new int[size];
    int[] childCosts = new int[size];

    PsiFragment fragment = buildFragment(hasher, root, getCost(root));

    if (upper != null) {
      fragment.setParent(upper);
    }

    if (size == 0 && !(root instanceof LeafPsiElement)) {
      // contains only whitespaces and other unmeaning children
      return new TreeHashResult(0, hasher.getNodeCost(root), fragment);
    }

    for (int i = 0; i < size; i++) {
      TreeHashResult res = this.hash(children.get(i), fragment, hasher);
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

    if (shouldBeAnonymized(root, (NodeSpecificHasherBase)hasher)) {
      h = 0;
    }

    if (myCallBack != null) {
      myCallBack.add(h, c, fragment);
    }

    return new TreeHashResult(h, c, fragment);
  }

  @Override
  protected TreeHashResult hashCodeBlock(List<? extends PsiElement> statements,
                                         PsiFragment upper,
                                         NodeSpecificHasher hasher,
                                         boolean forceHash) {
    if (!myForIndexing) return super.hashCodeBlock(statements, upper, hasher, forceHash);

    return TreeHashingUtils.hashCodeBlockForIndexing(this, myCallBack, statements, upper, hasher);
  }

  private TreeHashResult computeHash(PsiElement element,
                                     PsiFragment parent,
                                     EquivalenceDescriptor descriptor,
                                     NodeSpecificHasher hasher) {
    NodeSpecificHasherBase ssrHasher = (NodeSpecificHasherBase)hasher;
    PsiElement element2 = DuplocatorUtil.skipNodeIfNeccessary(element, descriptor, ssrHasher.getNodeFilter());
    boolean canSkip = element2 != element;

    PsiFragment fragment = buildFragment(hasher, element, 0);

    if (parent != null) {
      fragment.setParent(parent);
    }

    int hash = canSkip ? 0 : hasher.getNodeHash(element);
    int cost = hasher.getNodeCost(element);

    for (SingleChildDescriptor childDescriptor : descriptor.getSingleChildDescriptors()) {
      Couple<Integer> childHashResult = computeHash(childDescriptor, fragment, hasher);
      hash = hash * 31 + childHashResult.first;
      cost += childHashResult.second;
    }

    for (MultiChildDescriptor childDescriptor : descriptor.getMultiChildDescriptors()) {
      Couple<Integer> childHashResult = computeHash(childDescriptor, fragment, hasher);
      hash = hash * 31 + childHashResult.first;
      cost += childHashResult.second;
    }

    for (Object constant : descriptor.getConstants()) {
      int constantHash = constant != null ? constant.hashCode() : 0;
      hash = hash * 31 + constantHash;
    }

    for (PsiElement[] codeBlock : descriptor.getCodeBlocks()) {
      List<PsiElement> filteredBlock = filter(codeBlock, ssrHasher);
      TreeHashResult childHashResult = hashCodeBlock(filteredBlock, fragment, hasher);
      hash = hash * 31 + childHashResult.getHash();
      cost += childHashResult.getCost();
    }

    if (myCallback != null) {
      myCallback.add(hash, cost, fragment);
    }
    return new TreeHashResult(hash, cost, fragment);
  }

  public static List<PsiElement> filter(PsiElement[] elements, NodeSpecificHasherBase hasher) {
    List<PsiElement> filteredElements = new ArrayList<>();
    for (PsiElement element : elements) {
      if (!hasher.getNodeFilter().accepts(element)) {
        filteredElements.add(element);
      }
    }
    return filteredElements;
  }

  @Nonnull
  private Couple<Integer> computeHash(SingleChildDescriptor childDescriptor,
                                                        PsiFragment parentFragment,
                                                        NodeSpecificHasher nodeSpecificHasher) {

    PsiElement element = childDescriptor.getElement();
    if (element == null) {
      return Couple.of(0, 0);
    }
    Couple<Integer> result = doComputeHash(childDescriptor, parentFragment, nodeSpecificHasher);

    DuplicatesProfileBase duplicatesProfile = ((NodeSpecificHasherBase)nodeSpecificHasher).getDuplicatesProfile();
    PsiElementRole role = duplicatesProfile.getRole(element);
    if (role != null && !duplicatesProfile.getDuplocatorState(duplicatesProfile.getLanguage(element)).distinguishRole(role)) {
      return Couple.of(0, result.second);
    }
    return result;
  }

  private static boolean shouldBeAnonymized(PsiElement element, NodeSpecificHasherBase nodeSpecificHasher) {
    DuplicatesProfileBase duplicatesProfile = nodeSpecificHasher.getDuplicatesProfile();
    PsiElementRole role = duplicatesProfile.getRole(element);
    return role != null && !duplicatesProfile.getDuplocatorState(duplicatesProfile.getLanguage(element)).distinguishRole(role);
  }

  @Nonnull
  private Couple<Integer> doComputeHash(SingleChildDescriptor childDescriptor,
                                                          PsiFragment parentFragment,
                                                          NodeSpecificHasher nodeSpecificHasher) {
    PsiElement element = childDescriptor.getElement();

    switch (childDescriptor.getType()) {
      case OPTIONALLY_IN_PATTERN:
      case DEFAULT:
        TreeHashResult result = hash(element, parentFragment, nodeSpecificHasher);
        return Couple.of(result.getHash(), result.getCost());

      case CHILDREN_OPTIONALLY_IN_PATTERN:
      case CHILDREN:
        TreeHashResult[] childResults = computeHashesForChildren(element, parentFragment, nodeSpecificHasher);
        int[] hashes = getHashes(childResults);
        int[] costs = getCosts(childResults);

        int hash = vector(hashes, 31);
        int cost = vector(costs);

        return Couple.of(hash, cost);

      case CHILDREN_IN_ANY_ORDER:
        childResults = computeHashesForChildren(element, parentFragment, nodeSpecificHasher);
        hashes = getHashes(childResults);
        costs = getCosts(childResults);

        hash = vector(hashes);
        cost = vector(costs);

        return Couple.of(hash, cost);

      default:
        return Couple.of(0, 0);
    }
  }

  @Nonnull
  private Couple<Integer> computeHash(MultiChildDescriptor childDescriptor,
                                                        PsiFragment parentFragment,
                                                        NodeSpecificHasher nodeSpecificHasher) {
    PsiElement[] elements = childDescriptor.getElements();

    if (elements == null) {
      return Couple.of(0, 0);
    }

    switch (childDescriptor.getType()) {

      case OPTIONALLY_IN_PATTERN:
      case DEFAULT:
        TreeHashResult[] childResults = computeHashes(elements, parentFragment, nodeSpecificHasher);
        int[] hashes = getHashes(childResults);
        int[] costs = getCosts(childResults);

        int hash = vector(hashes, 31);
        int cost = vector(costs);

        return Couple.of(hash, cost);

      case IN_ANY_ORDER:
        childResults = computeHashes(elements, parentFragment, nodeSpecificHasher);
        hashes = getHashes(childResults);
        costs = getCosts(childResults);

        hash = vector(hashes);
        cost = vector(costs);

        return Couple.of(hash, cost);

      default:
        return Couple.of(0, 0);
    }
  }

  @Nonnull
  private TreeHashResult[] computeHashesForChildren(PsiElement element,
                                                    PsiFragment parentFragment,
                                                    NodeSpecificHasher nodeSpecificHasher) {
    List<TreeHashResult> result = new ArrayList<>();

    for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      TreeHashResult childResult = hash(element, parentFragment, nodeSpecificHasher);
      result.add(childResult);
    }
    return result.toArray(new TreeHashResult[0]);
  }

  @Nonnull
  private TreeHashResult[] computeHashes(PsiElement[] elements,
                                         PsiFragment parentFragment,
                                         NodeSpecificHasher nodeSpecificHasher) {
    TreeHashResult[] result = new TreeHashResult[elements.length];

    for (int i = 0; i < elements.length; i++) {
      result[i] = hash(elements[i], parentFragment, nodeSpecificHasher);
    }

    return result;
  }

  private static int[] getHashes(TreeHashResult[] results) {
    int[] hashes = new int[results.length];

    for (int i = 0; i < results.length; i++) {
      hashes[i] = results[i].getHash();
    }

    return hashes;
  }

  private static int[] getCosts(TreeHashResult[] results) {
    int[] costs = new int[results.length];

    for (int i = 0; i < results.length; i++) {
      costs[i] = results[i].getCost();
    }

    return costs;
  }
}
