package consulo.ide.impl.idea.dupLocator;

import consulo.ide.impl.idea.dupLocator.util.PsiFragment;
import consulo.usage.UsageInfo;

import jakarta.annotation.Nullable;

public interface DupInfo {
  int getPatterns();
  int getPatternCost(int number);
  int getPatternDensity(int number);
  PsiFragment[] getFragmentOccurences(int pattern);
  UsageInfo[] getUsageOccurences(int pattern);
  int getFileCount(final int pattern);
  @Nullable
  String getTitle(int pattern);
  @Nullable
  String getComment(int pattern);

  int getHash(final int i);
}
