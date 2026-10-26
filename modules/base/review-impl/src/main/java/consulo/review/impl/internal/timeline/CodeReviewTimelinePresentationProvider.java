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
package consulo.review.impl.internal.timeline;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.presentation.TypePresentationProvider;
import consulo.review.details.model.CodeReviewDetailsTabViewModel;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * The icon of a timeline tab is the one its review asks for - the state of the review.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
@ExtensionImpl
public class CodeReviewTimelinePresentationProvider extends TypePresentationProvider<CodeReviewTimelineVirtualFile> {
    @Override
    public Class<CodeReviewTimelineVirtualFile> getItemClass() {
        return CodeReviewTimelineVirtualFile.class;
    }

    @Override
    public @Nullable String getName(CodeReviewTimelineVirtualFile file) {
        return file.getDisplayName().get();
    }

    @Override
    public @Nullable String getTypeName(CodeReviewTimelineVirtualFile file) {
        return null;
    }

    @Override
    public @Nullable Image getIcon(CodeReviewTimelineVirtualFile file) {
        CodeReviewDetailsTabViewModel tabVm = file.getTabVm();
        return tabVm == null ? null : tabVm.getIcon();
    }
}
