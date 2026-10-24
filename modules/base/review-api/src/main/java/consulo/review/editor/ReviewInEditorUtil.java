// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.editor;

import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public final class ReviewInEditorUtil {
    private ReviewInEditorUtil() {
    }

    public static int transferLineToAfter(List<CodeReviewChangedRange> ranges, int line) {
        return CodeReviewEditorDocumentUtil.transferLineToAfter(ranges, line);
    }

    public static @Nullable Integer transferLineFromAfter(List<CodeReviewChangedRange> ranges, int line) {
        return transferLineFromAfter(ranges, line, false);
    }

    public static @Nullable Integer transferLineFromAfter(List<CodeReviewChangedRange> ranges, int line, boolean approximate) {
        return CodeReviewEditorDocumentUtil.transferLineFromAfter(ranges, line, approximate);
    }

    public static boolean isLastBlankLine(Document document, int lineIdx) {
        return CodeReviewEditorDocumentUtil.isLastBlankLine(document, lineIdx);
    }

    /**
     * Sets up an inspection widget action group for review in editor, until {@code parentDisposable} is disposed
     */
    @RequiredUIAccess
    public static void showReviewToolbar(Disposable parentDisposable, CodeReviewInEditorViewModel vm, Editor editor) {
        showReviewToolbarWithActions(parentDisposable, vm, editor);
    }

    @RequiredUIAccess
    public static void showReviewToolbarWithActions(Disposable parentDisposable,
                                                    CodeReviewInEditorViewModel vm,
                                                    Editor editor,
                                                    AnAction... additionalActions) {
        showInspectionWidgetAction(parentDisposable, editor, new CodeReviewInEditorToolbarModel(vm, List.of(additionalActions), null));
    }

    /**
     * This is a very special case for GitLab plugin to show on a file with an empty diff
     */
    @RequiredUIAccess
    public static void showReviewToolbarWithWarning(Disposable parentDisposable,
                                                    CodeReviewInEditorViewModel vm,
                                                    Editor editor,
                                                    Supplier<LocalizeValue> warningSupplier,
                                                    AnAction... additionalActions) {
        showInspectionWidgetAction(parentDisposable, editor,
            new CodeReviewInEditorToolbarModel(vm, List.of(additionalActions), warningSupplier));
    }

    @RequiredUIAccess
    private static void showInspectionWidgetAction(Disposable parentDisposable, Editor editor, CodeReviewInEditorToolbarModel model) {
        editor.putUserData(CodeReviewInEditorToolbarModel.KEY, model);
        Disposer.register(parentDisposable, () -> {
            if (editor.getUserData(CodeReviewInEditorToolbarModel.KEY) == model) {
                editor.putUserData(CodeReviewInEditorToolbarModel.KEY, null);
            }
        });
    }
}
