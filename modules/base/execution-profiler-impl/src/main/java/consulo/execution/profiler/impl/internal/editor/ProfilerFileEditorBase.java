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
package consulo.execution.profiler.impl.internal.editor;

import consulo.fileEditor.FileEditor;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import kava.beans.PropertyChangeListener;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public abstract class ProfilerFileEditorBase extends UserDataHolderBase implements FileEditor {
    private final VirtualFile myFile;
    private final LocalizeValue myName;

    private @Nullable Component myComponent;
    private volatile boolean myDisposed;

    protected ProfilerFileEditorBase(VirtualFile file, LocalizeValue name) {
        myFile = file;
        myName = name;
    }

    @RequiredUIAccess
    protected abstract Component createComponent();

    @RequiredUIAccess
    @Override
    public final Component getUIComponent() {
        Component component = myComponent;
        if (component == null) {
            component = createComponent();
            myComponent = component;
        }
        return component;
    }

    @Override
    public String getName() {
        return myName.get();
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

    protected boolean isDisposed() {
        return myDisposed;
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }
}
