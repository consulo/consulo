// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

@FunctionalInterface
public interface FileChannelOpener {
    FileChannel open(Path path, boolean readOnly) throws IOException;
}
