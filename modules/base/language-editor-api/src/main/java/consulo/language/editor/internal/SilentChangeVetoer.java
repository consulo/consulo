// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;

@ExtensionAPI(ComponentScope.PROJECT)
public interface SilentChangeVetoer {
    
    ThreeState canChangeFileSilently(VirtualFile virtualFile);
}
