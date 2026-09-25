// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io;

import java.io.IOException;
import java.nio.file.Path;

public class CorruptedException extends IOException {
    public CorruptedException(Path file) {
        this("Storage corrupted " + file);
    }

    public CorruptedException(Path file,
                              Throwable cause) {
        this("Storage corrupted " + file, cause);
    }

    public CorruptedException(String message) {
        super(message);
    }

    public CorruptedException(String message,
                              Throwable cause) {
        super(message, cause);
    }
}
