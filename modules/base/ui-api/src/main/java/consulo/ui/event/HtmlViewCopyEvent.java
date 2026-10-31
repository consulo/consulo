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
package consulo.ui.event;

import consulo.ui.HtmlView;

/**
 * A copy button of {@link HtmlView.RenderData#copyTargets()} was clicked and its value is on the clipboard
 */
public final class HtmlViewCopyEvent extends ComponentEvent<HtmlView> {
    private final String myTargetId;

    public HtmlViewCopyEvent(HtmlView component, String targetId) {
        super(component);
        myTargetId = targetId;
    }

    /**
     * The {@code id} of the element whose button was clicked
     */
    public String getTargetId() {
        return myTargetId;
    }
}
