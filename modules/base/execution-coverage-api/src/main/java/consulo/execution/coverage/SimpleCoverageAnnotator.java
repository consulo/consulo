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
    protected DirCoverageInfo getDirCoverageInfo(@Nonnull PsiDirectory directory, @Nonnull CoverageSuitesBundle currentSuite) {
        VirtualFile dir = directory.getVirtualFile();

        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(directory.getProject()).getFileIndex();
        //final Module module = projectFileIndex.getModuleForFile(dir);

        boolean isInTestContent = projectFileIndex.isInTestSourceContent(dir);
        if (!currentSuite.isTrackTestFolders() && isInTestContent) {
            return null;
        }

        String path = normalizeFilePath(dir.getPath());

        return isInTestContent ? myTestDirCoverageInfos.get(path) : myDirCoverageInfos.get(path);
    }

    @Nullable
    @Override
    public String getDirCoverageInformationString(
        @Nonnull PsiDirectory directory,
        @Nonnull CoverageSuitesBundle currentSuite,
        @Nonnull CoverageDataManager manager
    ) {
        DirCoverageInfo coverageInfo = getDirCoverageInfo(directory, currentSuite);
        if (coverageInfo == null) {
            return null;
        }

        if (manager.isSubCoverageActive()) {
            return coverageInfo.coveredLineCount > 0 ? "covered" : null;
        }

        String filesCoverageInfo = getFilesCoverageInformationString(coverageInfo);
        if (filesCoverageInfo != null) {
            StringBuilder builder = new StringBuilder();
            builder.append(filesCoverageInfo);
            String linesCoverageInfo = getLinesCoverageInformationString(coverageInfo);
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
    public static String getFilePath(String filePath) {
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
    @Override
    public String getFileCoverageInformationString(
        @Nonnull PsiFile psiFile,
        @Nonnull CoverageSuitesBundle currentSuite,
        @Nonnull CoverageDataManager manager
    ) {
        VirtualFile file = psiFile.getVirtualFile();
        assert file != null;
        String path = normalizeFilePath(file.getPath());

        FileCoverageInfo coverageInfo = myFileCoverageInfos.get(path);
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
        @Nonnull VirtualFile file,
        @Nonnull Annotator annotator,
        @Nonnull ProjectData projectData,
        @Nonnull Map<String, String> normalizedFiles2Files
    ) {
        String filePath = normalizeFilePath(file.getPath());

        // process file
        FileCoverageInfo info;

        ClassData classData = getClassData(filePath, projectData, normalizedFiles2Files);
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

    @Nullable
    private static ClassData getClassData(
        @Nonnull String filePath,
        @Nonnull ProjectData data,
        @Nonnull Map<String, String> normalizedFiles2Files
    ) {
        String originalFileName = normalizedFiles2Files.get(filePath);
        if (originalFileName == null) {
            return null;
        }
        return data.getClassData(originalFileName);
    }

    @Nullable
    protected DirCoverageInfo collectFolderCoverage(
        @Nonnull VirtualFile dir,
        @Nonnull CoverageDataManager dataManager,
        Annotator annotator,
        ProjectData projectInfo,
        boolean trackTestFolders,
        @Nonnull ProjectFileIndex index,
        @Nonnull CoverageEngine coverageEngine,
        Set<VirtualFile> visitedDirs,
        @Nonnull Map<String, String> normalizedFiles2Files
    ) {
        if (!index.isInContent(dir)) {
            return null;
        }

        if (visitedDirs.contains(dir)) {
            return null;
        }

        visitedDirs.add(dir);

        boolean isInTestSrcContent = TestSourcesFilter.isTestSources(dir, getProject());

        // Don't count coverage for tests folders if track test folders is switched off
        if (!trackTestFolders && isInTestSrcContent) {
            return null;
        }

        VirtualFile[] children = dataManager.doInReadActionIfProjectOpen(dir::getChildren);

        if (children == null) {
            return null;
        }

        DirCoverageInfo dirCoverageInfo = new DirCoverageInfo();

        for (VirtualFile fileOrDir : children) {
            if (fileOrDir.isDirectory()) {
                DirCoverageInfo childCoverageInfo = collectFolderCoverage(
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

                FileCoverageInfo fileInfo = collectBaseFileCoverage(fileOrDir, annotator, projectInfo, normalizedFiles2Files);

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

        String dirPath = normalizeFilePath(dir.getPath());
        if (isInTestSrcContent) {
            annotator.annotateTestDirectory(dirPath, dirCoverageInfo);
        }
        else {
            annotator.annotateSourceDirectory(dirPath, dirCoverageInfo);
        }

        return dirCoverageInfo;
    }

    public void annotate(
        @Nonnull VirtualFile contentRoot,
        @Nonnull CoverageSuitesBundle suite,
        @Nonnull CoverageDataManager dataManager,
        @Nonnull ProjectData data,
        Project project,
        Annotator annotator
    ) {
        if (!contentRoot.isValid()) {
            return;
        }

        // TODO: check name filter!!!!!

        ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();

        @SuppressWarnings("unchecked") Set<String> files = data.getClasses().keySet();
        Map<String, String> normalizedFiles2Files = new HashMap<>();
        for (String file : files) {
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

    @Nullable
    @Override
    protected Runnable createRenewRequest(@Nonnull CoverageSuitesBundle suite, @Nonnull CoverageDataManager dataManager) {
        ProjectData data = suite.getCoverageData();
        if (data == null) {
            return null;
        }

        return () -> {
            Project project = getProject();

            ProjectRootManager rootManager = ProjectRootManager.getInstance(project);

            // find all modules content roots
            VirtualFile[] modulesContentRoots = dataManager.doInReadActionIfProjectOpen(rootManager::getContentRoots);

            if (modulesContentRoots == null) {
                return;
            }

            // gather coverage from all content roots
            for (VirtualFile root : modulesContentRoots) {
                annotate(
                    root,
                    suite,
                    dataManager,
                    data,
                    project,
                    new Annotator() {
                        @Override
                        public void annotateSourceDirectory(String dirPath, DirCoverageInfo info) {
                            myDirCoverageInfos.put(dirPath, info);
                        }

                        @Override
                        public void annotateTestDirectory(String dirPath, DirCoverageInfo info) {
                            myTestDirCoverageInfos.put(dirPath, info);
                        }

                        @Override
                        public void annotateFile(@Nonnull String filePath, @Nonnull FileCoverageInfo info) {
                            myFileCoverageInfos.put(filePath, info);
                        }
                    }
                );
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
        };
    }

    @Nullable
    protected String getLinesCoverageInformationString(@Nonnull FileCoverageInfo info) {
        return calcCoveragePercentage(info) + "% lines covered";
    }

    protected static int calcCoveragePercentage(FileCoverageInfo info) {
        return calcPercent(info.coveredLineCount, info.totalLineCount);
    }

    private static int calcPercent(int covered, int total) {
        return total != 0 ? (int) ((double) covered / total * 100) : 100;
    }

    @Nullable
    protected String getFilesCoverageInformationString(@Nonnull DirCoverageInfo info) {
        return calcPercent(info.coveredFilesCount, info.totalFilesCount) + "% files";
    }

    @Nullable
    private static FileCoverageInfo fileInfoForCoveredFile(@Nonnull ClassData classData) {
        Object[] lines = classData.getLines();

        // class data lines = [0, 1, ... count] but first element with index = #0 is fake and isn't
        // used thus count = length = 1
        int count = lines.length - 1;

        if (count == 0) {
            return null;
        }

        FileCoverageInfo info = new FileCoverageInfo();

        int srcLinesCount = 0;
        int coveredLinesCount = 0;
        // let's count covered lines
        for (int i = 1; i <= count; i++) {
            LineData lineData = classData.getLineData(i);
            if (lineData == null) {
                // Ignore not src code
                continue;
            }
            int status = lineData.getStatus();
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
        void annotateSourceDirectory(String dirPath, DirCoverageInfo info);

        void annotateTestDirectory(String dirPath, DirCoverageInfo info);

        void annotateFile(@Nonnull String filePath, @Nonnull FileCoverageInfo info);
    }
}
