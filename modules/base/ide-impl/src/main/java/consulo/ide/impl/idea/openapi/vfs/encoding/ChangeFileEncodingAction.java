/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.undoRedo.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author cdr
 */
@ActionImpl(id = "ChangeFileEncodingAction")
public class ChangeFileEncodingAction extends AnAction implements DumbAware {
    @Nonnull
    private final Application myApplication;
    private final boolean myAllowDirectories;

    @Inject
    public ChangeFileEncodingAction(@Nonnull Application application) {
        this(application, false);
    }

    public ChangeFileEncodingAction(@Nonnull Application application, boolean allowDirectories) {
        super(ActionLocalize.actionChangefileencodingactionText(), ActionLocalize.actionChangefileencodingactionDescription());
        myApplication = application;
        myAllowDirectories = allowDirectories;
    }

    private boolean checkEnabled(@Nonnull VirtualFile virtualFile) {
        if (myAllowDirectories && virtualFile.isDirectory()) {
            return true;
        }
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        Document document = documentManager.getDocument(virtualFile);
        return document != null
            && (EncodingUtil.checkCanConvert(virtualFile) == null || EncodingUtil.checkCanReload(virtualFile, null) == null);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        VirtualFile myFile = e.getData(VirtualFile.KEY);
        boolean enabled = myFile != null && checkEnabled(myFile);
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(myFile != null);
    }

    @Override
    @RequiredUIAccess
    public final void actionPerformed(@Nonnull AnActionEvent e) {
        DataContext dataContext = e.getDataContext();

        ListPopup popup = createPopup(dataContext);
        if (popup != null) {
            popup.showInBestPositionFor(dataContext);
        }
    }

    @Nullable
    public ListPopup createPopup(@Nonnull DataContext dataContext) {
        VirtualFile virtualFile = dataContext.getData(VirtualFile.KEY);
        if (virtualFile == null) {
            return null;
        }
        boolean enabled = checkEnabled(virtualFile);
        if (!enabled) {
            return null;
        }
        Editor editor = dataContext.getData(Editor.KEY);
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        Document document = documentManager.getDocument(virtualFile);
        if (!myAllowDirectories && virtualFile.isDirectory() || document == null && !virtualFile.isDirectory()) {
            return null;
        }

        byte[] bytes;
        try {
            bytes = virtualFile.isDirectory() ? null : VfsUtilCore.loadBytes(virtualFile);
        }
        catch (IOException e) {
            return null;
        }
        DefaultActionGroup group = createActionGroup(virtualFile, editor, document, bytes, null);

        return JBPopupFactory.getInstance().createActionGroupPopup(
            getTemplatePresentation().getText(),
            group,
            dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        );
    }

    public DefaultActionGroup createActionGroup(
        @Nullable final VirtualFile myFile,
        final Editor editor,
        final Document document,
        final byte[] bytes,
        @Nullable final String clearItemText
    ) {
        return new ChooseFileEncodingAction(myFile) {
            @Override
            public void update(@Nonnull AnActionEvent e) {
            }

            @Nonnull
            @Override
            protected DefaultActionGroup createPopupActionGroup(JComponent button) {
                return createCharsetsActionGroup(
                    clearItemText,
                    null,
                    charset -> "Change encoding to '" + charset.displayName() + "'"
                );
                // no 'clear'
            }

            @Override
            @RequiredUIAccess
            protected void chosen(@Nullable VirtualFile virtualFile, @Nonnull Charset charset) {
                ChangeFileEncodingAction.this.chosen(document, editor, virtualFile, bytes, charset);
            }
        }.createPopupActionGroup(null);
    }

    // returns true if charset was changed, false if failed
    @RequiredUIAccess
    protected boolean chosen(
        Document document,
        Editor editor,
        @Nullable VirtualFile virtualFile,
        byte[] bytes,
        @Nonnull Charset charset
    ) {
        if (virtualFile == null) {
            return false;
        }
        String text = document.getText();
        EncodingUtil.Magic8 isSafeToConvert = EncodingUtil.isSafeToConvertTo(virtualFile, text, bytes, charset);
        EncodingUtil.Magic8 isSafeToReload = EncodingUtil.isSafeToReloadIn(virtualFile, text, bytes, charset);

        Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
        return changeTo(myApplication, project, document, editor, virtualFile, charset, isSafeToConvert, isSafeToReload);
    }

    @RequiredUIAccess
    public static boolean changeTo(
        @Nonnull Application application,
        @Nullable Project project,
        @Nonnull Document document,
        Editor editor,
        @Nonnull VirtualFile virtualFile,
        @Nonnull Charset charset,
        @Nonnull EncodingUtil.Magic8 isSafeToConvert,
        @Nonnull EncodingUtil.Magic8 isSafeToReload
    ) {
        Charset oldCharset = virtualFile.getCharset();
        @RequiredUIAccess
        final Runnable undo;
        @RequiredUIAccess
        final Runnable redo;

        if (isSafeToConvert == EncodingUtil.Magic8.ABSOLUTELY && isSafeToReload == EncodingUtil.Magic8.ABSOLUTELY) {
            //change and forget
            undo = () -> application.getInstance(ApplicationEncodingManager.class).setEncoding(virtualFile, oldCharset);
            redo = () -> application.getInstance(ApplicationEncodingManager.class).setEncoding(virtualFile, charset);
        }
        else {
            IncompatibleEncodingDialog dialog = new IncompatibleEncodingDialog(virtualFile, charset, isSafeToReload, isSafeToConvert);
            dialog.show();
            if (dialog.getExitCode() == IncompatibleEncodingDialog.RELOAD_EXIT_CODE) {
                undo = () -> EncodingUtil.reloadIn(virtualFile, oldCharset, project);
                redo = () -> EncodingUtil.reloadIn(virtualFile, charset, project);
            }
            else if (dialog.getExitCode() == IncompatibleEncodingDialog.CONVERT_EXIT_CODE) {
                undo = () -> EncodingUtil.saveIn(document, editor, virtualFile, oldCharset);
                redo = () -> EncodingUtil.saveIn(document, editor, virtualFile, charset);
            }
            else {
                return false;
            }
        }

        UndoableAction action = new GlobalUndoableAction(virtualFile) {
            @Override
            public void undo() {
                // invoke later because changing document inside undo/redo is not allowed
                application.invokeLater(undo, IdeaModalityState.nonModal(), (project == null ? application : project).getDisposed());
            }

            @Override
            public void redo() {
                // invoke later because changing document inside undo/redo is not allowed
                application.invokeLater(redo, IdeaModalityState.nonModal(), (project == null ? application : project).getDisposed());
            }
        };

        redo.run();
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(IdeLocalize.changeEncodingCommandName(virtualFile.getName()))
            .undoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
            .run(() -> {
                UndoManager undoManager = project == null ? ApplicationUndoManager.getInstance() : ProjectUndoManager.getInstance(project);
                undoManager.undoableActionPerformed(action);
            });

        return true;
    }
}
