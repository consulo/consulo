package consulo.execution.coverage;

import com.intellij.rt.coverage.data.LineData;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointName;
import consulo.dataContext.DataContext;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.coverage.view.CoverageViewExtension;
import consulo.execution.test.AbstractTestProxy;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Roman.Chernyatchik
 * <p/>
 * Coverage engine provide coverage support for different languages or coverage runner classes.
 * E.g. engine for JVM languages, Ruby, Python
 * <p/>
 * Each coverage engine may work with several coverage runner. E.g. Java coverage engine supports IDEA/EMMA/Cobertura,
 * Ruby engine works with RCov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class CoverageEngine {
    public static final ExtensionPointName<CoverageEngine> EP_NAME = ExtensionPointName.create(CoverageEngine.class);

    /**
     * Checks whether coverage feature is supported by this engine for given configuration or not.
     *
     * @param conf Run Configuration
     * @return True if coverage for given run configuration is supported by this engine
     */
    public abstract boolean isApplicableTo(@Nullable final RunConfigurationBase conf);

    public abstract boolean canHavePerTestCoverage(@Nullable final RunConfigurationBase conf);

    /**
     * Creates coverage enabled configuration for given RunConfiguration. It is supposed that one run configuration may be associated
     * not more than one coverage engine.
     *
     * @param conf Run Configuration
     * @return Coverage enabled configuration with engine specific settings
     */
    @Nonnull
    public abstract CoverageEnabledConfiguration createCoverageEnabledConfiguration(@Nullable final RunConfigurationBase conf);

    /**
     * Coverage suite is coverage settings & coverage data gather by coverage runner (for suites provided by TeamCity server)
     *
     * @param covRunner                Coverage Runner
     * @param name                     Suite name
     * @param coverageDataFileProvider Coverage raw data file provider
     * @param filters                  Coverage data filters
     * @param lastCoverageTimeStamp    timestamp
     * @param suiteToMerge             Suite to merge this coverage data with
     * @param coverageByTestEnabled    Collect coverage for test option
     * @param tracingEnabled           Tracing option
     * @param trackTestFolders         Track test folders option
     * @return Suite
     */
    @Nullable
    public CoverageSuite createCoverageSuite(
        @Nonnull final CoverageRunner covRunner,
        @Nonnull final String name,
        @Nonnull final CoverageFileProvider coverageDataFileProvider,
        @Nullable final String[] filters,
        final long lastCoverageTimeStamp,
        @Nullable final String suiteToMerge,
        final boolean coverageByTestEnabled,
        final boolean tracingEnabled,
        final boolean trackTestFolders
    ) {
        return createCoverageSuite(
            covRunner,
            name,
            coverageDataFileProvider,
            filters,
            lastCoverageTimeStamp,
            suiteToMerge,
            coverageByTestEnabled,
            tracingEnabled,
            trackTestFolders,
            null
        );
    }

    /**
     * Coverage suite is coverage settings & coverage data gather by coverage runner (for suites provided by TeamCity server)
     *
     * @param covRunner                Coverage Runner
     * @param name                     Suite name
     * @param coverageDataFileProvider Coverage raw data file provider
     * @param filters                  Coverage data filters
     * @param lastCoverageTimeStamp    timestamp
     * @param suiteToMerge             Suite to merge this coverage data with
     * @param coverageByTestEnabled    Collect coverage for test option
     * @param tracingEnabled           Tracing option
     * @param trackTestFolders         Track test folders option
     * @param project
     * @return Suite
     */
    @Nullable
    public abstract CoverageSuite createCoverageSuite(
        @Nonnull final CoverageRunner covRunner,
        @Nonnull final String name,
        @Nonnull final CoverageFileProvider coverageDataFileProvider,
        @Nullable final String[] filters,
        final long lastCoverageTimeStamp,
        @Nullable final String suiteToMerge,
        final boolean coverageByTestEnabled,
        final boolean tracingEnabled,
        final boolean trackTestFolders,
        Project project
    );

    /**
     * Coverage suite is coverage settings & coverage data gather by coverage runner
     *
     * @param covRunner                Coverage Runner
     * @param name                     Suite name
     * @param coverageDataFileProvider
     * @param config                   Coverage engine configuration
     * @return Suite
     */
    @Nullable
    public abstract CoverageSuite createCoverageSuite(
        @Nonnull final CoverageRunner covRunner,
        @Nonnull final String name,
        @Nonnull final CoverageFileProvider coverageDataFileProvider,
        @Nonnull final CoverageEnabledConfiguration config
    );

    @Nullable
    public abstract CoverageSuite createEmptyCoverageSuite(@Nonnull final CoverageRunner coverageRunner);

    /**
     * Coverage annotator which annotates smth(e.g. Project view nodes / editor) with coverage information
     *
     * @param project Project
     * @return Annotator
     */
    @Nonnull
    public abstract CoverageAnnotator getCoverageAnnotator(Project project);

    /**
     * Determines if coverage information should be displayed for given file. E.g. coverage may be applicable
     * only to user source files or only for files of specific types
     *
     * @param psiFile file
     * @return false if coverage N/A for given file
     */
    public abstract boolean coverageEditorHighlightingApplicableTo(@Nonnull final PsiFile psiFile);

    /**
     * Checks whether file is accepted by coverage filters or not. Is used in Project View Nodes annotator.
     *
     * @param psiFile Psi file
     * @param suite   Coverage suite
     * @return true if included in coverage
     */
    public abstract boolean acceptedByFilters(@Nonnull final PsiFile psiFile, @Nonnull final CoverageSuitesBundle suite);

    /**
     * E.g. all *.class files for java source file with several classes
     *
     * @param srcFile
     * @param module
     * @return files
     */
    @Nonnull
    public Set<File> getCorrespondingOutputFiles(
        @Nonnull final PsiFile srcFile,
        @Nullable final Module module,
        @Nonnull final CoverageSuitesBundle suite
    ) {
        final VirtualFile virtualFile = srcFile.getVirtualFile();
        return virtualFile == null ? Collections.<File>emptySet() : Collections.singleton(VirtualFileUtil.virtualToIoFile(virtualFile));
    }

    /**
     * When output directory is empty we probably should recompile source and then choose suite again
     *
     * @param module
     * @param chooseSuiteAction @return True if should stop and wait compilation (e.g. for Java). False if we can ignore output (e.g. for Ruby)
     */
    public abstract boolean recompileProjectAndRerunAction(
        @Nonnull final Module module,
        @Nonnull final CoverageSuitesBundle suite,
        @Nonnull final Runnable chooseSuiteAction
    );

    /**
     * Qualified name same as in coverage raw project data
     * E.g. java class qualified name by *.class file of some Java class in corresponding source file
     *
     * @param outputFile
     * @param sourceFile
     * @return
     */
    @Nullable
    public String getQualifiedName(@Nonnull final File outputFile, @Nonnull final PsiFile sourceFile) {
        final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(outputFile);
        if (virtualFile != null) {
            return getQualifiedName(virtualFile, sourceFile);
        }
        return null;
    }

    @Deprecated
    @Nullable
    public String getQualifiedName(@Nonnull final VirtualFile outputFile, @Nonnull final PsiFile sourceFile) {
        return null;
    }

    @Nonnull
    public abstract Set<String> getQualifiedNames(@Nonnull final PsiFile sourceFile);

    /**
     * Decide include a file or not in coverage report if coverage data isn't available for the file. E.g file wasn't touched by coverage
     * util
     *
     * @param qualifiedName
     * @param outputFile
     * @param sourceFile
     * @param suite
     * @return
     */
    public boolean includeUntouchedFileInCoverage(
        @Nonnull final String qualifiedName,
        @Nonnull final File outputFile,
        @Nonnull final PsiFile sourceFile,
        @Nonnull final CoverageSuitesBundle suite
    ) {
        final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(outputFile);
        if (virtualFile != null) {
            return includeUntouchedFileInCoverage(qualifiedName, virtualFile, sourceFile, suite);
        }
        return false;
    }

    @Deprecated
    public boolean includeUntouchedFileInCoverage(
        @Nonnull final String qualifiedName,
        @Nonnull final VirtualFile outputFile,
        @Nonnull final PsiFile sourceFile,
        @Nonnull final CoverageSuitesBundle suite
    ) {
        return false;
    }

    /**
     * Collect code lines if untouched file should be included in coverage information. These lines will be marked as uncovered.
     *
     * @param suite
     * @return List (probably empty) of code lines or null if all lines should be marked as uncovered
     */
    @Nullable
    public List<Integer> collectSrcLinesForUntouchedFile(@Nonnull final File classFile, @Nonnull final CoverageSuitesBundle suite) {
        final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(classFile);
        if (virtualFile != null) {
            return collectSrcLinesForUntouchedFile(virtualFile, suite);
        }
        return null;
    }

    @Deprecated
    @Nullable
    public List<Integer> collectSrcLinesForUntouchedFile(@Nonnull final VirtualFile classFile, @Nonnull final CoverageSuitesBundle suite) {
        return null;
    }

    /**
     * Content of brief report which will be shown by click on coverage icon
     *
     * @param editor
     * @param psiFile
     * @param lineNumber
     * @param startOffset
     * @param endOffset
     * @param lineData
     * @return
     */
    public String generateBriefReport(
        @Nonnull Editor editor,
        @Nonnull PsiFile psiFile,
        int lineNumber,
        int startOffset,
        int endOffset,
        @Nullable LineData lineData
    ) {
        final int hits = lineData == null ? 0 : lineData.getHits();
        return "Hits: " + hits;
    }

    public abstract List<PsiElement> findTestsByNames(@Nonnull final String[] testNames, @Nonnull final Project project);

    @Nullable
    public abstract String getTestMethodName(@Nonnull final PsiElement element, @Nonnull final AbstractTestProxy testProxy);

    /**
     * @return true to enable 'Generate Coverage Report...' action
     */
    public boolean isReportGenerationAvailable(
        @Nonnull Project project,
        @Nonnull DataContext dataContext,
        @Nonnull CoverageSuitesBundle currentSuite
    ) {
        return false;
    }

    public void generateReport(
        @Nonnull final Project project,
        @Nonnull final DataContext dataContext,
        @Nonnull final CoverageSuitesBundle currentSuite
    ) {
    }

    public abstract String getPresentableText();

    public boolean coverageProjectViewStatisticsApplicableTo(VirtualFile fileOrDir) {
        return false;
    }

    public Object[] postProcessExecutableLines(Object[] lines, Editor editor) {
        return lines;
    }

    public boolean shouldHighlightFullLines() {
        return false;
    }

    public static String getEditorTitle() {
        return "Code Coverage";
    }

    public CoverageViewExtension createCoverageViewExtension(
        Project project,
        CoverageSuitesBundle suiteBundle,
        CoverageViewManager.StateBean stateBean
    ) {
        return null;
    }

    public boolean isInLibraryClasses(Project project, VirtualFile file) {
        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        return projectFileIndex.isInLibraryClasses(file);
    }
}
