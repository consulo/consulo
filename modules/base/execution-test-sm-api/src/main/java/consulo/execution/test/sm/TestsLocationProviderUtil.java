/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.test.sm;

import consulo.application.Application;
import consulo.execution.test.sm.internal.SMTestHelper;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.lang.text.StringTokenizer;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.TempFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Roman Chernyatchik
 */
public class TestsLocationProviderUtil {
    private static final String PROTOCOL_SEPARATOR = "://";
    private static final int MIN_PROXIMITY_THRESHOLD = 1;

    private TestsLocationProviderUtil() {
    }

    @Nullable
    public static String extractPath(@Nonnull String locationUrl) {
        int index = locationUrl.indexOf(PROTOCOL_SEPARATOR);
        if (index >= 0) {
            return locationUrl.substring(index + PROTOCOL_SEPARATOR.length());
        }
        return null;
    }

    public static List<VirtualFile> findSuitableFilesFor(String filePath, Project project) {
        ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();

        // at first let's try to find file as is, by it's real path
        // and check that file belongs to current project
        // this location provider designed for tests thus we will check only project content
        // (we cannot check just sources or tests folders because RM doesn't use it
        VirtualFile file = getByFullPath(filePath);
        boolean inProjectContent = file != null && (index.isInContent(file));

        if (inProjectContent) {
            return Collections.singletonList(file);
        }

        //split file by "/" in parts
        List<String> folders = new LinkedList<>();
        StringTokenizer st = new StringTokenizer(filePath, "/", false);
        String fileName = null;
        while (st.hasMoreTokens()) {
            String pathComponent = st.nextToken();
            if (st.hasMoreTokens()) {
                folders.addFirst(pathComponent);
            }
            else {
                // last token
                fileName = pathComponent;
            }
        }
        if (fileName == null) {
            return Collections.emptyList();
        }
        return findFilesClosestToTarget(folders, collectCandidates(project, fileName, true), MIN_PROXIMITY_THRESHOLD);
    }

    /**
     * Looks for files with given name which are close to given path
     *
     * @param targetParentFolders   folders path
     * @param candidates
     * @param minProximityThreshold
     * @return
     */
    public static List<VirtualFile> findFilesClosestToTarget(
        @Nonnull List<String> targetParentFolders,
        List<FileInfo> candidates,
        int minProximityThreshold
    ) {
        // let's find all files with similar relative path

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // let's iterate relative path components and determine which files are closer to our relative path
        for (String folderName : targetParentFolders) {
            for (FileInfo info : candidates) {
                info.processRelativePathComponent(folderName);
            }
        }

        // let's extract the closest files to relative path. For this we will find max proximity and  and
        // we also assume that relative files and folders should have at least one common parent folder - just
        // to remove false positives on some cases
        int maxProximity = 0;
        for (FileInfo fileInfo : candidates) {
            int proximity = fileInfo.getProximity();
            if (proximity > maxProximity) {
                maxProximity = proximity;
            }
        }

        if (maxProximity >= minProximityThreshold) {
            List<VirtualFile> files = new ArrayList<>();
            for (FileInfo info : candidates) {
                if (info.getProximity() == maxProximity) {
                    files.add(info.getFile());
                }
            }
            return files;
        }

        return Collections.emptyList();
    }

    public static List<FileInfo> collectCandidates(Project project, String fileName, boolean includeNonProjectItems) {
        return project.getInstance(SMTestHelper.class).collectCandidates(project, fileName, includeNonProjectItems);
    }

    @Nullable
    private static VirtualFile getByFullPath(String filePath) {
        VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (fileByPath != null) {
            return fileByPath;
        }
        // if we are in UnitTest mode probably TempFileSystem is used instead of LocalFileSystem
        if (Application.get().isUnitTestMode()) {
            return TempFileSystem.getInstance().findFileByPath(filePath);
        }
        return null;
    }

    public static class FileInfo {
        private final VirtualFile myFile;
        private VirtualFile myCurrentFolder;
        private int myProximity = 0;

        public FileInfo(VirtualFile file) {
            myFile = file;
            myCurrentFolder = myFile.getParent();
        }

        public void processRelativePathComponent(String folderName) {
            if (myCurrentFolder == null) {
                return;
            }

            if (!folderName.equals(myCurrentFolder.getName())) {
                // if one of path components differs - no sense in checking others
                myCurrentFolder = null;
                return;
            }

            // common folder was found, let's increase proximity degree and move to parent folder
            myProximity++;
            myCurrentFolder = myCurrentFolder.getParent();
        }

        public VirtualFile getFile() {
            return myFile;
        }

        public int getProximity() {
            return myProximity;
        }
    }
}
