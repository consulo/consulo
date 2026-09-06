// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.index.io.PersistentEnumerator;

import java.io.IOException;
import java.nio.file.Path;

public class PersistentIndexingStampEnumerator extends PersistentEnumerator<TimestampsImmutable> {
    public static PersistentIndexingStampEnumerator createTimestampsEnumerator(Path path) throws IOException {
        return new PersistentIndexingStampEnumerator(path);
    }

    public PersistentIndexingStampEnumerator(Path path) throws IOException {
        super(path.toFile(), new TimestampsKeyDescriptor(), 1024, null, 1);
    }
}
