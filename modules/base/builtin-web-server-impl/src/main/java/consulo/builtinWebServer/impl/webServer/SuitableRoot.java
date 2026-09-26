// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.builtinWebServer.impl.webServer;

import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

record SuitableRoot(VirtualFile file, @Nullable String moduleQualifier) {
}
