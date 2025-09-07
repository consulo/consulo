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
package consulo.versionControlSystem.impl.internal.ui.awt;

import consulo.dataContext.DataSink;
import consulo.project.Project;
import consulo.project.ui.impl.internal.VirtualFileDeleteProvider;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.impl.internal.change.action.DeleteUnversionedFilesAction;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangeNodeDecorator;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesBrowserNode;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesTreeListImpl;
import consulo.versionControlSystem.impl.internal.change.ui.awt.TreeModelBuilder;
import consulo.versionControlSystem.ui.awt.LegacyDialog;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class SelectFilesDialog extends AbstractSelectFilesDialog<VirtualFile> implements LegacyDialog {
    @Nonnull
    private final VirtualFileListImpl myFileList;
    private final boolean myDeletableFiles;

    protected SelectFilesDialog(
        Project project, List<VirtualFile> originalFiles, String prompt,
        VcsShowConfirmationOption confirmationOption,
        boolean selectableFiles, boolean showDoNotAskOption, boolean deletableFiles
    ) {
        super(project, false, confirmationOption, prompt, showDoNotAskOption);
        myDeletableFiles = deletableFiles;
        myFileList = new VirtualFileListImpl(project, originalFiles, selectableFiles, deletableFiles);
        myFileList.setChangesToDisplay(originalFiles);
    }

    @Nonnull
    public static SelectFilesDialog init(
        Project project, List<VirtualFile> originalFiles, String prompt,
        VcsShowConfirmationOption confirmationOption,
        boolean selectableFiles, boolean showDoNotAskOption, boolean deletableFiles
    ) {
        SelectFilesDialog dialog = new SelectFilesDialog(project, originalFiles, prompt, confirmationOption, selectableFiles,
            showDoNotAskOption, deletableFiles
        );
        dialog.init();
        return dialog;
    }

    public Collection<VirtualFile> getSelectedFiles() {
        return myFileList.getIncludedChanges();
    }

    public void setSelectedFiles(@Nonnull Collection<VirtualFile> selected) {
        myFileList.setIncludedChanges(selected);
    }

    @Nonnull
    @Override
    protected ChangesTreeListImpl getFileList() {
        return myFileList;
    }

    @Nonnull
    @Override
    protected DefaultActionGroup createToolbarActions() {
        DefaultActionGroup defaultGroup = super.createToolbarActions();
        if (myDeletableFiles) {
            AnAction deleteAction = new DeleteUnversionedFilesAction() {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    super.actionPerformed(e);
                    myFileList.refresh();
                }
            };
            defaultGroup.add(deleteAction);
            deleteAction.registerCustomShortcutSet(CommonShortcuts.getDelete(), this.getFileList());
        }
        return defaultGroup;
    }

    public static class VirtualFileListImpl extends ChangesTreeListImpl<VirtualFile> {

        @Nullable
        private final DeleteProvider myDeleteProvider;

        public VirtualFileListImpl(Project project, List<VirtualFile> originalFiles, boolean selectableFiles, boolean deletableFiles) {
            super(project, originalFiles, selectableFiles, true, null, null);
            myDeleteProvider = (deletableFiles ? new VirtualFileDeleteProvider() : null);
        }

        @Override
        protected DefaultTreeModel buildTreeModel(List<VirtualFile> changes, ChangeNodeDecorator changeNodeDecorator) {
            return TreeModelBuilder.buildFromVirtualFiles(myProject, isShowFlatten(), changes);
        }

        @Override
        protected List<VirtualFile> getSelectedObjects(ChangesBrowserNode node) {
            return node.getAllFilesUnder();
        }

        @Override
        protected VirtualFile getLeadSelectedObject(ChangesBrowserNode node) {
            Object o = node.getUserObject();
            if (o instanceof VirtualFile) {
                return (VirtualFile) o;
            }
            return null;
        }

        @Override
        public void calcData(Key<?> key, DataSink sink) {
            super.calcData(key, sink);
            if (DeleteProvider.KEY == key && myDeleteProvider != null) {
                sink.put(DeleteProvider.KEY, myDeleteProvider);
            }
            else if (VirtualFile.KEY_OF_ARRAY == key) {
                sink.put(VirtualFile.KEY_OF_ARRAY, ArrayUtil.toObjectArray(getSelectedChanges(), VirtualFile.class));
            }
        }

        public void refresh() {
            setChangesToDisplay(new ArrayList<>(ContainerUtil.filter(getIncludedChanges(), input -> input != null && input.isValid())));
        }
    }
}
