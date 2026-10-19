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
package consulo.diff.impl.internal.viewer.binary;

import consulo.application.AccessRule;
import consulo.application.WriteAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ProcessCanceledException;
import consulo.diff.DiffContext;
import consulo.diff.content.DiffContent;
import consulo.diff.content.FileContent;
import consulo.diff.impl.internal.action.FocusOppositePaneAction;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.impl.internal.viewer.BinaryEditorHolder;
import consulo.diff.impl.internal.viewer.DiffNotifications;
import consulo.diff.impl.internal.viewer.StatusPanel;
import consulo.diff.impl.internal.viewer.TransferableFileEditorStateSupport;
import consulo.diff.impl.internal.viewer.side.TwosideDiffViewer;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.util.Side;
import consulo.fileEditor.FileEditor;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static consulo.diff.internal.DiffImplUtil.getDiffSettings;

public class TwosideBinaryDiffViewer extends TwosideDiffViewer<BinaryEditorHolder> {
    public static final Logger LOG = Logger.getInstance(TwosideBinaryDiffViewer.class);

    
    private final TransferableFileEditorStateSupport myTransferableStateSupport;
    
    private final StatusPanel myStatusPanel;

    @RequiredUIAccess
    public TwosideBinaryDiffViewer(DiffContext context, DiffRequest request) {
        super(context, (ContentDiffRequest)request, BinaryEditorHolder.BinaryEditorHolderFactory.INSTANCE);

        myStatusPanel = new StatusPanel();
        new MyFocusOppositePaneAction().install(myPanel.getComponent());

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
    
    protected Runnable performRediff(ProgressIndicator indicator) {
        try {
            indicator.checkCanceled();

            List<DiffContent> contents = myRequest.getContents();
            if (!(contents.get(0) instanceof FileContent) || !(contents.get(1) instanceof FileContent)) {
                return applyNotification(null);
            }

            VirtualFile file1 = ((FileContent)contents.get(0)).getFile();
            VirtualFile file2 = ((FileContent)contents.get(1)).getFile();

            ThrowableComputable<@Nullable Supplier<Component>, RuntimeException> action = () -> {
                if (!file1.isValid() || !file2.isValid()) {
                    return DiffNotifications::createError;
                }

                try {
                    // we can't use getInputStream() here because we can't restore BOM marker
                    // (getBom() can return null for binary files, while getInputStream() strips BOM for all files).
                    // It can be made for files from VFS that implements FileSystemInterface though.
                    byte[] bytes1 = file1.contentsToByteArray();
                    byte[] bytes2 = file2.contentsToByteArray();
                    return Arrays.equals(bytes1, bytes2) ? DiffNotifications::createEqualContents : null;
                }
                catch (IOException e) {
                    LOG.warn(e);
                    return null;
                }
            };
            Supplier<Component> notification = AccessRule.read(action);

            return applyNotification(notification);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error(e);
            return applyNotification(DiffNotifications::createError);
        }
    }

    
    private Runnable applyNotification(@Nullable Supplier<Component> notification) {
        return () -> {
            clearDiffPresentation();
            if (notification != null) {
                myPanel.addNotification(notification.get());
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

    
    FileEditor getCurrentEditor() {
        return getCurrentEditorHolder().getEditor();
    }

    
    @Override
    protected Component getStatusPanel() {
        return myStatusPanel.getComponent();
    }

    //
    // Misc
    //

    public static boolean canShowRequest(DiffContext context, DiffRequest request) {
        return TwosideDiffViewer.canShowRequest(context, request, BinaryEditorHolder.BinaryEditorHolderFactory.INSTANCE);
    }

    //
    // Actions
    //

    private class MyAcceptSideAction extends LegacyDumbAwareAction {
        
        private final Side myBaseSide;

        public MyAcceptSideAction(Side baseSide) {
            myBaseSide = baseSide;
            getTemplatePresentation().setText("Copy Content to " + baseSide.select("Right", "Left"));
            getTemplatePresentation().setIcon(baseSide.select(PlatformIconGroup.vcsArrow_right(), PlatformIconGroup.vcsArrow_left()));
            setShortcutSet(ActionManager.getInstance()
                .getAction(baseSide.select("Diff.ApplyLeftSide", "Diff.ApplyRightSide"))
                .getShortcutSet());
        }

        @Override
        public void update(AnActionEvent e) {
            VirtualFile baseFile = getContentFile(myBaseSide);
            VirtualFile targetFile = getContentFile(myBaseSide.other());

            boolean enabled = baseFile != null && targetFile != null && targetFile.isWritable();
            e.getPresentation().setEnabledAndVisible(enabled);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            VirtualFile baseFile = getContentFile(myBaseSide);
            VirtualFile targetFile = getContentFile(myBaseSide.other());
            assert baseFile != null && targetFile != null;

            try {
                WriteAction.run(() -> targetFile.setBinaryContent(baseFile.contentsToByteArray()));
            }
            catch (IOException err) {
                LOG.warn(err);
                MessageBoxes.okError(LocalizeValue.ofNullable(err.getMessage()))
                    .title(LocalizeValue.localizeTODO("Can't Copy File"))
                    .showAsync();
            }
        }

        private @Nullable VirtualFile getContentFile(Side side) {
            DiffContent content = side.select(myRequest.getContents());
            VirtualFile file = content instanceof FileContent ? ((FileContent)content).getFile() : null;
            return file != null && file.isValid() ? file : null;
        }
    }

    private class MyFocusOppositePaneAction extends FocusOppositePaneAction {
        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            setCurrentSide(getCurrentSide().other());
            UnifiedDiffUtil.requestFocus(getPreferredFocusedComponent());
        }
    }
}
