package consulo.language.duplicateAnalysis.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.language.Language;
import consulo.language.duplicateAnalysis.DuplicatesProfile;
import consulo.language.psi.PsiAnchor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.usage.UsageInfo;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public abstract class PsiFragment {
  private static final Logger LOG = Logger.getInstance(PsiFragment.class);

  protected final PsiAnchor[] myElementAnchors;
  private final Language myLanguage;
  private PsiFragment[] myParents;
  private boolean myDuplicate;
  private boolean myChecked;
  private boolean myNested;
  private int myCost;

  public PsiFragment(PsiElement element) {
    this(element, 0);
  }

  public PsiFragment(PsiElement element, int cost) {
    myElementAnchors = new PsiAnchor[]{createAnchor(element)};
    myDuplicate = false;
    myChecked = false;
    myNested = false;
    myParents = null;
    myCost = cost;
    myLanguage = calcLanguage(element);
  }

  protected Language calcLanguage(PsiElement element) {
    return doGetLanguageForElement(element);
  }

  protected PsiAnchor createAnchor(PsiElement element) {
    return ReadAction.compute(() -> PsiAnchor.create(element));
  }

  public PsiFragment(List<? extends PsiElement> elements) {
    this(elements, 0, elements.size() - 1);
  }

  public PsiFragment(List<? extends PsiElement> elements, int from, int to) {
    myElementAnchors = new PsiAnchor[to - from + 1];

    for (int i = from; i <= to; i++) {
      myElementAnchors[i - from] = createAnchor(elements.get(i));
    }

    myDuplicate = false;
    myChecked = false;
    myNested = false;
    myParents = null;
    myLanguage = to >= from && from < elements.size()
                 ? calcLanguage(elements.get(from))
                 : null;
  }

  @Nonnull
  @RequiredReadAction
  private static Language doGetLanguageForElement(@Nonnull PsiElement element) {
    DuplicatesProfile profile = DuplicatesProfile.findProfileForLanguage(element.getLanguage());
    if (profile == null) {
      return element.getLanguage();
    }
    return profile.getLanguage(element);
  }

  public void setCost(int c) {
    if (myCost != -1) {
      myCost = c;
    }
  }

  public void markDuplicate() {
    myDuplicate = true;
  }

  public boolean isNested() {
    if (myChecked) {
      return myNested;
    }

    myChecked = true;

    if (myParents != null) {
      PsiFragment parent1 = myParents[0];
      PsiFragment parent2 = myParents[1];

      if (parent1 != null) {
        myNested |= parent1.myDuplicate || parent1.isNested();
        if (parent2 != null) {
          myNested |= parent2.myDuplicate || parent2.isNested();
        }
      }
    }

    return myNested;
  }

  public void setParent(PsiFragment f) {
    if (f == null) return;
    if (myParents == null) {
      myParents = new PsiFragment[]{f, null};
    }
    else {
      if (myParents[0] == f || myParents[1] == f) return;
      if (myParents[1] != null) {
        LOG.error("Third parent set.");
      }

      myParents[1] = f;
    }
  }

  public PsiElement[] getElements() {
    PsiElement[] elements = new PsiElement[myElementAnchors.length];

    for (int i = 0; i < elements.length; i++) {
      elements[i] = myElementAnchors[i].retrieve();
    }

    return elements;
  }

  @Nullable
  public PsiFile getFile() {
    return myElementAnchors.length > 0 ? myElementAnchors[0].getFile() : null;
  }

  public int getStartOffset() {
    return myElementAnchors.length > 0 ? myElementAnchors[0].getStartOffset() : -1;
  }

  public int getEndOffset() {
    return myElementAnchors.length > 0 ? myElementAnchors[myElementAnchors.length - 1].getEndOffset() : -1;
  }

  public boolean intersectsWith(PsiFragment f) {
    int start = getStartOffset();
    int end = getEndOffset();
    int fStart = f.getStartOffset();
    int fEnd = f.getEndOffset();

    return
      Comparing.equal(f.getFile(), getFile()) && ((start <= fStart && fStart <= end) || (start <= fEnd && fEnd <= end));
  }

  public boolean contains(PsiFragment f) {
    int start = getStartOffset();
    int end = getEndOffset();
    int fStart = f.getStartOffset();
    int fEnd = f.getEndOffset();

    return
      Comparing.equal(f.getFile(), getFile()) && (start <= fStart && end >= fEnd);
  }

  public abstract boolean isEqual(PsiElement[] elements, int discardCost);

  @Nullable
  public UsageInfo getUsageInfo() {
    if (myElementAnchors.length == 1) {
      PsiElement element = myElementAnchors[0].retrieve();
      if (element == null || !element.isValid()) return null;
      return new UsageInfo(element);
    }

    PsiElement parent = PsiTreeUtil.findCommonParent(getElements());
    if (parent == null) return null;
    int offs = parent.getTextRange().getStartOffset();

    int startOffsetInParent = getStartOffset() - offs;
    int endOffsetInParent = getEndOffset() - offs;
    if (startOffsetInParent < 0) return null;
    if (endOffsetInParent < startOffsetInParent) return null;
    return new UsageInfo(parent, startOffsetInParent, endOffsetInParent);
  }

  //debug only
  public String toString() {
    StringBuilder buffer = new StringBuilder();

    for (PsiAnchor psiAnchor : myElementAnchors) {
      PsiElement element = psiAnchor.retrieve();
      if (element != null) {
        buffer.append(element.getText());
        buffer.append("\n");
      }
    }

    return buffer.toString();
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof PsiFragment)) return false;

    PsiFragment other = ((PsiFragment)o);

    return other.getStartOffset() == getStartOffset() &&
           other.getEndOffset() == getEndOffset() &&
           Comparing.equal(other.getFile(), getFile());
  }

  public int hashCode() {
    int result = getStartOffset();
    result += 31 * result + getEndOffset();
    PsiFile file = getFile();
    if (file != null) {
      result += 31 * result + file.getName().hashCode();
    }
    return result;
  }

  public int getCost() {
    return myCost;
  }

  public int[][] getOffsets() {
    int[][] result = new int[myElementAnchors.length][2];
    int idx = 0;
    for (PsiAnchor anchor : myElementAnchors) {
      result[idx][0] = anchor.getStartOffset();
      result[idx++][1] = anchor.getEndOffset();
    }
    return result;
  }

  public boolean containsMultipleFragments() {
    return myElementAnchors.length > 1;
  }

  @Nullable
  public Language getLanguage() {
    return myLanguage;
  }
}


