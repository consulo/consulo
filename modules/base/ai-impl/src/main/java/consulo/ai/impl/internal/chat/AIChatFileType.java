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

import consulo.ai.icon.AIIconGroup;
import consulo.ai.localize.AILocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.fileType.FileType;

/**
 * The type of the file a chat is opened as in an editor tab.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public final class AIChatFileType implements FileType {
    public static final AIChatFileType INSTANCE = new AIChatFileType();

    private AIChatFileType() {
    }

    @Override
    public String getId() {
        return "AI_CHAT";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return AILocalize.fileTypeChat();
    }

    @Override
    public LocalizeValue getDescription() {
        return AILocalize.fileTypeChat();
    }

    @Override
    public Image getIcon() {
        return AIIconGroup.toolwindowAiassistant();
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
