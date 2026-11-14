// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem;

import java.io.IOException;

/**
 * The file system cannot answer now, because the environment behind the path is not connected.
 * The state of the file is unknown. It is not "absent".
 * <p>
 * The changes of a file which cannot be written now are kept until the environment answers again.
 */
public final class FileSystemUnavailableException extends IOException {
    public FileSystemUnavailableException(Throwable cause) {
        super(cause);
    }
}
