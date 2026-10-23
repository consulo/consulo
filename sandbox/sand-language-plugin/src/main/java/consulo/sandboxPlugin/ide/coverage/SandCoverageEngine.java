package consulo.sandboxPlugin.ide.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.coverage.CoverageAnnotator;
import consulo.execution.coverage.CoverageEnabledConfiguration;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageFileProvider;
import consulo.execution.coverage.CoverageRunner;
import consulo.execution.coverage.CoverageSuite;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.test.AbstractTestProxy;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.run.SandConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Coverage for Sand configurations; nothing is collected, it only enables the coverage fragment.
 *
 * @author VISTALL
 * @since 2026-10-23
 */
@ExtensionImpl
public class SandCoverageEngine extends CoverageEngine {
    @Override
    public boolean isApplicableTo(@Nullable RunConfigurationBase conf) {
        return conf instanceof SandConfiguration;
    }

    @Override
    public boolean canHavePerTestCoverage(@Nullable RunConfigurationBase conf) {
        return true;
    }

    @Override
    public CoverageEnabledConfiguration createCoverageEnabledConfiguration(@Nullable RunConfigurationBase conf) {
        return new CoverageEnabledConfiguration(conf) {
        };
    }

    @Override
    public @Nullable CoverageSuite createCoverageSuite(CoverageRunner covRunner,
                                                       String name,
                                                       CoverageFileProvider coverageDataFileProvider,
                                                       @Nullable String[] filters,
                                                       long lastCoverageTimeStamp,
                                                       @Nullable String suiteToMerge,
                                                       boolean coverageByTestEnabled,
                                                       boolean tracingEnabled,
                                                       boolean trackTestFolders,
                                                       Project project) {
        return null;
    }

    @Override
    public @Nullable CoverageSuite createCoverageSuite(CoverageRunner covRunner,
                                                       String name,
                                                       CoverageFileProvider coverageDataFileProvider,
                                                       CoverageEnabledConfiguration config) {
        return null;
    }

    @Override
    public @Nullable CoverageSuite createEmptyCoverageSuite(CoverageRunner coverageRunner) {
        return null;
    }

    @Override
    public CoverageAnnotator getCoverageAnnotator(Project project) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean coverageEditorHighlightingApplicableTo(PsiFile psiFile) {
        return false;
    }

    @Override
    public boolean acceptedByFilters(PsiFile psiFile, CoverageSuitesBundle suite) {
        return false;
    }

    @Override
    public boolean recompileProjectAndRerunAction(Module module, CoverageSuitesBundle suite, Runnable chooseSuiteAction) {
        return false;
    }

    @Override
    public Set<String> getQualifiedNames(PsiFile sourceFile) {
        return Set.of();
    }

    @Override
    public List<PsiElement> findTestsByNames(String[] testNames, Project project) {
        return List.of();
    }

    @Override
    public @Nullable String getTestMethodName(PsiElement element, AbstractTestProxy testProxy) {
        return null;
    }

    @Override
    public String getPresentableText() {
        return "Sand Coverage";
    }
}
