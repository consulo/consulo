// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.util.concurrent.ActionCallback;
import org.jspecify.annotations.Nullable;

public class GridRequestSource {
    public final @Nullable RequestPlace place;
    private final ActionCallback myCallback = new ActionCallback();
    public Phase phase = Phase.FIRST;
    private boolean myErrorOccurred;
    private @Nullable String myErrorMessage;
    private boolean myMutatedDataLocally;

    public GridRequestSource(@Nullable RequestPlace place) {
        this.place = place;
    }

    public ActionCallback getActionCallback() {
        return myCallback;
    }

    public static GridRequestSource create(@Nullable RequestPlace source) {
        return new GridRequestSource(source);
    }

    public void requestComplete(boolean success) {
        if (success) {
            myCallback.setDone();
        }
        else {
            myCallback.setRejected();
        }
    }

    public void clearError() {
        myErrorOccurred = false;
        myErrorMessage = null;
    }

    public void setErrorOccurred(String message) {
        myErrorOccurred = true;
        myErrorMessage = message;
    }

    public boolean errorOccurred() {
        return myErrorOccurred;
    }

    public @Nullable String getErrorMessage() {
        return myErrorMessage;
    }

    /**
     * Marks requests whose result should replace pending row-local mutation state instead of preserving it on equal-row reloads.
     * This is used both for submit-driven reloads and for explicit user actions that discard local changes before reloading.
     */
    public void setMutatedDataLocally(boolean value) {
        myMutatedDataLocally = value;
    }

    public boolean isMutatedDataLocally() {
        return myMutatedDataLocally;
    }

    public enum Phase {
        PREPARE,
        FIRST,
        LOAD_VALUES_USING_KEYS,
        COUNT
    }

    public interface RequestPlace {
        enum RowIdPolicy {
            AUTO,
            EXCLUDE
        }

        default RowIdPolicy rowIdPolicy() {
            return RowIdPolicy.AUTO;
        }
    }

    public interface GridRequestPlace<Row, Column> extends RequestPlace {
        CoreGrid<Row, Column> getGrid();
    }
}
