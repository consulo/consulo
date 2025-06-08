// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.util.mac.foundation.ID;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

public abstract class TBItem {
    private final @Nonnull String myName;
    private @Nullable String myUid;

    protected @Nonnull ID myNativePeer = ID.NIL; // java wrapper holds native object

    final @Nullable ItemListener myListener;
    boolean myIsVisible = true;

    TBItem(@Nonnull @NonNls String name, @Nullable ItemListener listener) {
        myName = name;
        myListener = listener;
    }

    @Override
    public String toString() {
        return myUid == null ? String.format("%s [null-uid]", myName) : myUid;
    }

    @Nonnull
    String getName() {
        return myName;
    }

    @TestOnly
    public @Nonnull ID getNativePeer() {
        return myNativePeer;
    }

    @Nullable
    String getUid() {
        return myUid;
    }

    void setUid(@Nullable String uid) {
        myUid = uid;
    }

    synchronized
    @Nonnull ID createNativePeer() {
        // called from AppKit (when NSTouchBarDelegate create items)
        if (myNativePeer == ID.NIL) {
            myNativePeer = _createNativePeer();
        }
        return myNativePeer;
    }

    synchronized void releaseNativePeer() {
        if (myNativePeer == ID.NIL)
            return;
        NST.releaseNativePeer(myNativePeer);
        myNativePeer = ID.NIL;
    }

    protected abstract ID _createNativePeer();    // called from AppKit
}
