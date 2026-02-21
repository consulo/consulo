package consulo.language.duplicateAnalysis;

import consulo.language.psi.PsiElement;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public interface _DupInfo {
  Set<Integer> getPatterns();

  int getHeight(Integer pattern);

  int getDensity(Integer pattern);

  Set<PsiElement> getOccurencies(Integer pattern);

  String toString(Integer pattern);
}
