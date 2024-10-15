// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.application.Application;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffManager;
import consulo.diff.DiffRequestPanel;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.content.DocumentContent;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.localize.UILocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author peter
 */
class MemoryDiskConflictResolver {
    private static final Logger LOG = Logger.getInstance(MemoryDiskConflictResolver.class);

    private final Set<VirtualFile> myConflicts = new LinkedHashSet<>();
    private Throwable myConflictAppeared;

    void beforeContentChange(@Nonnull VFileContentChangeEvent event) {
        if (event.isFromSave()) {
            return;
        }

        VirtualFile file = event.getFile();
        if (!file.isValid() || hasConflict(file)) {
            return;
        }

        Document document = FileDocumentManager.getInstance().getCachedDocument(file);
        if (document == null || !FileDocumentManager.getInstance().isDocumentUnsaved(document)) {
            return;
        }

        long documentStamp = document.getModificationStamp();
        long oldFileStamp = event.getOldModificationStamp();
        if (documentStamp != oldFileStamp) {
            LOG.info("reload " + file.getName() + " from disk?");
            LOG.info("  documentStamp:" + documentStamp);
            LOG.info("  oldFileStamp:" + oldFileStamp);
            if (myConflicts.isEmpty()) {
                Application application = Application.get();
                if (application.isUnitTestMode()) {
                    myConflictAppeared = new Throwable();
                }
                application.invokeLater(this::processConflicts);
            }
            myConflicts.add(file);
        }
    }

    boolean hasConflict(VirtualFile file) {
        return myConflicts.contains(file);
    }

    @RequiredUIAccess
    private void processConflicts() {
        List<VirtualFile> conflicts = new ArrayList<>(myConflicts);
        myConflicts.clear();

        for (VirtualFile file : conflicts) {
            Document document = FileDocumentManager.getInstance().getCachedDocument(file);
            if (document != null && file.getModificationStamp() != document.getModificationStamp() && askReloadFromDisk(file, document)) {
                FileDocumentManager.getInstance().reloadFromDisk(document);
            }
        }
        myConflictAppeared = null;
    }

    @RequiredUIAccess
    boolean askReloadFromDisk(VirtualFile file, Document document) {
        if (myConflictAppeared != null) {
            Throwable trace = myConflictAppeared;
            myConflictAppeared = null;
            throw new IllegalStateException(
                "Unexpected memory-disk conflict in tests for " + file.getPath() +
                    ", please use FileDocumentManager#reloadFromDisk or avoid VFS refresh",
                trace
            );
        }

        LocalizeValue message = UILocalize.fileCacheConflictMessageText(file.getPresentableUrl());

        DialogBuilder builder = new DialogBuilder();
        builder.setCenterPanel(new JBLabel(message.get(), UIUtil.getQuestionIcon(), SwingConstants.CENTER));
        builder.addOkAction().setText(UILocalize.fileCacheConflictLoadFsChangesButton());
        builder.addCancelAction().setText(UILocalize.fileCacheConflictKeepMemoryChangesButton());
        builder.addAction(new LocalizeAction(UILocalize.fileCacheConflictShowDifferenceButton()) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(ActionEvent e) {
                Project project = ProjectLocator.getInstance().guessProjectForFile(file);
                String fsContent = LoadTextUtil.loadText(file).toString();
                DocumentContent content1 = DiffContentFactory.getInstance().create(project, fsContent, file.getFileType());
                DocumentContent content2 = DiffContentFactory.getInstance().create(project, document, file);
                LocalizeValue title = UILocalize.fileCacheConflictForFileDialogTitle(file.getPresentableUrl());
                LocalizeValue title1 = UILocalize.fileCacheConflictDiffContentFileSystemContent();
                LocalizeValue title2 = UILocalize.fileCacheConflictDiffContentMemoryContent();

                DiffRequest request = new SimpleDiffRequest(title.get(), content1, content2, title1.get(), title2.get());
                request.putUserData(DiffUserDataKeys.GO_TO_SOURCE_DISABLE, true);

                DialogBuilder diffBuilder = new DialogBuilder(project);

                DiffRequestPanel diffPanel = DiffManager.getInstance().createRequestPanel(project, diffBuilder, diffBuilder.getWindow());
                diffPanel.setRequest(request);

                diffBuilder.setCenterPanel(diffPanel.getComponent());
                diffBuilder.setDimensionServiceKey("FileDocumentManager.FileCacheConflict");
                diffBuilder.addOkAction().setText(UILocalize.fileCacheConflictSaveChangesButton());
                diffBuilder.addCancelAction();
                diffBuilder.setTitle(title);

                if (diffBuilder.show() == DialogWrapper.OK_EXIT_CODE) {
                    builder.getDialogWrapper().close(DialogWrapper.CANCEL_EXIT_CODE);
                }
            }
        });
        builder.setTitle(UILocalize.fileCacheConflictDialogTitle());
        builder.setButtonsAlignment(SwingConstants.CENTER);
        builder.setHelpId("reference.dialogs.fileCacheConflict");
        return builder.show() == 0;
    }
}