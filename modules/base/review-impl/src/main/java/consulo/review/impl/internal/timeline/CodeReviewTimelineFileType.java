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

import consulo.localize.LocalizeValue;
import consulo.review.icon.ReviewIconGroup;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.fileType.FileType;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class CodeReviewTimelineFileType implements FileType {
    public static final CodeReviewTimelineFileType INSTANCE = new CodeReviewTimelineFileType();

    private CodeReviewTimelineFileType() {
    }

    @Override
    public String getId() {
        return "CodeReviewTimeline";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ReviewLocalize.reviewTimelineEditorName();
    }

    @Override
    public LocalizeValue getDescription() {
        return ReviewLocalize.reviewTimelineEditorName();
    }

    @Override
    public String getDefaultExtension() {
        return "";
    }

    @Override
    public Image getIcon() {
        return ReviewIconGroup.pullrequestopen();
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
