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
package consulo.codeEditor.toolbar.floating;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import org.jspecify.annotations.Nullable;

/**
 * Fills the floating code toolbar of an editor. The first provider answering with a configuration wins.
 *
 * @author VISTALL
 * @since 2026-11-09
 * @see CodeFloatingToolbar
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CodeFloatingToolbarProvider {
    /**
     * Called in a read action, off the ui thread.
     *
     * @return {@code null} when the toolbar is not available for the selection
     */
    @RequiredReadAction
    @Nullable
    CodeFloatingToolbarConfiguration getConfiguration(Editor editor, CodeFloatingToolbarSelection selection);
}
