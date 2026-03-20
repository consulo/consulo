package consulo.execution.coverage;

import com.intellij.rt.coverage.data.LineData;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
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
import org.jspecify.annotations.Nullable;

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
    /**
     * Checks whether coverage feature is supported by this engine for given configuration or not.
     *
     * @param conf Run Configuration
     * @return True if coverage for given run configuration is supported by this engine
     */
    public abstract boolean isApplicableTo(@Nullable RunConfigurationBase conf);

    public abstract boolean canHavePerTestCoverage(@Nullable RunConfigurationBase conf);

    /**
     * Creates coverage enabled configuration for given RunConfiguration. It is supposed that one run configuration may be associated
     * not more than one coverage engine.
     *
     * @param conf Run Configuration
     * @return Coverage enabled configuration with engine specific settings
     */
    public abstract CoverageEnabledConfiguration createCoverageEnabledConfiguration(@Nullable RunConfigurationBase conf);

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
    public @Nullable CoverageSuite createCoverageSuite(
        CoverageRunner covRunner,
        String name,
        CoverageFileProvider coverageDataFileProvider,
        @Nullable String[] filters,
        long lastCoverageTimeStamp,
        @Nullable String suiteToMerge,
        boolean coverageByTestEnabled,
        boolean tracingEnabled,
        boolean trackTestFolders
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
    public abstract @Nullable CoverageSuite createCoverageSuite(
        CoverageRunner covRunner,
        String name,
        CoverageFileProvider coverageDataFileProvider,
        @Nullable String[] filters,
        long lastCoverageTimeStamp,
        @Nullable String suiteToMerge,
        boolean coverageByTestEnabled,
        boolean tracingEnabled,
        boolean trackTestFolders,
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
    public abstract @Nullable CoverageSuite createCoverageSuite(
        CoverageRunner covRunner,
        String name,
        CoverageFileProvider coverageDataFileProvider,
        CoverageEnabledConfiguration config
    );

    public abstract @Nullable CoverageSuite createEmptyCoverageSuite(CoverageRunner coverageRunner);

    /**
     * Coverage annotator which annotates smth(e.g. Project view nodes / editor) with coverage information
     *
     * @param project Project
     * @return Annotator
     */
    public abstract CoverageAnnotator getCoverageAnnotator(Project project);

    /**
     * Determines if coverage information should be displayed for given file. E.g. coverage may be applicable
     * only to user source files or only for files of specific types
     *
     * @param psiFile file
     * @return false if coverage N/A for given file
     */
    public abstract boolean coverageEditorHighlightingApplicableTo(PsiFile psiFile);

    /**
     * Checks whether file is accepted by coverage filters or not. Is used in Project View Nodes annotator.
     *
     * @param psiFile Psi file
     * @param suite   Coverage suite
     * @return true if included in coverage
     */
    public abstract boolean acceptedByFilters(PsiFile psiFile, CoverageSuitesBundle suite);

    /**
     * E.g. all *.class files for java source file with several classes
     *
     * @param srcFile
     * @param module
     * @return files
     */
    public Set<File> getCorrespondingOutputFiles(
        PsiFile srcFile,
        @Nullable Module module,
        CoverageSuitesBundle suite
    ) {
        VirtualFile virtualFile = srcFile.getVirtualFile();
        return virtualFile == null ? Collections.<File>emptySet() : Collections.singleton(VirtualFileUtil.virtualToIoFile(virtualFile));
    }

    /**
     * When output directory is empty we probably should recompile source and then choose suite again
     *
     * @param module
     * @param chooseSuiteAction @return True if should stop and wait compilation (e.g. for Java). False if we can ignore output (e.g. for Ruby)
     */
    public abstract boolean recompileProjectAndRerunAction(
        Module module,
        CoverageSuitesBundle suite,
        Runnable chooseSuiteAction
    );

    /**
     * Qualified name same as in coverage raw project data
     * E.g. java class qualified name by *.class file of some Java class in corresponding source file
     */
    public @Nullable String getQualifiedName(File outputFile, PsiFile sourceFile) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(outputFile);
        if (virtualFile != null) {
            return getQualifiedName(virtualFile, sourceFile);
        }
        return null;
    }

    @Deprecated
    public @Nullable String getQualifiedName(VirtualFile outputFile, PsiFile sourceFile) {
        return null;
    }

    
    public abstract Set<String> getQualifiedNames(PsiFile sourceFile);

    /**
     * Decide include a file or not in coverage report if coverage data isn't available for the file. E.g file wasn't touched by coverage
     * util
     */
    public boolean includeUntouchedFileInCoverage(
        String qualifiedName,
        File outputFile,
        PsiFile sourceFile,
        CoverageSuitesBundle suite
    ) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(outputFile);
        return virtualFile != null && includeUntouchedFileInCoverage(qualifiedName, virtualFile, sourceFile, suite);
    }

    @Deprecated
    public boolean includeUntouchedFileInCoverage(
        String qualifiedName,
        VirtualFile outputFile,
        PsiFile sourceFile,
        CoverageSuitesBundle suite
    ) {
        return false;
    }

    /**
     * Collect code lines if untouched file should be included in coverage information. These lines will be marked as uncovered.
     *
     * @param suite
     * @return List (probably empty) of code lines or null if all lines should be marked as uncovered
     */
    public @Nullable List<Integer> collectSrcLinesForUntouchedFile(File classFile, CoverageSuitesBundle suite) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(classFile);
        return virtualFile != null ? collectSrcLinesForUntouchedFile(virtualFile, suite) : null;
    }

    @Deprecated
    public @Nullable List<Integer> collectSrcLinesForUntouchedFile(VirtualFile classFile, CoverageSuitesBundle suite) {
        return null;
    }

    /**
     * Content of brief report which will be shown by click on coverage icon
     */
    public String generateBriefReport(
        Editor editor,
        PsiFile psiFile,
        int lineNumber,
        int startOffset,
        int endOffset,
        @Nullable LineData lineData
    ) {
        int hits = lineData == null ? 0 : lineData.getHits();
        return "Hits: " + hits;
    }

    public abstract List<PsiElement> findTestsByNames(String[] testNames, Project project);

    public abstract @Nullable String getTestMethodName(PsiElement element, AbstractTestProxy testProxy);

    /**
     * @return true to enable 'Generate Coverage Report...' action
     */
    public boolean isReportGenerationAvailable(
        Project project,
        DataContext dataContext,
        CoverageSuitesBundle currentSuite
    ) {
        return false;
    }

    public void generateReport(
        Project project,
        DataContext dataContext,
        CoverageSuitesBundle currentSuite
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
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        return projectFileIndex.isInLibraryClasses(file);
    }
}
