package consulo.language.duplicateAnalysis.treeHash;

import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.duplicateAnalysis.DuplocatorSettings;
import consulo.language.duplicateAnalysis.NodeSpecificHasher;
import consulo.language.duplicateAnalysis.iterator.FilteringNodeIterator;
import consulo.language.duplicateAnalysis.iterator.SiblingNodeIterator;
import consulo.language.duplicateAnalysis.util.DuplocatorUtil;
import consulo.language.duplicateAnalysis.util.NodeFilter;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.*;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class NodeSpecificHasherBase extends NodeSpecificHasher {
  private final TreeHasherBase myTreeHasher;
  private final DuplocatorSettings mySettings;
  private final DuplicatesProfileBase myDuplicatesProfile;

  private final NodeFilter myNodeFilter = new NodeFilter() {
    @Override
    public boolean accepts(PsiElement element) {
      return DuplocatorUtil.isIgnoredNode(element) || isToSkipAsLiteral(element);
    }
  };
  protected final boolean myForIndexing;

  private boolean isToSkipAsLiteral(PsiElement element) {
    return isLiteral(element) &&
           !myDuplicatesProfile.getDuplocatorState(myDuplicatesProfile.getLanguage(element)).distinguishLiterals();
  }

  public NodeSpecificHasherBase(@Nonnull DuplocatorSettings settings,
                                @Nonnull FragmentsCollector callback,
                                @Nonnull DuplicatesProfileBase duplicatesProfile) {
    this(settings, callback, duplicatesProfile, false);
  }

  public NodeSpecificHasherBase(@Nonnull DuplocatorSettings settings,
                                @Nonnull FragmentsCollector callback,
                                @Nonnull DuplicatesProfileBase duplicatesProfile,
                                boolean forIndexing) {
    myTreeHasher = new TreeHasherBase(callback, duplicatesProfile, forIndexing ? 0:-1, forIndexing);
    mySettings = settings;
    myDuplicatesProfile = duplicatesProfile;
    myForIndexing = forIndexing;
  }

  @Nonnull
  public NodeFilter getNodeFilter() {
    return myNodeFilter;
  }

  @Override
  public int getNodeHash(PsiElement node) {
    if (node == null) {
      return 0;
    }
    if (node instanceof PsiWhiteSpace || node instanceof PsiErrorElement) {
      return 0;
    }
    else if (node instanceof LeafPsiElement) {
      if (isToSkipAsLiteral(node)) {
        return 0;
      }
      return node.getText().hashCode();

    }
    return node.getClass().getName().hashCode();
  }

  private boolean isLiteral(PsiElement node) {
    if (node instanceof LeafPsiElement psiElement) {
      IElementType elementType = psiElement.getNode().getElementType();
      if (myDuplicatesProfile.getLiterals().contains(elementType)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int getNodeCost(PsiElement node) {
    return node != null ? myDuplicatesProfile.getNodeCost(node) : 0;
  }

  @Override
  public List<PsiElement> getNodeChildren(PsiElement node) {
    List<PsiElement> result = new ArrayList<>();

    FilteringNodeIterator it = new FilteringNodeIterator(new SiblingNodeIterator(node.getFirstChild()), myNodeFilter);
    while (it.hasNext()) {
      result.add(it.current());
      it.advance();
    }

    return result;
  }

  @Override
  public boolean areNodesEqual(@Nonnull PsiElement node1, @Nonnull PsiElement node2) {
    return false;
  }

  @Override
  public boolean areTreesEqual(@Nonnull PsiElement root1, @Nonnull PsiElement root2, int discardCost) {
    if (root1 == root2) {
      return true;
    }
    return new DuplicatesMatchingVisitor(this, myNodeFilter, discardCost).match(root1, root2);
  }

  @Nonnull
  public DuplicatesProfileBase getDuplicatesProfile() {
    return myDuplicatesProfile;
  }

  @Override
  public boolean checkDeep(PsiElement node1, PsiElement node2) {
    // todo: try to optimize this
    return true;
  }

  @Override
  public void visitNode(@Nonnull PsiElement node) {
    Language language = null;
    if (node instanceof PsiFile) {
      FileType fileType = ((PsiFile)node).getFileType();
      if (fileType instanceof LanguageFileType) {
        language = ((LanguageFileType)fileType).getLanguage();
      }
    }
    if (language == null) language = node.getLanguage();
    if ((myForIndexing || mySettings.SELECTED_PROFILES.contains(language.getID())) &&
        myDuplicatesProfile.isMyLanguage(language)) {

      myTreeHasher.hash(node, this);
    }
  }

  @Override
  public void hashingFinished() {
  }
}
