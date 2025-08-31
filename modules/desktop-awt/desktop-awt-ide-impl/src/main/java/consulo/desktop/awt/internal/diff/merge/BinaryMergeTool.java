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
package consulo.desktop.awt.internal.diff.merge;

import consulo.annotation.component.ExtensionImpl;
import consulo.desktop.awt.internal.diff.BinaryEditorHolder;
import consulo.desktop.awt.internal.diff.binary.ThreesideBinaryDiffViewer;
import consulo.diff.DiffContext;
import consulo.diff.FrameDiffTool;
import consulo.diff.content.DiffContent;
import consulo.diff.merge.*;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;

@ExtensionImpl(id = "binary", order = "after text")
public class BinaryMergeTool implements MergeTool {
    public static final BinaryMergeTool INSTANCE = new BinaryMergeTool();

    @Nonnull
    @Override
    @RequiredUIAccess
    public MergeViewer createComponent(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
        return new BinaryMergeViewer(context, (ThreesideMergeRequest)request);
    }

    @Override
    public boolean canShow(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
        if (!(request instanceof ThreesideMergeRequest)) {
            return false;
        }

        MergeImplUtil.ProxyDiffContext diffContext = new MergeImplUtil.ProxyDiffContext(context);
        for (DiffContent diffContent : ((ThreesideMergeRequest)request).getContents()) {
            if (!BinaryEditorHolder.BinaryEditorHolderFactory.INSTANCE.canShowContent(diffContent, diffContext)) {
                return false;
            }
        }

        return true;
    }

    public static class BinaryMergeViewer implements MergeViewer {
        @Nonnull
        private final MergeContext myMergeContext;
        @Nonnull
        private final ThreesideMergeRequest myMergeRequest;

        @Nonnull
        private final DiffContext myDiffContext;
        @Nonnull
        private final ContentDiffRequest myDiffRequest;

        @Nonnull
        private final MyThreesideViewer myViewer;

        public BinaryMergeViewer(@Nonnull MergeContext context, @Nonnull ThreesideMergeRequest request) {
            myMergeContext = context;
            myMergeRequest = request;

            myDiffContext = new MergeImplUtil.ProxyDiffContext(myMergeContext);
            myDiffRequest = new SimpleDiffRequest(
                myMergeRequest.getTitle(),
                getDiffContents(myMergeRequest),
                getDiffContentTitles(myMergeRequest)
            );

            myViewer = new MyThreesideViewer(myDiffContext, myDiffRequest);
        }

        @Nonnull
        private static List<DiffContent> getDiffContents(@Nonnull ThreesideMergeRequest mergeRequest) {
            return ContainerUtil.newArrayList(mergeRequest.getContents());
        }

        @Nonnull
        private static List<String> getDiffContentTitles(@Nonnull ThreesideMergeRequest mergeRequest) {
            return MergeImplUtil.notNullizeContentTitles(mergeRequest.getContentTitles());
        }

        //
        // Impl
        //

        @Nonnull
        @Override
        public JComponent getComponent() {
            return myViewer.getComponent();
        }

        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
            return myViewer.getPreferredFocusedComponent();
        }

        @Nonnull
        @Override
        @RequiredUIAccess
        public ToolbarComponents init() {
            ToolbarComponents components = new ToolbarComponents();

            FrameDiffTool.ToolbarComponents init = myViewer.init();
            components.statusPanel = init.statusPanel;
            components.toolbarActions = init.toolbarActions;

            components.closeHandler =
                () -> MergeImplUtil.showExitWithoutApplyingChangesDialog(BinaryMergeViewer.this, myMergeRequest, myMergeContext);

            return components;
        }

        @Nullable
        @Override
        public ActionRecord getResolveAction(@Nonnull MergeResult result) {
            if (result == MergeResult.RESOLVED) {
                return null;
            }

            return new ActionRecord(
                MergeImplUtil.getResolveActionTitle(result, myMergeRequest, myMergeContext),
                () -> {
                    if (result == MergeResult.CANCEL
                        && !MergeImplUtil.showExitWithoutApplyingChangesDialog(BinaryMergeViewer.this, myMergeRequest, myMergeContext)) {
                        return;
                    }
                    myMergeContext.finishMerge(result);
                }
            );
        }

        @Override
        @RequiredUIAccess
        public void dispose() {
            Disposer.dispose(myViewer);
        }

        //
        // Getters
        //

        @Nonnull
        public MyThreesideViewer getViewer() {
            return myViewer;
        }

        //
        // Viewer
        //

        private static class MyThreesideViewer extends ThreesideBinaryDiffViewer {
            public MyThreesideViewer(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
                super(context, request);
            }

            @Override
            @RequiredUIAccess
            public void rediff(boolean trySync) {
            }
        }
    }
}
