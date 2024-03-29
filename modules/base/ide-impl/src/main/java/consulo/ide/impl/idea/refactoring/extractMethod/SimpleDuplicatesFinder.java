package consulo.ide.impl.idea.refactoring.extractMethod;

import consulo.language.editor.PsiEquivalenceUtil;
import consulo.application.progress.ProgressManager;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * User : ktisha
 */
public class SimpleDuplicatesFinder {
  private static final Key<PsiElement> PARAMETER = Key.create("PARAMETER");

  protected PsiElement myReplacement;
  private final ArrayList<PsiElement> myPattern;
  private final Set<String> myParameters;
  private final Collection<String> myOutputVariables;

  public SimpleDuplicatesFinder(@Nonnull final PsiElement statement1,
                                @Nonnull final PsiElement statement2,
                                Collection<String> variables,
                                AbstractVariableData[] variableData) {
    myOutputVariables = variables;
    myParameters = new HashSet<>();
    for (AbstractVariableData data : variableData) {
      myParameters.add(data.getOriginalName());
    }
    myPattern = new ArrayList<>();
    PsiElement sibling = statement1;

    do {
      myPattern.add(sibling);
      if (sibling == statement2) break;
      sibling = PsiTreeUtil.skipSiblingsForward(sibling, PsiWhiteSpace.class, PsiComment.class);
    } while (sibling != null);
  }

  public List<SimpleMatch> findDuplicates(@Nullable final List<PsiElement> scope,
                                          @Nonnull final PsiElement generatedMethod) {
    final List<SimpleMatch> result = new ArrayList<>();
    annotatePattern();
    if (scope != null) {
      for (PsiElement element : scope) {
        findPatternOccurrences(result, element, generatedMethod);
      }
    }
    deannotatePattern();
    return result;
  }

  private void deannotatePattern() {
    for (final PsiElement patternComponent : myPattern) {
      patternComponent.accept(new PsiRecursiveElementWalkingVisitor() {
        @Override public void visitElement(PsiElement element) {
          if (element.getUserData(PARAMETER) != null) {
            element.putUserData(PARAMETER, null);
          }
        }
      });
    }
  }

  private void annotatePattern() {
    for (final PsiElement patternComponent : myPattern) {
      patternComponent.accept(new PsiRecursiveElementWalkingVisitor() {
        @Override
        public void visitElement(PsiElement element) {
          super.visitElement(element);
          if (myParameters.contains(element.getText())) {
            element.putUserData(PARAMETER, element);
          }

        }
      });
    }
  }

  private void findPatternOccurrences(@Nonnull final List<SimpleMatch> array, @Nonnull final PsiElement scope,
                                      @Nonnull final PsiElement generatedMethod) {
    if (scope == generatedMethod) return;
    final PsiElement[] children = scope.getChildren();
    for (PsiElement child : children) {
      final SimpleMatch match = isDuplicateFragment(child);
      if (match != null) {
        array.add(match);
        continue;
      }
      findPatternOccurrences(array, child, generatedMethod);
    }
  }

  @Nullable
  protected SimpleMatch isDuplicateFragment(@Nonnull final PsiElement candidate) {
    if (!canReplace(myReplacement, candidate)) return null;
    for (PsiElement pattern : myPattern) {
      if (PsiTreeUtil.isAncestor(pattern, candidate, false)) return null;
    }
    PsiElement sibling = candidate;
    final ArrayList<PsiElement> candidates = new ArrayList<>();
    for (int i = 0; i != myPattern.size(); ++i) {
      if (sibling == null) return null;

      candidates.add(sibling);
      sibling = PsiTreeUtil.skipSiblingsForward(sibling, PsiWhiteSpace.class, PsiComment.class);
    }
    if (myPattern.size() != candidates.size()) return null;
    if (candidates.size() <= 0) return null;
    final SimpleMatch match = new SimpleMatch(candidates.get(0), candidates.get(candidates.size() - 1));
    for (int i = 0; i < myPattern.size(); i++) {
      if (!matchPattern(myPattern.get(i), candidates.get(i), match)) return null;
    }
    return match;
  }

  private boolean matchPattern(@Nullable final PsiElement pattern,
                               @Nullable final PsiElement candidate,
                               @Nonnull final SimpleMatch match) {
    ProgressManager.checkCanceled();
    if (pattern == null || candidate == null) return pattern == candidate;
    final PsiElement[] children1 = PsiEquivalenceUtil.getFilteredChildren(pattern, null, true);
    final PsiElement[] children2 = PsiEquivalenceUtil.getFilteredChildren(candidate, null, true);
    final PsiElement patternParent = pattern.getParent();
    final PsiElement candidateParent = candidate.getParent();
    if (patternParent == null || candidateParent == null) return false;
    if (pattern.getUserData(PARAMETER) != null && patternParent.getClass() == candidateParent.getClass()) {
      match.changeParameter(pattern.getText(), candidate.getText());
      return true;
    }
    if (children1.length != children2.length) return false;

    for (int i = 0; i < children1.length; i++) {
      final PsiElement child1 = children1[i];
      final PsiElement child2 = children2[i];
      if (!matchPattern(child1, child2, match)) return false;
    }

    if (children1.length == 0) {
      if (pattern.getUserData(PARAMETER) != null && patternParent.getClass() == candidateParent.getClass()) {
        match.changeParameter(pattern.getText(), candidate.getText());
        return true;
      }
      if (myOutputVariables.contains(pattern.getText())) {
        match.changeOutput(candidate.getText());
        return true;
      }
      if (!pattern.textMatches(candidate)) {
        return false;
      }
    }

    return true;
  }

  protected boolean canReplace(PsiElement replacement, PsiElement element) {
    return !PsiTreeUtil.isAncestor(replacement, element, false);
  }

  public void setReplacement(PsiElement replacement) {
    myReplacement = replacement;
  }

}
