package consulo.execution.coverage;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.content.TestSourcesFilter;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author traff
 */
public abstract class SimpleCoverageAnnotator extends BaseCoverageAnnotator {
    private final Map<String, FileCoverageInfo> myFileCoverageInfos = new HashMap<>();
    private final Map<String, DirCoverageInfo> myTestDirCoverageInfos = new HashMap<>();
    private final Map<String, DirCoverageInfo> myDirCoverageInfos = new HashMap<>();

    public SimpleCoverageAnnotator(Project project) {
        super(project);
    }

    @Override
    public void onSuiteChosen(CoverageSuitesBundle newSuite) {
        super.onSuiteChosen(newSuite);

        myFileCoverageInfos.clear();
        myTestDirCoverageInfos.clear();
        myDirCoverageInfos.clear();
    }

    @Nullable
    protected DirCoverageInfo getDirCoverageInfo(@Nonnull final PsiDirectory directory, @Nonnull final CoverageSuitesBundle currentSuite) {
        final VirtualFile dir = directory.getVirtualFile();

        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(directory.getProject()).getFileIndex();
        //final Module module = projectFileIndex.getModuleForFile(dir);

        final boolean isInTestContent = projectFileIndex.isInTestSourceContent(dir);
        if (!currentSuite.isTrackTestFolders() && isInTestContent) {
            return null;
        }

        final String path = normalizeFilePath(dir.getPath());

        return isInTestContent ? myTestDirCoverageInfos.get(path) : myDirCoverageInfos.get(path);
    }

    @Nullable
    public String getDirCoverageInformationString(
        @Nonnull final PsiDirectory directory,
        @Nonnull final CoverageSuitesBundle currentSuite,
        @Nonnull final CoverageDataManager manager
    ) {
        DirCoverageInfo coverageInfo = getDirCoverageInfo(directory, currentSuite);
        if (coverageInfo == null) {
            return null;
        }

        if (manager.isSubCoverageActive()) {
            return coverageInfo.coveredLineCount > 0 ? "covered" : null;
        }

        final String filesCoverageInfo = getFilesCoverageInformationString(coverageInfo);
        if (filesCoverageInfo != null) {
            final StringBuilder builder = new StringBuilder();
            builder.append(filesCoverageInfo);
            final String linesCoverageInfo = getLinesCoverageInformationString(coverageInfo);
            if (linesCoverageInfo != null) {
                builder.append(", ").append(linesCoverageInfo);
            }
            return builder.toString();
        }
        return null;
    }

    // SimpleCoverageAnnotator doesn't require normalized file paths any more
    // so now coverage report should work w/o usage of this method
    @Deprecated
    public static String getFilePath(final String filePath) {
        return normalizeFilePath(filePath);
    }

    @Nonnull
    private static String normalizeFilePath(@Nonnull String filePath) {
        if (Platform.current().os().isWindows()) {
            filePath = filePath.toLowerCase();
        }
        return FileUtil.toSystemIndependentName(filePath);
    }

    @Nullable
    public String getFileCoverageInformationString(
        @Nonnull final PsiFile psiFile,
        @Nonnull final CoverageSuitesBundle currentSuite,
        @Nonnull final CoverageDataManager manager
    ) {
        final VirtualFile file = psiFile.getVirtualFile();
        assert file != null;
        final String path = normalizeFilePath(file.getPath());

        final FileCoverageInfo coverageInfo = myFileCoverageInfos.get(path);
        if (coverageInfo == null) {
            return null;
        }

        if (manager.isSubCoverageActive()) {
            return coverageInfo.coveredLineCount > 0 ? "covered" : null;
        }

        return getLinesCoverageInformationString(coverageInfo);
    }

    @Nullable
    protected FileCoverageInfo collectBaseFileCoverage(
        @Nonnull final VirtualFile file,
        @Nonnull final Annotator annotator,
        @Nonnull final ProjectData projectData,
        @Nonnull final Map<String, String> normalizedFiles2Files
    ) {
        final String filePath = normalizeFilePath(file.getPath());

        // process file
        final FileCoverageInfo info;

        final ClassData classData = getClassData(filePath, projectData, normalizedFiles2Files);
        if (classData != null) {
            // fill info from coverage data
            info = fileInfoForCoveredFile(classData);
        }
        else {
            // file wasn't mentioned in coverage information
            info = fillInfoForUncoveredFile(VirtualFileUtil.virtualToIoFile(file));
        }

        if (info != null) {
            annotator.annotateFile(filePath, info);
        }
        return info;
    }

    private static
    @Nullable
    ClassData getClassData(
        final @Nonnull String filePath,
        final @Nonnull ProjectData data,
        final @Nonnull Map<String, String> normalizedFiles2Files
    ) {
        final String originalFileName = normalizedFiles2Files.get(filePath);
        if (originalFileName == null) {
            return null;
        }
        return data.getClassData(originalFileName);
    }

    @Nullable
    protected DirCoverageInfo collectFolderCoverage(
        @Nonnull final VirtualFile dir,
        final @Nonnull CoverageDataManager dataManager,
        final Annotator annotator,
        final ProjectData projectInfo,
        boolean trackTestFolders,
        @Nonnull final ProjectFileIndex index,
        @Nonnull final CoverageEngine coverageEngine,
        Set<VirtualFile> visitedDirs,
        @Nonnull final Map<String, String> normalizedFiles2Files
    ) {
        if (!index.isInContent(dir)) {
            return null;
        }

        if (visitedDirs.contains(dir)) {
            return null;
        }

        visitedDirs.add(dir);

        final boolean isInTestSrcContent = TestSourcesFilter.isTestSources(dir, getProject());

        // Don't count coverage for tests folders if track test folders is switched off
        if (!trackTestFolders && isInTestSrcContent) {
            return null;
        }

        final VirtualFile[] children = dataManager.doInReadActionIfProjectOpen(dir::getChildren);

        if (children == null) {
            return null;
        }

        final DirCoverageInfo dirCoverageInfo = new DirCoverageInfo();

        for (VirtualFile fileOrDir : children) {
            if (fileOrDir.isDirectory()) {
                final DirCoverageInfo childCoverageInfo = collectFolderCoverage(
                    fileOrDir,
                    dataManager,
                    annotator,
                    projectInfo,
                    trackTestFolders,
                    index,
                    coverageEngine,
                    visitedDirs,
                    normalizedFiles2Files
                );

                if (childCoverageInfo != null) {
                    dirCoverageInfo.totalFilesCount += childCoverageInfo.totalFilesCount;
                    dirCoverageInfo.coveredFilesCount += childCoverageInfo.coveredFilesCount;
                    dirCoverageInfo.totalLineCount += childCoverageInfo.totalLineCount;
                    dirCoverageInfo.coveredLineCount += childCoverageInfo.coveredLineCount;
                }
            }
            else if (coverageEngine.coverageProjectViewStatisticsApplicableTo(fileOrDir)) {
                // let's count statistics only for ruby-based files

                final FileCoverageInfo fileInfo = collectBaseFileCoverage(fileOrDir, annotator, projectInfo, normalizedFiles2Files);

                if (fileInfo != null) {
                    dirCoverageInfo.totalLineCount += fileInfo.totalLineCount;
                    dirCoverageInfo.totalFilesCount++;

                    if (fileInfo.coveredLineCount > 0) {
                        dirCoverageInfo.coveredFilesCount++;
                        dirCoverageInfo.coveredLineCount += fileInfo.coveredLineCount;
                    }
                }
            }
        }


        //TODO - toplevelFilesCoverage - is unused variable!

        // no sense to include directories without ruby files
        if (dirCoverageInfo.totalFilesCount == 0) {
            return null;
        }

        final String dirPath = normalizeFilePath(dir.getPath());
        if (isInTestSrcContent) {
            annotator.annotateTestDirectory(dirPath, dirCoverageInfo);
        }
        else {
            annotator.annotateSourceDirectory(dirPath, dirCoverageInfo);
        }

        return dirCoverageInfo;
    }

    public void annotate(
        @Nonnull final VirtualFile contentRoot,
        @Nonnull final CoverageSuitesBundle suite,
        final @Nonnull CoverageDataManager dataManager,
        @Nonnull final ProjectData data,
        final Project project,
        final Annotator annotator
    ) {
        if (!contentRoot.isValid()) {
            return;
        }

        // TODO: check name filter!!!!!

        final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();

        @SuppressWarnings("unchecked") final Set<String> files = data.getClasses().keySet();
        final Map<String, String> normalizedFiles2Files = new HashMap<>();
        for (final String file : files) {
            normalizedFiles2Files.put(normalizeFilePath(file), file);
        }
        collectFolderCoverage(
            contentRoot,
            dataManager,
            annotator,
            data,
            suite.isTrackTestFolders(),
            index,
            suite.getCoverageEngine(),
            new HashSet<>(),
            Collections.unmodifiableMap(normalizedFiles2Files)
        );
    }

    @Override
    @Nullable
    protected Runnable createRenewRequest(@Nonnull final CoverageSuitesBundle suite, final @Nonnull CoverageDataManager dataManager) {
        final ProjectData data = suite.getCoverageData();
        if (data == null) {
            return null;
        }

        return new Runnable() {
            public void run() {
                final Project project = getProject();

                final ProjectRootManager rootManager = ProjectRootManager.getInstance(project);

                // find all modules content roots
                final VirtualFile[] modulesContentRoots = dataManager.doInReadActionIfProjectOpen(rootManager::getContentRoots);

                if (modulesContentRoots == null) {
                    return;
                }

                // gather coverage from all content roots
                for (VirtualFile root : modulesContentRoots) {
                    annotate(root, suite, dataManager, data, project, new Annotator() {
                        public void annotateSourceDirectory(final String dirPath, final DirCoverageInfo info) {
                            myDirCoverageInfos.put(dirPath, info);
                        }

                        public void annotateTestDirectory(final String dirPath, final DirCoverageInfo info) {
                            myTestDirCoverageInfos.put(dirPath, info);
                        }

                        public void annotateFile(@Nonnull final String filePath, @Nonnull final FileCoverageInfo info) {
                            myFileCoverageInfos.put(filePath, info);
                        }
                    });
                }

                //final VirtualFile[] roots = ProjectRootManagerEx.getInstanceEx(project).getContentRootsFromAllModules();
                //index.iterateContentUnderDirectory(roots[0], new ContentIterator() {
                //  public boolean processFile(final VirtualFile fileOrDir) {
                //    // TODO support for libraries and sdk
                //    if (index.isInContent(fileOrDir)) {
                //      final String normalizedPath = RubyCoverageEngine.rcovalizePath(fileOrDir.getPath(), (RubyCoverageSuite)suite);
                //
                //      // TODO - check filters
                //
                //      if (fileOrDir.isDirectory()) {
                //        //// process dir
                //        //if (index.isInTestSourceContent(fileOrDir)) {
                //        //  //myTestDirCoverageInfos.put(RubyCoverageEngine.rcovalizePath(fileOrDir.getPath(), (RubyCoverageSuite)suite), )
                //        //} else {
                //        //  myDirCoverageInfos.put(normalizedPath, new FileCoverageInfo());
                //        //}
                //      } else {
                //        // process file
                //        final ClassData classData = data.getOrCreateClassData(normalizedPath);
                //        if (classData != null) {
                //          final int count = classData.getLines().length;
                //          if (count != 0) {
                //            final FileCoverageInfo info = new FileCoverageInfo();
                //            info.totalLineCount = count;
                //            // let's count covered lines
                //            for (int i = 1; i <= count; i++) {
                //              final LineData lineData = classData.getLineData(i);
                //              if (lineData.getStatus() != LineCoverage.NONE){
                //                info.coveredLineCount++;
                //              }
                //            }
                //            myFileCoverageInfos.put(normalizedPath, info);
                //          }
                //        }
                //      }
                //    }
                //    return true;
                //  }
                //});

                dataManager.triggerPresentationUpdate();
            }
        };
    }

    @Nullable
    protected String getLinesCoverageInformationString(@Nonnull final FileCoverageInfo info) {
        return calcCoveragePercentage(info) + "% lines covered";
    }

    protected static int calcCoveragePercentage(FileCoverageInfo info) {
        return calcPercent(info.coveredLineCount, info.totalLineCount);
    }

    private static int calcPercent(final int covered, final int total) {
        return total != 0 ? (int) ((double) covered / total * 100) : 100;
    }

    @Nullable
    protected String getFilesCoverageInformationString(@Nonnull final DirCoverageInfo info) {
        return calcPercent(info.coveredFilesCount, info.totalFilesCount) + "% files";
    }

    @Nullable
    private static FileCoverageInfo fileInfoForCoveredFile(@Nonnull final ClassData classData) {
        final Object[] lines = classData.getLines();

        // class data lines = [0, 1, ... count] but first element with index = #0 is fake and isn't
        // used thus count = length = 1
        final int count = lines.length - 1;

        if (count == 0) {
            return null;
        }

        final FileCoverageInfo info = new FileCoverageInfo();

        int srcLinesCount = 0;
        int coveredLinesCount = 0;
        // let's count covered lines
        for (int i = 1; i <= count; i++) {
            final LineData lineData = classData.getLineData(i);
            if (lineData == null) {
                // Ignore not src code
                continue;
            }
            final int status = lineData.getStatus();
            // covered - if src code & covered (or inferred covered)
            if (status != LineCoverage.NONE) {
                coveredLinesCount++;
            }
            srcLinesCount++;
        }
        info.totalLineCount = srcLinesCount;
        info.coveredLineCount = coveredLinesCount;
        return info;
    }

    @Nullable
    protected FileCoverageInfo fillInfoForUncoveredFile(@Nonnull File file) {
        return null;
    }

    private interface Annotator {
        void annotateSourceDirectory(final String dirPath, final DirCoverageInfo info);

        void annotateTestDirectory(final String dirPath, final DirCoverageInfo info);

        void annotateFile(@Nonnull final String filePath, @Nonnull final FileCoverageInfo info);
    }
}
