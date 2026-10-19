/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.diff.DiffManager;
import consulo.diff.DiffRequestFactory;
import consulo.diff.request.DiffRequest;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ActionImpl(id = "CompareTwoFiles")
public class CompareFilesAction extends BaseShowDiffAction {
    public static final Key<DiffRequest> DIFF_REQUEST = Key.create("CompareFilesAction.DiffRequest");

    public static final String LAST_USED_FILE_KEY = "two.files.diff.last.used.file";
    public static final String LAST_USED_FOLDER_KEY = "two.files.diff.last.used.folder";

    public CompareFilesAction() {
        super(
            ActionLocalize.actionComparetwofilesText(),
            ActionLocalize.actionComparetwofilesDescription(),
            PlatformIconGroup.actionsDiff()
        );
    }

    @Override
    protected void updatePresentation(AnActionEvent e) {
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);

        String text = "Compare Files";
        if (files != null && files.length == 1) {
            text = "Compare With…";
        }
        else if (files != null && files.length == 2) {
            Type type1 = getType(files[0]);
            Type type2 = getType(files[1]);

            if (type1 != type2) {
                text = "Compare";
            }
            else {
                text = switch (type1) {
                    case FILE -> "Compare Files";
                    case DIRECTORY -> "Compare Directories";
                    case ARCHIVE -> "Compare Archives";
                };
            }
        }
        e.getPresentation().setText(text);
    }

    @Override
    protected boolean isAvailable(AnActionEvent e) {
        DiffRequest request = e.getData(DIFF_REQUEST);
        if (request != null) {
            return true;
        }

        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        if (files == null) {
            return false;
        }

        if (files.length == 0 || files.length > 3) {
            return false;
        }
        boolean hasContent = ContainerUtil.all(Arrays.asList(files), BaseShowDiffAction::hasContent);
        if (!hasContent) {
            return false;
        }

        if (files.length == 3) {
            Set<Type> types = ContainerUtil.map2Set(files, CompareFilesAction::getType);
            if (types.contains(Type.DIRECTORY) || types.contains(Type.ARCHIVE)) {
                return false;
            }
        }

        return true;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        VirtualFile[] data = e.getData(VirtualFile.KEY_OF_ARRAY);
        if (e.getData(DIFF_REQUEST) != null || data == null || data.length != 1) {
            super.actionPerformed(e);
            return;
        }

        Project project = e.getData(Project.KEY);
        VirtualFile file = data[0];
        if (!hasContent(file)) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        chooseOtherFile(project, file).whenCompleteAsync((otherFile, error) -> {
            if (otherFile == null) {
                return;
            }
            DiffManager.getInstance().showDiff(project, DiffRequestFactory.getInstance().createFromFiles(project, file, otherFile));
        }, uiAccess);
    }

    @Override
    protected @Nullable DiffRequest getDiffRequest(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        DiffRequest diffRequest = e.getData(DIFF_REQUEST);
        if (diffRequest != null) {
            return diffRequest;
        }

        VirtualFile[] data = e.getRequiredData(VirtualFile.KEY_OF_ARRAY);
        if (data.length < 2) {
            return null;
        }
        return DiffRequestFactory.getInstance().createFromFiles(project, data[0], data[1]);
    }

    @RequiredUIAccess
    private static CompletableFuture<VirtualFile> chooseOtherFile(@Nullable Project project, VirtualFile file) {
        FileChooserDescriptor descriptor;
        String key;

        Type type = getType(file);
        if (type == Type.DIRECTORY || type == Type.ARCHIVE) {
            descriptor = new FileChooserDescriptor(false, true, true, true, true, false);
            key = LAST_USED_FOLDER_KEY;
        }
        else {
            descriptor = new FileChooserDescriptor(true, false, false, true, true, false);
            key = LAST_USED_FILE_KEY;
        }
        VirtualFile selectedFile = getDefaultSelection(project, key, file);
        return FileChooser.chooseFile(descriptor, project, selectedFile).thenApply(otherFile -> {
            if (otherFile != null) {
                updateDefaultSelection(project, key, otherFile);
            }
            return otherFile;
        });
    }

    
    private static VirtualFile getDefaultSelection(@Nullable Project project, String key, VirtualFile file) {
        if (project == null) {
            return file;
        }
        String path = ProjectPropertiesComponent.getInstance(project).getValue(key);
        if (path == null) {
            return file;
        }
        VirtualFile lastSelection = LocalFileSystem.getInstance().findFileByPath(path);
        return lastSelection != null ? lastSelection : file;
    }

    private static void updateDefaultSelection(@Nullable Project project, String key, VirtualFile file) {
        if (project == null) {
            return;
        }
        ProjectPropertiesComponent.getInstance(project).setValue(key, file.getPath());
    }

    
    private static Type getType(@Nullable VirtualFile file) {
        if (file == null) {
            return Type.FILE;
        }
        if (file.isDirectory()) {
            return Type.DIRECTORY;
        }
        if (file.getFileType() instanceof ArchiveFileType) {
            return Type.ARCHIVE;
        }
        return Type.FILE;
    }

    private enum Type {
        FILE,
        DIRECTORY,
        ARCHIVE
    }
}
