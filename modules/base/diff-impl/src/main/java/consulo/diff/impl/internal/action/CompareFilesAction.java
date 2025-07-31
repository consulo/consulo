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
import consulo.diff.DiffRequestFactory;
import consulo.diff.request.DiffRequest;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Set;

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
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);

        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);

        String text = "Compare Files";
        if (files != null && files.length == 1) {
            text = "Compare With...";
        }
        else if (files != null && files.length == 2) {
            Type type1 = getType(files[0]);
            Type type2 = getType(files[1]);

            if (type1 != type2) {
                text = "Compare";
            }
            else {
                switch (type1) {
                    case FILE:
                        text = "Compare Files";
                        break;
                    case DIRECTORY:
                        text = "Compare Directories";
                        break;
                    case ARCHIVE:
                        text = "Compare Archives";
                        break;
                }
            }
        }
        e.getPresentation().setText(text);
    }

    @Override
    protected boolean isAvailable(@Nonnull AnActionEvent e) {
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

    @Nullable
    @Override
    protected DiffRequest getDiffRequest(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        DiffRequest diffRequest = e.getData(DIFF_REQUEST);
        if (diffRequest != null) {
            return diffRequest;
        }

        VirtualFile[] data = e.getRequiredData(VirtualFile.KEY_OF_ARRAY);
        if (data.length == 1) {
            VirtualFile otherFile = getOtherFile(project, data[0]);
            if (otherFile == null) {
                return null;
            }
            if (!hasContent(data[0])) {
                return null;
            }
            return DiffRequestFactory.getInstance().createFromFiles(project, data[0], otherFile);
        }
        else {
            return DiffRequestFactory.getInstance().createFromFiles(project, data[0], data[1]);
        }
    }

    @Nullable
    private static VirtualFile getOtherFile(@Nullable Project project, @Nonnull VirtualFile file) {
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
        VirtualFile otherFile = IdeaFileChooser.chooseFile(descriptor, project, selectedFile);
        if (otherFile != null) {
            updateDefaultSelection(project, key, otherFile);
        }
        return otherFile;
    }

    @Nonnull
    private static VirtualFile getDefaultSelection(@Nullable Project project, @Nonnull String key, @Nonnull VirtualFile file) {
        if (project == null) {
            return file;
        }
        final String path = ProjectPropertiesComponent.getInstance(project).getValue(key);
        if (path == null) {
            return file;
        }
        VirtualFile lastSelection = LocalFileSystem.getInstance().findFileByPath(path);
        return lastSelection != null ? lastSelection : file;
    }

    private static void updateDefaultSelection(@Nullable Project project, @Nonnull String key, @Nonnull VirtualFile file) {
        if (project == null) {
            return;
        }
        ProjectPropertiesComponent.getInstance(project).setValue(key, file.getPath());
    }

    @Nonnull
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
