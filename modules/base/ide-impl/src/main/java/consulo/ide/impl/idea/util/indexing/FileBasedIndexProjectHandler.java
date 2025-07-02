/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.indexing;

import consulo.application.impl.internal.performance.PerformanceWatcher;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.Processor;
import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.openapi.project.CacheUpdateRunner;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.project.DumbModeTask;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * @author max
 */
public class FileBasedIndexProjectHandler {
    private static final Logger LOG = Logger.getInstance(FileBasedIndexProjectHandler.class);

    public static final int ourMinFilesToStartDumMode = Registry.intValue("ide.dumb.mode.minFilesToStart", 20);
    private static final int ourMinFilesSizeToStartDumMode = Registry.intValue("ide.dumb.mode.minFilesSizeToStart", 1048576);

    @Nullable
    public static DumbModeTask createChangedFilesIndexingTask(final Project project) {
        final FileBasedIndex i = FileBasedIndex.getInstance();
        if (!(i instanceof FileBasedIndexImpl) || !IndexInfrastructure.hasIndices()) {
            return null;
        }

        FileBasedIndexImpl index = (FileBasedIndexImpl) i;

        if (!mightHaveManyChangedFilesInProject(project, index)) {
            return null;
        }

        return new DumbModeTask(project) {
            @Override
            public void performInDumbMode(@Nonnull ProgressIndicator indicator, Exception trace) {
                long start = System.currentTimeMillis();
                Collection<VirtualFile> files = index.getFilesToUpdate(project);
                long calcDuration = System.currentTimeMillis() - start;

                indicator.setIndeterminate(false);
                indicator.setTextValue(IdeLocalize.progressIndexingUpdating());

                LOG.info("Reindexing refreshed files: " + files.size() + " to update, calculated in " + calcDuration + "ms");
                if (!files.isEmpty()) {
                    PerformanceWatcher.Snapshot snapshot = PerformanceWatcher.takeSnapshot();
                    reindexRefreshedFiles(indicator, files, project, index);
                    snapshot.logResponsivenessSinceCreation("Reindexing refreshed files");
                }
            }

            @Override
            public String toString() {
                StringBuilder sampleOfChangedFilePathsToBeIndexed = new StringBuilder();

                index.processChangedFiles(project, new Processor<>() {
                    int filesInProjectToBeIndexed;
                    final String projectBasePath = project.getBasePath();

                    @Override
                    public boolean process(VirtualFile file) {
                        if (filesInProjectToBeIndexed != 0) {
                            sampleOfChangedFilePathsToBeIndexed.append(", ");
                        }

                        String filePath = file.getPath();
                        String loggedPath = projectBasePath != null ? FileUtil.getRelativePath(projectBasePath, filePath, '/') : null;
                        if (loggedPath == null) {
                            loggedPath = filePath;
                        }
                        else {
                            loggedPath = "%project_path%/" + loggedPath;
                        }
                        sampleOfChangedFilePathsToBeIndexed.append(loggedPath);

                        return ++filesInProjectToBeIndexed < ourMinFilesToStartDumMode;
                    }
                });
                return super.toString() + " [" + project + ", " + sampleOfChangedFilePathsToBeIndexed + "]";
            }
        };
    }

    private static boolean mightHaveManyChangedFilesInProject(Project project, FileBasedIndexImpl index) {
        long start = System.currentTimeMillis();
        return !index.processChangedFiles(project, new Processor<>() {
            int filesInProjectToBeIndexed;
            long sizeOfFilesToBeIndexed;

            @Override
            public boolean process(VirtualFile file) {
                ++filesInProjectToBeIndexed;
                if (file.isValid() && !file.isDirectory()) {
                    sizeOfFilesToBeIndexed += file.getLength();
                }
                return filesInProjectToBeIndexed < ourMinFilesToStartDumMode && sizeOfFilesToBeIndexed < ourMinFilesSizeToStartDumMode && System.currentTimeMillis() < start + 100;
            }
        });
    }

    private static void reindexRefreshedFiles(ProgressIndicator indicator, Collection<VirtualFile> files, final Project project, final FileBasedIndexImpl index) {
        CacheUpdateRunner.processFiles(indicator, files, project, content -> index.processRefreshedFile(project, content));
    }
}
