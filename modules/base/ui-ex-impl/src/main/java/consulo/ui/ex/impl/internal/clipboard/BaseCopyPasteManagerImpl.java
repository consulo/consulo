/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.ex.impl.internal.clipboard;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.Clipboard;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.ex.CopyPasteManager;
import consulo.ui.ex.internal.CopyPasteManagerInternal;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
public abstract class BaseCopyPasteManagerImpl implements CopyPasteManagerInternal {
    private static final int HISTORY_LIMIT = 20;

    /**
     * State of one {@link UIAccess}. Desktop answers the same one forever, web answers one per browser
     * session.
     */
    public static class SessionState {
        private final List<DataTransfer> myHistory = new ArrayList<>();
        private @Nullable Disposable mySubscription;

        public SessionState() {
        }
    }

    private final List<BiConsumer<DataTransfer, DataTransfer>> myListeners = new CopyOnWriteArrayList<>();

    // the cut marker and the kill ring flag are read from places which have no ui thread to offer, so
    // they stay on the service rather than following a session
    private volatile Object @Nullable [] myCutElements;
    private volatile boolean myKillRingBroken;

    @RequiredUIAccess
    protected abstract SessionState getSessionState(UIAccess uiAccess);

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> setContents(DataTransfer transfer) {
        UIAccess uiAccess = UIAccess.current();
        SessionState state = bound(uiAccess);

        synchronized (state.myHistory) {
            state.myHistory.remove(transfer);
            state.myHistory.add(0, transfer);
            while (state.myHistory.size() > HISTORY_LIMIT) {
                state.myHistory.remove(state.myHistory.size() - 1);
            }
        }
        myKillRingBroken = false;

        return uiAccess.getClipboard().setContents(transfer);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<DataTransfer> getContents() {
        UIAccess uiAccess = UIAccess.current();
        bound(uiAccess);

        return uiAccess.getClipboard().getContents();
    }

    @Override
    @RequiredUIAccess
    public DataTransfer getLocalContents() {
        UIAccess uiAccess = UIAccess.current();
        bound(uiAccess);

        return uiAccess.getClipboard().getLocalContents();
    }

    @Override
    @RequiredUIAccess
    public List<DataTransfer> getHistory() {
        SessionState state = bound(UIAccess.current());
        synchronized (state.myHistory) {
            return List.copyOf(state.myHistory);
        }
    }

    @Override
    @RequiredUIAccess
    public void removeFromHistory(DataTransfer transfer) {
        SessionState state = bound(UIAccess.current());
        synchronized (state.myHistory) {
            state.myHistory.remove(transfer);
        }
    }

    @Override
    @RequiredUIAccess
    public void addContentListener(BiConsumer<DataTransfer, DataTransfer> listener, Disposable parent) {
        bound(UIAccess.current());

        myListeners.add(listener);
        Disposer.register(parent, () -> myListeners.remove(listener));
    }

    @Override
    @RequiredUIAccess
    public void setCutElements(Object @Nullable [] elements) {
        myCutElements = elements;
    }

    @Override
    public boolean isCutElement(@Nullable Object element) {
        Object[] elements = myCutElements;
        if (elements == null) {
            return false;
        }

        for (Object cut : elements) {
            if (cut.equals(element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void stopKillRings() {
        myKillRingBroken = true;
    }

    @Override
    public boolean isKillRingBroken() {
        return myKillRingBroken;
    }

    /**
     * Subscribing here rather than at registration is what lets a listener survive the death of the
     * {@link UIAccess} it was registered on - a new session simply gets subscribed on first use and the
     * registrations are untouched.
     */
    @RequiredUIAccess
    private SessionState bound(UIAccess uiAccess) {
        SessionState state = getSessionState(uiAccess);
        if (state.mySubscription == null) {
            Clipboard clipboard = uiAccess.getClipboard();
            state.mySubscription = clipboard.addContentListener(this::fireContentChanged);
        }
        return state;
    }

    private void fireContentChanged(DataTransfer oldTransfer, DataTransfer newTransfer) {
        for (BiConsumer<DataTransfer, DataTransfer> listener : myListeners) {
            listener.accept(oldTransfer, newTransfer);
        }
    }
}
