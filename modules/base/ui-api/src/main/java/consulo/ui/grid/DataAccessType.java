// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

public enum DataAccessType {
    DATA_WITH_MUTATIONS {
        @Override
        public <Row, Column> GridModel<Row, Column> getModel(MutationSupport<Row, Column> support) {
            return support.getMutationModel();
        }
    },
    DATABASE_DATA {
        @Override
        public <Row, Column> GridModel<Row, Column> getModel(MutationSupport<Row, Column> support) {
            return support.getDataModel();
        }
    };

    public abstract <Row, Column> GridModel<Row, Column> getModel(MutationSupport<Row, Column> support);
}
