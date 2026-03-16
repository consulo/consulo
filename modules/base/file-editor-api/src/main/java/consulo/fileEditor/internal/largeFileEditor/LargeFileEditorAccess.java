// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.internal.largeFileEditor;

import consulo.codeEditor.Editor;
import consulo.virtualFileSystem.VirtualFile;

import java.nio.charset.Charset;

public interface LargeFileEditorAccess {

    
    VirtualFile getVirtualFile();

    
    Editor getEditor();

    /**
     * @return true - if success, false - otherwise
     */
    boolean tryChangeEncoding(Charset charset);

    String getCharsetName();
}
