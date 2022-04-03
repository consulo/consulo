package consulo.coverage;

import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Roman.Chernyatchik
 */
public interface CoverageAnnotator {
  /**
   *
   * @param directory  {@link PsiDirectory} to obtain coverage information for
   * @param manager
   * @return human-readable coverage information
   */
  @Nullable
  String getDirCoverageInformationString(@Nonnull PsiDirectory directory, @Nonnull CoverageSuitesBundle currentSuite,
                                         @Nonnull CoverageDataManager manager);

  /**
   *
   * @param file {@link PsiFile} to obtain coverage information for
   * @param manager
   * @return human-readable coverage information
   */
  @Nullable
  String getFileCoverageInformationString(@Nonnull PsiFile file, @Nonnull CoverageSuitesBundle currentSuite,
                                          @Nonnull CoverageDataManager manager);

  void onSuiteChosen(@Nullable CoverageSuitesBundle newSuite);

  void renewCoverageData(@Nonnull CoverageSuitesBundle suite, @Nonnull CoverageDataManager dataManager);
}
