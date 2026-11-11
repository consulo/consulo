// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.module.content.internal;

public final class WorkspaceFileKindMask {
    public static final int CONTENT = 1;
    public static final int EXTERNAL_BINARY = 2;
    public static final int EXTERNAL_SOURCE = 4;
    public static final int EXTERNAL = EXTERNAL_SOURCE | EXTERNAL_BINARY;
    public static final int CUSTOM = 8;
    public static final int CONTENT_NON_INDEXABLE = 16;
    public static final int EXTERNAL_NON_INDEXABLE = 32;
    public static final int ALL = CONTENT | EXTERNAL | CUSTOM | CONTENT_NON_INDEXABLE | EXTERNAL_NON_INDEXABLE;

    private WorkspaceFileKindMask() {
    }
}
