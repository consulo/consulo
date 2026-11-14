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
package consulo.ai.impl.internal.chat;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.EditorTabTitleProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * A chat tab shows the start of the title - a title is often the whole first message; the tooltip has all of it.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class AIChatTabTitleProvider implements EditorTabTitleProvider, DumbAware {
    private static final int MAX_LENGTH = 24;

    @Override
    public @Nullable String getEditorTabTitle(Project project, VirtualFile file) {
        if (!(file instanceof AIChatVirtualFile)) {
            return null;
        }
        String title = file.getName();
        return title.length() <= MAX_LENGTH ? title : title.substring(0, MAX_LENGTH - 1).strip() + "…";
    }

    @Override
    public @Nullable String getEditorTabTooltipText(Project project, VirtualFile file) {
        return file instanceof AIChatVirtualFile ? file.getName() : null;
    }
}
