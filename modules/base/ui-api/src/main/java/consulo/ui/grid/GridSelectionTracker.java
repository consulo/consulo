// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

public interface GridSelectionTracker {
    void performOperation(GridSelectionTracker.Operation operation);

    boolean canPerformOperation(GridSelectionTracker.Operation operation);

    interface Operation {
        boolean checkStackSize(int size);

        boolean checkSelectedColumnsCount(int count);

        boolean checkSelectedRowsCount(int count);

        boolean perform(GridSelectionTracker tracker);
    }
}
