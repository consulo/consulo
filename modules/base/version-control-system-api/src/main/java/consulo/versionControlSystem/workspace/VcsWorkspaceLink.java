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
package consulo.versionControlSystem.workspace;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * A link of the empty workspace view, e.g. "Create a worktree".
 */
public final class VcsWorkspaceLink {
    private final LocalizeValue myText;
    private final @Nullable Image myIcon;
    private final Runnable myAction;

    public VcsWorkspaceLink(LocalizeValue text, @Nullable Image icon, @RequiredUIAccess Runnable action) {
        myText = text;
        myIcon = icon;
        myAction = action;
    }

    public LocalizeValue getText() {
        return myText;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    @RequiredUIAccess
    public void run() {
        myAction.run();
    }
}
