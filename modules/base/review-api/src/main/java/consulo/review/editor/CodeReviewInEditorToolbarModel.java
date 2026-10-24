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

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * What the review toolbar of an editor shows - put on the editor by {@link ReviewInEditorUtil#showReviewToolbar}, read by
 * the inspection widget of the editor.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public record CodeReviewInEditorToolbarModel(CodeReviewInEditorViewModel vm,
                                             List<AnAction> additionalActions,
                                             @Nullable Supplier<LocalizeValue> customWarningSupplier) {
    public static final Key<CodeReviewInEditorToolbarModel> KEY = Key.create(CodeReviewInEditorToolbarModel.class.getName());
}
