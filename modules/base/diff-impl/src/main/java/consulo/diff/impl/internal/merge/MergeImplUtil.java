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
package consulo.diff.impl.internal.merge;

import consulo.diff.DiffContext;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.merge.MergeContext;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.merge.MergeTool.MergeViewer;
import consulo.diff.util.ThreeSide;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.MessageBoxes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class MergeImplUtil {

    public static LocalizeValue getResolveActionTitle(MergeResult result, MergeRequest request, MergeContext context) {
        Function<MergeResult, String> getter = DiffImplUtil.getUserData(request, context, DiffUserDataKeysEx.MERGE_ACTION_CAPTIONS);
        String message = getter != null ? getter.apply(result) : null;
        if (message != null) {
            return LocalizeValue.localizeTODO(message);
        }

        return switch (result) {
            case CANCEL -> LocalizeValue.localizeTODO("Abort");
            case LEFT -> LocalizeValue.localizeTODO("Accept Left");
            case RIGHT -> LocalizeValue.localizeTODO("Accept Right");
            case RESOLVED -> LocalizeValue.localizeTODO("Apply");
            default -> throw new IllegalArgumentException(result.toString());
        };
    }

    
    public static List<String> notNullizeContentTitles(List<String> mergeContentTitles) {
        String left = StringUtil.notNullize(ThreeSide.LEFT.select(mergeContentTitles), "Your Version");
        String base = StringUtil.notNullize(ThreeSide.BASE.select(mergeContentTitles), "Base Version");
        String right = StringUtil.notNullize(ThreeSide.RIGHT.select(mergeContentTitles), "Server Version");
        return Arrays.asList(left, base, right);
    }

    public static class ProxyDiffContext extends DiffContext {
        
        private final MergeContext myMergeContext;

        public ProxyDiffContext(MergeContext mergeContext) {
            myMergeContext = mergeContext;
        }

        @Override
        public @Nullable Project getProject() {
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

        @Override
        public <T> @Nullable T getUserData(Key<T> key) {
            return myMergeContext.getUserData(key);
        }

        @Override
        public <T> void putUserData(Key<T> key, @Nullable T value) {
            myMergeContext.putUserData(key, value);
        }
    }

    @RequiredUIAccess
    public static CompletableFuture<Boolean> showExitWithoutApplyingChangesDialog(
        MergeViewer viewer,
        MergeRequest request,
        MergeContext context
    ) {
        Function<MergeViewer, CompletableFuture<Boolean>> customHandler =
            DiffImplUtil.getUserData(request, context, DiffUserDataKeysEx.MERGE_CANCEL_HANDLER);
        if (customHandler != null) {
            return customHandler.apply(viewer);
        }

        LocalizeValue title = DiffLocalize.cancelVisualMergeDialogTitle();
        LocalizeValue message = DiffLocalize.mergeDialogExitWithoutApplyingChangesConfirmationMessage();
        Couple<String> customMessage = DiffImplUtil.getUserData(request, context, DiffUserDataKeysEx.MERGE_CANCEL_MESSAGE);
        if (customMessage != null) {
            title = LocalizeValue.localizeTODO(customMessage.first);
            message = LocalizeValue.localizeTODO(customMessage.second);
        }

        return MessageBoxes.yesNo().title(title).text(message).showAsync();
    }
}
