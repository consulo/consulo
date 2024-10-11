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

import consulo.diff.DiffContext;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.merge.MergeContext;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.merge.MergeTool.MergeViewer;
import consulo.diff.util.ThreeSide;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class MergeImplUtil {
    @Nonnull
    public static Action createSimpleResolveAction(
        @Nonnull MergeResult result,
        @Nonnull MergeRequest request,
        @Nonnull MergeContext context,
        @Nonnull MergeViewer viewer
    ) {
        String caption = getResolveActionTitle(result, request, context);
        return new AbstractAction(caption) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (result == MergeResult.CANCEL && !showExitWithoutApplyingChangesDialog(viewer, request, context)) {
                    return;
                }
                context.finishMerge(result);
            }
        };
    }

    @Nonnull
    public static String getResolveActionTitle(@Nonnull MergeResult result, @Nonnull MergeRequest request, @Nonnull MergeContext context) {
        Function<MergeResult, String> getter = DiffImplUtil.getUserData(request, context, DiffUserDataKeysEx.MERGE_ACTION_CAPTIONS);
        String message = getter != null ? getter.apply(result) : null;
        if (message != null) {
            return message;
        }

        switch (result) {
            case CANCEL:
                return "Abort";
            case LEFT:
                return "Accept Left";
            case RIGHT:
                return "Accept Right";
            case RESOLVED:
                return "Apply";
            default:
                throw new IllegalArgumentException(result.toString());
        }
    }

    @Nonnull
    public static List<String> notNullizeContentTitles(@Nonnull List<String> mergeContentTitles) {
        String left = StringUtil.notNullize(ThreeSide.LEFT.select(mergeContentTitles), "Your Version");
        String base = StringUtil.notNullize(ThreeSide.BASE.select(mergeContentTitles), "Base Version");
        String right = StringUtil.notNullize(ThreeSide.RIGHT.select(mergeContentTitles), "Server Version");
        return ContainerUtil.list(left, base, right);
    }

    public static class ProxyDiffContext extends DiffContext {
        @Nonnull
        private final MergeContext myMergeContext;

        public ProxyDiffContext(@Nonnull MergeContext mergeContext) {
            myMergeContext = mergeContext;
        }

        @Nullable
        @Override
        public Project getProject() {
            return myMergeContext.getProject();
        }

        @Override
        public boolean isWindowFocused() {
            return true;
        }

        @Override
        public boolean isFocused() {
            return myMergeContext.isFocused();
        }

        @Override
        public void requestFocus() {
            myMergeContext.requestFocus();
        }

        @Nullable
        @Override
        public <T> T getUserData(@Nonnull Key<T> key) {
            return myMergeContext.getUserData(key);
        }

        @Override
        public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
            myMergeContext.putUserData(key, value);
        }
    }

    public static boolean showExitWithoutApplyingChangesDialog(
        @Nonnull MergeViewer viewer,
        @Nonnull MergeRequest request,
        @Nonnull MergeContext context
    ) {
        Predicate<MergeViewer> customHandler = DiffImplUtil.getUserData(request, context, DiffUserDataKeysEx.MERGE_CANCEL_HANDLER);
        if (customHandler != null) {
            return customHandler.test(viewer);
        }

        return showExitWithoutApplyingChangesDialog(viewer.getComponent(), request, context);
    }

    public static boolean showExitWithoutApplyingChangesDialog(
        @Nonnull JComponent component,
        @Nonnull MergeRequest request,
        @Nonnull MergeContext context
    ) {
        String message = DiffLocalize.mergeDialogExitWithoutApplyingChangesConfirmationMessage().get();
        String title = DiffLocalize.cancelVisualMergeDialogTitle().get();
        Couple<String> customMessage = DiffImplUtil.getUserData(request, context, DiffUserDataKeysEx.MERGE_CANCEL_MESSAGE);
        if (customMessage != null) {
            title = customMessage.first;
            message = customMessage.second;
        }

        return Messages.showYesNoDialog(
            component.getRootPane(),
            message,
            title,
            Messages.getQuestionIcon()
        ) == Messages.YES;
    }
}
