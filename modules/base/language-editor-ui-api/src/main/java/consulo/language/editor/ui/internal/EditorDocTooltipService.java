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
package consulo.language.editor.ui.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Shows the ctrl-hover quick-doc tooltip for an editor offset. Frontends without a hint surface answer
 * {@code null} from {@link #show} and the feature degrades to the underline and the hand cursor.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface EditorDocTooltipService {
    /**
     * @param linkActivated receives the activated hyperlink description, already filtered to activation events
     */
    @RequiredUIAccess
    @Nullable
    EditorDocTooltip show(Editor editor, int offset, String html, @Nullable Consumer<String> linkActivated);

    /**
     * Hides every open hint of this frontend, the sweep that accompanies dropping the ctrl highlight.
     */
    void hideAllHints();
}
