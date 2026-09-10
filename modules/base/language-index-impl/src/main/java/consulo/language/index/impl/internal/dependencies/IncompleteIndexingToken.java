// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

public class IncompleteIndexingToken {
    private volatile boolean mySuccessful = true;

    public void markUnsuccessful() {
        mySuccessful = false;
    }

    public boolean isSuccessful() {
        return mySuccessful;
    }
}
