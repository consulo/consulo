package consulo.execution.coverage;

import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;

import org.jspecify.annotations.Nullable;

/**
 * @author Roman.Chernyatchik
 */
public interface CoverageAnnotator {
    /**
     * @param directory {@link PsiDirectory} to obtain coverage information for
     * @param manager
     * @return human-readable coverage information
     */
    @Nullable String getDirCoverageInformationString(
        PsiDirectory directory, CoverageSuitesBundle currentSuite,
        CoverageDataManager manager
    );

    /**
     * @param file    {@link PsiFile} to obtain coverage information for
     * @param manager
     * @return human-readable coverage information
     */
    @Nullable String getFileCoverageInformationString(
        PsiFile file, CoverageSuitesBundle currentSuite,
        CoverageDataManager manager
    );

    void onSuiteChosen(@Nullable CoverageSuitesBundle newSuite);

    void renewCoverageData(CoverageSuitesBundle suite, CoverageDataManager dataManager);
}
