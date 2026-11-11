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

import consulo.ai.localize.AILocalize;
import consulo.fileEditor.FileEditor;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import kava.beans.PropertyChangeListener;
import org.jspecify.annotations.Nullable;

/**
 * The editor tab of a chat. The chat panel comes once the history of the chat is loaded; closing the tab ends the
 * conversation with the provider, while the chat itself stays in the tree.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public final class AIChatFileEditor extends UserDataHolderBase implements FileEditor {
    private final AIChatVirtualFile myFile;
    private final AIChatView myChatView;

    private @Nullable LoadingLayout<DockLayout> myComponent;
    private volatile boolean myDisposed;

    public AIChatFileEditor(AIChatVirtualFile file, AIChatView chatView) {
        myFile = file;
        myChatView = chatView;
    }

    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        LoadingLayout<DockLayout> component = myComponent;
        if (component == null) {
            component = LoadingLayout.create(DockLayout.create(), this);
            myComponent = component;
            myChatView.fill(myFile, component, this);
        }
        return component;
    }

    @Override
    public String getName() {
        return AILocalize.fileTypeChat().get();
    }

    @Override
    public VirtualFile getFile() {
        return myFile;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return myFile.isValid();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }

    boolean isDisposed() {
        return myDisposed;
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }
}
