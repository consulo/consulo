package consulo.language.duplicateAnalysis;

import consulo.language.duplicateAnalysis.util.PsiFragment;
import consulo.usage.UsageInfo;
import jakarta.annotation.Nullable;

public interface DupInfo {
    int getPatterns();

    int getPatternCost(int number);

    int getPatternDensity(int number);

    PsiFragment[] getFragmentOccurences(int pattern);

    UsageInfo[] getUsageOccurences(int pattern);

    int getFileCount(int pattern);

    @Nullable
    String getTitle(int pattern);

    @Nullable
    String getComment(int pattern);

    int getHash(int i);
}
