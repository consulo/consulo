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
package consulo.desktop.awt.internal.diff.binary;

import consulo.application.AccessRule;
import consulo.application.AllIcons;
import consulo.application.WriteAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ProcessCanceledException;
import consulo.desktop.awt.internal.diff.BinaryEditorHolder;
import consulo.desktop.awt.internal.diff.util.AWTDiffUtil;
import consulo.desktop.awt.internal.diff.util.DiffNotifications;
import consulo.desktop.awt.internal.diff.util.StatusPanel;
import consulo.desktop.awt.internal.diff.util.TransferableFileEditorStateSupport;
import consulo.desktop.awt.internal.diff.util.side.TwosideDiffViewer;
import consulo.diff.DiffContext;
import consulo.diff.content.DiffContent;
import consulo.diff.content.FileContent;
import consulo.diff.impl.internal.action.FocusOppositePaneAction;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.util.Side;
import consulo.fileEditor.FileEditor;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static consulo.diff.impl.internal.util.DiffImplUtil.getDiffSettings;

public class TwosideBinaryDiffViewer extends TwosideDiffViewer<BinaryEditorHolder> {
    public static final Logger LOG = Logger.getInstance(TwosideBinaryDiffViewer.class);

    @Nonnull
    private final TransferableFileEditorStateSupport myTransferableStateSupport;
    @Nonnull
    private final StatusPanel myStatusPanel;

    public TwosideBinaryDiffViewer(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
        super(context, (ContentDiffRequest)request, BinaryEditorHolder.BinaryEditorHolderFactory.INSTANCE);

        myStatusPanel = new StatusPanel();
        new MyFocusOppositePaneAction().install(myPanel);

        myContentPanel.setTopAction(new MyAcceptSideAction(Side.LEFT));
        myContentPanel.setBottomAction(new MyAcceptSideAction(Side.RIGHT));

        myTransferableStateSupport = new TransferableFileEditorStateSupport(getDiffSettings(context), getEditorHolders(), this);
    }

    @Override
    @RequiredUIAccess
    protected void processContextHints() {
        super.processContextHints();
        myTransferableStateSupport.processContextHints(myRequest, myContext);
    }

    @Override
    @RequiredUIAccess
    protected void updateContextHints() {
        super.updateContextHints();
        myTransferableStateSupport.updateContextHints(myRequest, myContext);
    }

    @Override
    protected List<AnAction> createToolbarActions() {
        List<AnAction> group = new ArrayList<>();

        group.add(new MyAcceptSideAction(Side.LEFT));
        group.add(new MyAcceptSideAction(Side.RIGHT));

        group.add(AnSeparator.getInstance());
        group.add(myTransferableStateSupport.createToggleAction());
        group.addAll(super.createToolbarActions());

        return group;
    }

    //
    // Diff
    //

    @Override
    @RequiredUIAccess
    protected void onSlowRediff() {
        super.onSlowRediff();
        myStatusPanel.setBusy(true);
    }

    @Override
    @Nonnull
    protected Runnable performRediff(@Nonnull final ProgressIndicator indicator) {
        try {
            indicator.checkCanceled();

            List<DiffContent> contents = myRequest.getContents();
            if (!(contents.get(0) instanceof FileContent) || !(contents.get(1) instanceof FileContent)) {
                return applyNotification(null);
            }

            final VirtualFile file1 = ((FileContent)contents.get(0)).getFile();
            final VirtualFile file2 = ((FileContent)contents.get(1)).getFile();

            ThrowableComputable<JPanel, RuntimeException> action = () -> {
                if (!file1.isValid() || !file2.isValid()) {
                    return DiffNotifications.createError();
                }

                try {
                    // we can't use getInputStream() here because we can't restore BOM marker
                    // (getBom() can return null for binary files, while getInputStream() strips BOM for all files).
                    // It can be made for files from VFS that implements FileSystemInterface though.
                    byte[] bytes1 = file1.contentsToByteArray();
                    byte[] bytes2 = file2.contentsToByteArray();
                    return Arrays.equals(bytes1, bytes2) ? DiffNotifications.createEqualContents() : null;
                }
                catch (IOException e) {
                    LOG.warn(e);
                    return null;
                }
            };
            final JComponent notification = AccessRule.read(action);

            return applyNotification(notification);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error(e);
            return applyNotification(DiffNotifications.createError());
        }
    }

    @Nonnull
    private Runnable applyNotification(@Nullable final JComponent notification) {
        return () -> {
            clearDiffPresentation();
            if (notification != null) {
                myPanel.addNotification(notification);
            }
        };
    }

    private void clearDiffPresentation() {
        myStatusPanel.setBusy(false);
        myPanel.resetNotifications();
    }

    //
    // Getters
    //

    @Nonnull
    FileEditor getCurrentEditor() {
        return getCurrentEditorHolder().getEditor();
    }

    @Nonnull
    @Override
    protected JComponent getStatusPanel() {
        return myStatusPanel;
    }

    //
    // Misc
    //

    public static boolean canShowRequest(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
        return TwosideDiffViewer.canShowRequest(context, request, BinaryEditorHolder.BinaryEditorHolderFactory.INSTANCE);
    }

    //
    // Actions
    //

    private class MyAcceptSideAction extends DumbAwareAction {
        @Nonnull
        private final Side myBaseSide;

        public MyAcceptSideAction(@Nonnull Side baseSide) {
            myBaseSide = baseSide;
            getTemplatePresentation().setText("Copy Content to " + baseSide.select("Right", "Left"));
            getTemplatePresentation().setIcon(baseSide.select(AllIcons.Vcs.Arrow_right, AllIcons.Vcs.Arrow_left));
            setShortcutSet(ActionManager.getInstance()
                .getAction(baseSide.select("Diff.ApplyLeftSide", "Diff.ApplyRightSide"))
                .getShortcutSet());
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            VirtualFile baseFile = getContentFile(myBaseSide);
            VirtualFile targetFile = getContentFile(myBaseSide.other());

            boolean enabled = baseFile != null && targetFile != null && targetFile.isWritable();
            e.getPresentation().setEnabledAndVisible(enabled);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            final VirtualFile baseFile = getContentFile(myBaseSide);
            final VirtualFile targetFile = getContentFile(myBaseSide.other());
            assert baseFile != null && targetFile != null;

            try {
                WriteAction.run(() -> targetFile.setBinaryContent(baseFile.contentsToByteArray()));
            }
            catch (IOException err) {
                LOG.warn(err);
                Messages.showErrorDialog(getProject(), err.getMessage(), "Can't Copy File");
            }
        }

        @Nullable
        private VirtualFile getContentFile(@Nonnull Side side) {
            DiffContent content = side.select(myRequest.getContents());
            VirtualFile file = content instanceof FileContent ? ((FileContent)content).getFile() : null;
            return file != null && file.isValid() ? file : null;
        }
    }

    private class MyFocusOppositePaneAction extends FocusOppositePaneAction {
        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            setCurrentSide(getCurrentSide().other());
            AWTDiffUtil.requestFocus(getProject(), getPreferredFocusedComponent());
        }
    }
}
