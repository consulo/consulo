// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.util.mac.foundation.ID;
import consulo.ui.ex.action.AnAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

final class TBItemGroup extends TBItem {
    private final ItemsContainer myGroupItems;

    TBItemGroup(@Nonnull String name, @Nullable ItemListener listener, @Nonnull List<AnAction> actions) {
        super("group", listener);
        myGroupItems = new ItemsContainer(name + "_group");
        for (AnAction action : actions) {
            myGroupItems.addItem(new TBItemAnActionButton(listener, action, null)); // TODO: pass stats from parent touchbar
        }
    }

    int size() {
        return myGroupItems.size();
    }

    TBItemAnActionButton getItem(int c) {
        return (TBItemAnActionButton) myGroupItems.get(c);
    }

    @Override
    protected ID _createNativePeer() {
        if (myGroupItems.isEmpty())
            return ID.NIL;

        final ID[] ids = myGroupItems.getNativePeers();
        return NST.createGroupItem(getUid(), ids);
    }

    @Override
    synchronized void releaseNativePeer() {
        myGroupItems.releaseAll();
        super.releaseNativePeer();
    }
}
