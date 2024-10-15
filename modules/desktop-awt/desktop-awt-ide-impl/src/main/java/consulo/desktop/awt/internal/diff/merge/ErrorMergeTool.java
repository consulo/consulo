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
import consulo.desktop.awt.internal.diff.util.AWTDiffUtil;
import consulo.diff.merge.MergeContext;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.merge.MergeTool;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

@ExtensionImpl
public class ErrorMergeTool implements MergeTool {
    public static final ErrorMergeTool INSTANCE = new ErrorMergeTool();

    @Nonnull
    @Override
    @RequiredUIAccess
    public MergeViewer createComponent(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
        return new MyViewer(context, request);
    }

    @Override
    public boolean canShow(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
        return true;
    }

    private static class MyViewer implements MergeViewer {
        @Nonnull
        private final MergeContext myMergeContext;
        @Nonnull
        private final MergeRequest myMergeRequest;

        @Nonnull
        private final JPanel myPanel;

        public MyViewer(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
            myMergeContext = context;
            myMergeRequest = request;

            myPanel = new JPanel(new BorderLayout());
            myPanel.add(createComponent(), BorderLayout.CENTER);
        }

        @Nonnull
        private JComponent createComponent() {
            return AWTDiffUtil.createMessagePanel("Can't show diff");
        }

        @Nonnull
        @Override
        public JComponent getComponent() {
            return myPanel;
        }

        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
            return null;
        }

        @Nonnull
        @Override
        @RequiredUIAccess
        public ToolbarComponents init() {
            return new ToolbarComponents();
        }

        @Nullable
        @Override
        public ActionRecord getResolveAction(@Nonnull final MergeResult result) {
            if (result == MergeResult.RESOLVED) {
                return null;
            }

            LocalizeValue caption = MergeImplUtil.getResolveActionTitle(result, myMergeRequest, myMergeContext);
            return new ActionRecord(caption, () -> myMergeContext.finishMerge(result));
        }

        @Override
        @RequiredUIAccess
        public void dispose() {
        }
    }
}
