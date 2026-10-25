// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorAndFontDescriptorsProvider;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.localize.LocalizeValue;
import consulo.review.localize.ReviewLocalize;

@ExtensionImpl
public class ReviewColorsPageFactory implements ColorAndFontDescriptorsProvider {
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return new AttributesDescriptor[0];
    }

    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return new ColorDescriptor[]{
            new ColorDescriptor(ReviewLocalize.reviewColorDescriptorReviewChangedLines(),
                CodeReviewColorUtil.REVIEW_CHANGED_LINES_COLOR,
                ColorDescriptor.Kind.BACKGROUND)
        };
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ReviewLocalize.reviewColorCollaboration();
    }
}
