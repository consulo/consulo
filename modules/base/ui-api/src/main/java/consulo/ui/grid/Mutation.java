// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

public abstract class Mutation {
    private final ModelIndex<GridRow> myRow;

    protected Mutation(ModelIndex<GridRow> row) {
        myRow = row;
    }

    public ModelIndex<GridRow> getRow() {
        return myRow;
    }
}
