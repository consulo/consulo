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
package consulo.fileEditor.impl.internal.unified;

import consulo.ui.clipboard.DataTransferType;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public final class EditorTabTransfer {
    public static final DataTransferType<EditorTabTransfer> TYPE = DataTransferType.create("consulo.fileEditor.tab");

    private final UnifiedFileEditorWindow myWindow;
    private final VirtualFile myFile;
    private final boolean myCopy;

    public EditorTabTransfer(UnifiedFileEditorWindow window, VirtualFile file, boolean copy) {
        myWindow = window;
        myFile = file;
        myCopy = copy;
    }

    public UnifiedFileEditorWindow getWindow() {
        return myWindow;
    }

    public VirtualFile getFile() {
        return myFile;
    }

    public boolean isCopy() {
        return myCopy;
    }
}
