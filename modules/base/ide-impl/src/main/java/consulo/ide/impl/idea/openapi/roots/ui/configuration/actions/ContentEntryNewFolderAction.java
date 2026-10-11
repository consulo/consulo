/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.actions;

import consulo.ide.impl.idea.openapi.roots.ui.configuration.ContentEntryTreeEditor;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.internal.AnActionWithUIUpdate;
import consulo.ui.ex.localize.UILocalize;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class ContentEntryNewFolderAction extends DumbAwareAction implements AnActionWithUIUpdate {
    private static final Logger LOG = Logger.getInstance(ContentEntryNewFolderAction.class);

    private final ContentEntryTreeEditor myTreeEditor;

    public ContentEntryNewFolderAction(ContentEntryTreeEditor treeEditor) {
        super(
            ActionLocalize.actionFilechooserNewfolderText(),
            ActionLocalize.actionFilechooserNewfolderDescription(),
            PlatformIconGroup.actionsNewfolder()
        );
        myTreeEditor = treeEditor;
    }

    @Override
    @RequiredUIAccess
    public void updateAtUI(AnActionEvent e) {
        e.getPresentation().setEnabled(myTreeEditor.getSelectedFiles().size() == 1);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        List<VirtualFile> selectedFiles = myTreeEditor.getSelectedFiles();
        if (selectedFiles.size() != 1) {
            return;
        }

        VirtualFile parent = selectedFiles.get(0);

        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(UILocalize.newFolderDialogTitle())
            .text(UILocalize.createNewFolderEnterNewFolderNamePromptText())
            .asQuestion()
            .validator(name -> StringUtil.isEmptyOrSpaces(name)
                ? InputProblem.error(UILocalize.createNewFolderFolderNameCannotBeEmptyErrorMessage())
                : null)
            .showAsync(myTreeEditor.getTree())
            .whenCompleteAsync((name, error) -> {
                if (name != null && !StringUtil.isEmptyOrSpaces(name)) {
                    createFolder(parent, name.trim());
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    private void createFolder(VirtualFile parent, String name) {
        VirtualFile folder = CommandProcessor.getInstance().<@Nullable VirtualFile>newCommand()
            .project(myTreeEditor.getProject())
            .name(UILocalize.fileChooserCreateNewFolderCommandName())
            .inWriteAction()
            .compute(() -> createDirectories(parent, name));

        if (folder == null) {
            MessageBoxes.okError(UILocalize.createNewFolderCouldNotCreateFolderErrorMessage(name))
                .title(UILocalize.errorDialogTitle())
                .showAsync(myTreeEditor.getTree());
            return;
        }

        myTreeEditor.refreshAndSelect(folder.getUrl());
    }

    private @Nullable VirtualFile createDirectories(VirtualFile parent, String name) {
        try {
            VirtualFile current = parent;
            for (String part : StringUtil.tokenize(name, "\\/")) {
                VirtualFile child = current.findChild(part);
                current = child != null && child.isDirectory() ? child : current.createChildDirectory(this, part);
            }
            return current;
        }
        catch (IOException e) {
            LOG.warn(e);
            return null;
        }
    }
}
