package consulo.language.duplicateAnalysis;

import consulo.language.psi.PsiElement;

import java.util.HashSet;
import java.util.TreeSet;

public interface _DupInfo {
  TreeSet<Integer> getPatterns();

  int getHeight(Integer pattern);

  int getDensity(Integer pattern);

  HashSet<PsiElement> getOccurencies(Integer pattern);

  String toString(Integer pattern);
}
