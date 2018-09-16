package com.intellij.coverage;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Roman.Chernyatchik
 */
public interface CoverageAnnotator {
  /**
   *
   * @param directory  {@link com.intellij.psi.PsiDirectory} to obtain coverage information for
   * @param manager
   * @return human-readable coverage information
   */
  @Nullable
  String getDirCoverageInformationString(@Nonnull PsiDirectory directory, @Nonnull CoverageSuitesBundle currentSuite,
                                         @Nonnull CoverageDataManager manager);

  /**
   *
   * @param file {@link com.intellij.psi.PsiFile} to obtain coverage information for
   * @param manager
   * @return human-readable coverage information
   */
  @Nullable
  String getFileCoverageInformationString(@Nonnull PsiFile file, @Nonnull CoverageSuitesBundle currentSuite,
                                          @Nonnull CoverageDataManager manager);

  void onSuiteChosen(@javax.annotation.Nullable CoverageSuitesBundle newSuite);

  void renewCoverageData(@Nonnull CoverageSuitesBundle suite, @Nonnull CoverageDataManager dataManager);
}
