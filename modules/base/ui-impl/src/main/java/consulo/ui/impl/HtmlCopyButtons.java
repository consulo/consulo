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
package consulo.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.HtmlView;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * What every frontend shares about the copy buttons of {@link HtmlView.RenderData#copyTargets()}: the link a button is, the image
 * it shows and where it sits over its target
 */
public final class HtmlCopyButtons {
    /**
     * The href of a copy button, followed by the id of its target. A frontend handles it itself and never reports it as a link.
     */
    public static final String HREF_PREFIX = "consulo-copy:";

    /**
     * The id of the image of a copy button under {@link HtmlView#IMAGE_SRC_PREFIX}, which the view resolves before asking the
     * image resolver of the caller
     */
    public static final String IMAGE_ID = "consulo-copy-button";

    /**
     * The gap between a button and the top right corner of its target
     */
    public static final int INSET = 4;

    public static @Nullable String getTargetId(String href) {
        return href.startsWith(HREF_PREFIX) ? href.substring(HREF_PREFIX.length()) : null;
    }

    public static Image getImage() {
        return PlatformIconGroup.actionsCopy();
    }

    public static LocalizeValue getTooltip() {
        return CommonLocalize.buttonCopy();
    }

    private HtmlCopyButtons() {
    }
}
