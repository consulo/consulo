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

import consulo.ui.HtmlView;
import org.jspecify.annotations.Nullable;

/**
 * What every frontend shares about the buttons of {@link HtmlView.RenderData#gutterActions()}: the link a button is, the image it
 * shows and where it sits next to its target
 */
public final class HtmlGutterActions {
    /**
     * The href of a gutter button, followed by the id of its target. A frontend handles it itself and never reports it as a link.
     */
    public static final String HREF_PREFIX = "consulo-gutter:";

    /**
     * The id of the image of a gutter button under {@link HtmlView#IMAGE_SRC_PREFIX}, followed by the id of its target, which the
     * view resolves to the icon of the action before asking the image resolver of the caller
     */
    public static final String IMAGE_ID_PREFIX = "consulo-gutter-";

    /**
     * The gap between a button and the first line of its target
     */
    public static final int GAP = 2;

    public static @Nullable String getTargetId(String href) {
        return href.startsWith(HREF_PREFIX) ? href.substring(HREF_PREFIX.length()) : null;
    }

    public static @Nullable String getImageTargetId(String imageId) {
        return imageId.startsWith(IMAGE_ID_PREFIX) ? imageId.substring(IMAGE_ID_PREFIX.length()) : null;
    }

    private HtmlGutterActions() {
    }
}
