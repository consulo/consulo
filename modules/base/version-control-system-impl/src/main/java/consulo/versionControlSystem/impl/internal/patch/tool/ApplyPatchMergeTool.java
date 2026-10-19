/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.patch.tool;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.diff.DiffContext;
import consulo.diff.impl.internal.merge.MergeImplUtil;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.merge.MergeContext;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.merge.MergeTool;
import consulo.ui.MessageBoxes;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@ExtensionImpl
public class ApplyPatchMergeTool implements MergeTool {
    @Override
    @RequiredUIAccess
    public MergeViewer createComponent(MergeContext context, MergeRequest request) {
        return new MyApplyPatchViewer(context, (ApplyPatchMergeRequest) request);
    }

    @Override
    public boolean canShow(MergeContext context, MergeRequest request) {
        return request instanceof ApplyPatchMergeRequest;
    }

    private static class MyApplyPatchViewer extends ApplyPatchViewer implements MergeViewer {
        private final MergeContext myMergeContext;

        private final ApplyPatchMergeRequest myMergeRequest;

        @RequiredUIAccess
        public MyApplyPatchViewer(MergeContext context, ApplyPatchMergeRequest request) {
            super(createWrapperDiffContext(context), request);
            myMergeContext = context;
            myMergeRequest = request;
        }

        private static DiffContext createWrapperDiffContext(MergeContext mergeContext) {
            return new MergeImplUtil.ProxyDiffContext(mergeContext);
        }

        @Override
        @RequiredUIAccess
        public ToolbarComponents init() {
            initPatchViewer();

            ToolbarComponents components = new ToolbarComponents();
            components.statusPanel = getStatusPanel().getComponent();
            components.toolbarActions = createToolbarActions();

            components.closeHandler = () -> MergeImplUtil.showExitWithoutApplyingChangesDialog(this, myMergeRequest, myMergeContext);
            return components;
        }

        @Override
        public @Nullable ActionRecord getResolveAction(MergeResult result) {
            if (result == MergeResult.LEFT || result == MergeResult.RIGHT) {
                return null;
            }

            return new ActionRecord(
                MergeImplUtil.getResolveActionTitle(result, myMergeRequest, myMergeContext),
                () -> {
                    UIAccess uiAccess = UIAccess.current();
                    confirmResolve(result).whenCompleteAsync((confirmed, error) -> {
                        if (!Boolean.TRUE.equals(confirmed) || isDisposed()) {
                            return;
                        }
                        myMergeContext.finishMerge(result);
                    }, uiAccess);
                }
            );
        }

        @RequiredUIAccess
        private CompletableFuture<Boolean> confirmResolve(MergeResult result) {
            if (result == MergeResult.RESOLVED) {
                int unresolved = getUnresolvedCount();
                if (unresolved != 0) {
                    return MessageBoxes.yesNo()
                        .title(DiffLocalize.applyPartiallyResolvedMergeDialogTitle())
                        .text(DiffLocalize.applyPatchPartiallyResolvedChangesConfirmationMessage(unresolved))
                        .showAsync();
                }
            }

            if (result == MergeResult.CANCEL) {
                return MergeImplUtil.showExitWithoutApplyingChangesDialog(this, myMergeRequest, myMergeContext);
            }

            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        private int getUnresolvedCount() {
            int count = 0;
            for (ApplyPatchChange change : getPatchChanges()) {
                if (change.isResolved()) {
                    continue;
                }
                count++;
            }
            return count;
        }

        @Override
        protected void onChangeResolved() {
            super.onChangeResolved();

            if (!ContainerUtil.exists(getModelChanges(), c -> !c.isResolved())) {
                Application.get().invokeLater(() -> {
                    if (isDisposed()) {
                        return;
                    }

                    Toast toast = Toast.create();
                    toast.addStyle(ToastStyle.SUCCESS);
                    toast.setHtmlText(DiffLocalize.applyPatchAllChangesProcessedMessageText());
                    toast.addHyperlinkListener(event -> {
                        if (!isDisposed()) {
                            myMergeContext.finishMerge(MergeResult.RESOLVED);
                        }
                    });
                    addNotification(toast);
                });
            }
        }
    }
}
