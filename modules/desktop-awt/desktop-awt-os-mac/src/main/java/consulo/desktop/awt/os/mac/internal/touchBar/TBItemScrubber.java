// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.performance.ActivityTracker;
import consulo.application.util.mac.foundation.ID;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class TBItemScrubber extends TBItem implements NSTLibrary.ScrubberDelegate {
    private final int myWidth;
    private final @Nullable TouchBarStats myStats;
    private final NSTLibrary.ScrubberCacheUpdater myUpdater;
    private final List<ItemData> myItems = new ArrayList<>();
    private int myNativeItemsCount;

    // NOTE: should be completely filled before native peer creation
    // (now it assumed that updateScrubberItems traverse fixed collection items)
    TBItemScrubber(@Nullable ItemListener listener, @Nullable TouchBarStats stats, int scrubWidth) {
        super("scrubber", listener);
        myWidth = scrubWidth;
        myStats = stats;
        myUpdater = () -> {
            // NOTE: called from AppKit (when last cached item become visible)
            if (myItems.isEmpty())
                return 0;
            if (myNativeItemsCount >= myItems.size())
                return 0;

            final int chunkSize = 25;
            final int newItemsCount = Math.min(chunkSize, myItems.size() - myNativeItemsCount);
            final int fromPosition = myNativeItemsCount;
            NST.updateScrubberItems(this, fromPosition, newItemsCount, false, true);

            final @Nonnull Application app = ApplicationManager.getApplication();
            app.executeOnPooledThread(() -> NST.updateScrubberItems(this, fromPosition, newItemsCount, true, false));

            myNativeItemsCount += newItemsCount;
            return newItemsCount;
        };
    }

    @Nonnull
    List<ItemData> getItems() {
        return myItems;
    }

    @Nullable
    TouchBarStats getStats() {
        return myStats;
    }

    // NOTE: designed to be completely filled before usage
    public TBItemScrubber addItem(Image icon, String text, Runnable action) {
        final Runnable nativeAction = action == null && myListener == null ? null : () -> {
            if (action != null)
                action.run();
            if (myListener != null)
                myListener.onItemEvent(this, 0);
            ActivityTracker.getInstance().inc();
        };
        myItems.add(new ItemData(icon, text, nativeAction));
        return this;
    }

    void enableItems(Collection<Integer> indices, boolean enabled) {
        if (indices == null || indices.isEmpty())
            return;

        for (int c = 0; c < myItems.size(); ++c) {
            if (!indices.contains(c))
                continue;
            final ItemData id = myItems.get(c);
            id.myEnabled = enabled;
        }

        synchronized (this) {
            NST.enableScrubberItems(myNativePeer, indices, enabled);
        }
    }

    void showItems(Collection<Integer> indices, boolean visible, boolean inverseOthers) {
        synchronized (this) {
            NST.showScrubberItem(myNativePeer, indices, visible, inverseOthers);
        }
    }

    @Override
    protected ID _createNativePeer() {
        myNativeItemsCount = myItems.isEmpty() ? 0 : Math.min(30, myItems.size());
        final ID result = NST.createScrubber(getUid(), myWidth, this, myUpdater, myItems, myNativeItemsCount, myStats);
        NST.enableScrubberItems(result, _getDisabledIndices(), false);
        if (myNativeItemsCount > 0 && result != ID.NIL) {
            final @Nonnull Application app = ApplicationManager.getApplication();
            app.executeOnPooledThread(() -> NST.updateScrubberItems(this, 0, myNativeItemsCount, true, false));
        }
        return result;
    }

    @Override
    public void execute(int itemIndex) {
        if (myItems.isEmpty() || itemIndex < 0 || itemIndex >= myItems.size())
            return;
        final ItemData id = myItems.get(itemIndex);
        if (id != null && id.myAction != null)
            id.myAction.run();
    }

    private List<Integer> _getDisabledIndices() {
        final List<Integer> disabled = new ArrayList<>();
        for (int c = 0; c < myItems.size(); ++c) {
            if (!myItems.get(c).myEnabled)
                disabled.add(c);
        }
        return disabled;
    }

    static final class ItemData {
        private byte[] myTextBytes; // cache
        private final Image myIcon;

        private final String myText;
        private final Runnable myAction;
        private boolean myEnabled = true;

        // cache fields (are filled during packing, just for convenience)
        float fMulX = 0;
        Image darkIcon = null;
        int scaledWidth = 0;
        int scaledHeight = 0;

        ItemData(Image icon, String text, Runnable action) {
            myIcon = icon;
            myText = text;
            myAction = action;
        }

        Image getIcon() {
            return myIcon;
        }

        String getText() {
            return myText;
        }

        byte[] getTextBytes() {
            if (myTextBytes == null && myText != null) {
                myTextBytes = myText.getBytes(StandardCharsets.UTF_8);
            }

            return myTextBytes;
        }
    }
}
