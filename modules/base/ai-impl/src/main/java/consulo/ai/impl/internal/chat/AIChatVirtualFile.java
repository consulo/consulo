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

import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A chat as the file of its editor tab - one per chat, so opening the chat again selects the tab it already has.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public final class AIChatVirtualFile extends LightVirtualFileBase {
    private final VirtualFileSystem myFileSystem;
    private final String myPath;
    private final String myChatId;
    private final @Nullable Image myIcon;

    public AIChatVirtualFile(VirtualFileSystem fileSystem, String path, String chatId, String title, @Nullable Image icon) {
        super(title, AIChatFileType.INSTANCE, System.currentTimeMillis());
        myFileSystem = fileSystem;
        myPath = path;
        myChatId = chatId;
        myIcon = icon;
    }

    @Override
    public VirtualFileSystem getFileSystem() {
        return myFileSystem;
    }

    @Override
    public String getPath() {
        return myPath;
    }

    /**
     * The icon of the provider of the chat - null when the provider is gone.
     */
    public @Nullable Image getIcon() {
        return myIcon;
    }

    public String getChatId() {
        return myChatId;
    }

    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new IOException("A chat can't be written: " + getName());
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        return new byte[0];
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(new byte[0]);
    }
}
