// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

public interface MutationSupport<Row, Column> {
    GridModel<Row, Column> getMutationModel();

    GridModel<Row, Column> getDataModel();

    default GridModel<Row, Column> getModel(DataAccessType type) {
        return type.getModel(this);
    }
}
