// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import consulo.application.ui.UISettings;
import consulo.application.util.SystemInfo;
import consulo.application.util.mac.foundation.ID;
import consulo.awt.hacking.ComponentPeerHacking;
import consulo.awt.hacking.WritableRasterNativeHacking;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.ex.awt.EmptyIcon;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.IconLibrary;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class NST {
    private static final Logger LOG = Logger.getInstance(NST.class);
    // NOTE: JNA is stateless (doesn't have any limitations of multithreading use)
    private static NSTLibrary nstLibrary = null;

    static void loadLibrary() {
        try {
            loadLibraryImpl();
        }
        catch (Throwable e) {
            LOG.error("Failed to load nst library for touchbar: ", e);
        }

        if (nstLibrary != null) {
            // small check that loaded library works
            try {
                final ID test = nstLibrary.createTouchBar("test", (uid) -> ID.NIL, null);
                if (test == null || test.equals(ID.NIL)) {
                    LOG.error("Failed to create native touchbar object, result is null");
                    nstLibrary = null;
                }
                else {
                    nstLibrary.releaseNativePeer(test);
                    LOG.info("nst library works properly, successfully created and released native touchbar object");
                }
            }
            catch (Throwable e) {
                LOG.error("nst library was loaded, but can't be used: ", e);
                nstLibrary = null;
            }
        }
        else {
            LOG.error("nst library wasn't loaded");
        }
    }

    public static NSTLibrary loadLibraryImpl() {
        String libFileName = Platform.current().mapLibraryName("nst");

        File pluginPath = PluginManager.getPluginPath(NST.class);
        File nativePluginDirectory = new File(pluginPath, "native");
        File libFile = new File(nativePluginDirectory, libFileName);

        return nstLibrary = Native.load(libFile.getAbsolutePath(), NSTLibrary.class, Collections.singletonMap("jna.encoding", "UTF8"));
    }

    static boolean isAvailable() {
        return nstLibrary != null;
    }

    static ID createTouchBar(String name, NSTLibrary.ItemCreator creator, String escID) {
        return nstLibrary.createTouchBar(name, creator, escID); // creates autorelease-pool internally
    }

    static void releaseNativePeer(ID nativePeer) {
        nstLibrary.releaseNativePeer(nativePeer);
    }

    public static void setTouchBar(@Nullable Window window, ID touchBarNativePeer) {
        long nsViewPtr = ComponentPeerHacking.getNSViewPtr(window);
        if (nsViewPtr == 0) {
            return;
        }

        nstLibrary.setTouchBar(new ID(nsViewPtr), touchBarNativePeer);
    }

    static void selectItemsToShow(ID tbObj, String[] ids, int count) {
        nstLibrary.selectItemsToShow(tbObj, ids, count); // creates autorelease-pool internally
    }

    static void setPrincipal(ID tbObj, String uid) {
        nstLibrary.setPrincipal(tbObj, uid); // creates autorelease-pool internally
    }

    static ID createButton(String uid,
                           int buttWidth,
                           int buttFlags,
                           String text,
                           String hint, int isHintDisabled,
                           @Nullable Pair<Pointer, Dimension> raster,
                           NSTLibrary.Action action) {
        return nstLibrary.createButton(
            uid, buttWidth, buttFlags,
            text, hint,
            isHintDisabled,
            raster == null ? null : raster.getFirst(),
            raster == null ? 0 : raster.getSecond().width,
            raster == null ? 0 : raster.getSecond().height,
            action); // called from AppKit, uses per-event autorelease-pool
    }

    // NOTE: due to optimization, scrubber is created without an icon, icons must be updated async via updateScrubberItems
    @SuppressWarnings("unused")
    static ID createScrubber(
        String uid, int itemWidth, NSTLibrary.ScrubberDelegate delegate, NSTLibrary.ScrubberCacheUpdater updater,
        @Nonnull List<TBItemScrubber.ItemData> items, int visibleItems, @Nullable TouchBarStats stats
    ) {
        final Pair<Pointer, Integer> mem = _packItems(items, visibleItems, false, true);
        return nstLibrary.createScrubber(uid, itemWidth, delegate, updater, mem == null ? null : mem.getFirst(),
            mem == null ? 0 : mem.getSecond()); // called from AppKit, uses per-event autorelease-pool
    }

    static ID createGroupItem(String uid, ID[] items) {
        return nstLibrary.createGroupItem(uid, items == null || items.length == 0 ? null : items,
            items == null ? 0 : items.length); // called from AppKit, uses per-event autorelease-pool
    }

    static void updateButton(ID buttonObj,
                             int updateOptions,
                             int buttWidth,
                             int buttonFlags,
                             String text,
                             String hint, int isHintDisabled,
                             @Nullable Pair<Pointer, Dimension> raster,
                             NSTLibrary.Action action) {
        nstLibrary.updateButton(
            buttonObj, updateOptions,
            buttWidth, buttonFlags,
            text,
            hint, isHintDisabled,
            raster == null ? null : raster.getFirst(),
            raster == null ? 0 : raster.getSecond().width,
            raster == null ? 0 : raster.getSecond().height,
            action); // creates autorelease-pool internally
    }

    static void setArrowImage(ID buttObj, @Nullable Image arrow) {
        final BufferedImage img = _getImg4ByteRGBA(arrow);
        final Pointer raster4ByteRGBA = _getRaster(img);
        final int w = _getImgW(img);
        final int h = _getImgH(img);
        nstLibrary.setArrowImage(buttObj, raster4ByteRGBA, w, h); // creates autorelease-pool internally
    }

    private static Pointer _makeIndices(Collection<Integer> indices) {
        if (indices == null || indices.isEmpty()) {
            return null;
        }
        final int step = Native.getNativeSize(Integer.class);
        final Pointer mem = new Pointer(Native.malloc((long) indices.size() * step));
        int offset = 0;
        for (Integer i : indices) {
            mem.setInt(offset, i);
            offset += step;
        }
        return mem;
    }

    static void updateScrubberItems(
        TBItemScrubber scrubber, int fromIndex, int itemsCount,
        boolean withImages, boolean withText
    ) {
        final long startNs = withImages && scrubber.getStats() != null ? System.nanoTime() : 0;
        @Nonnull List<TBItemScrubber.ItemData> items = scrubber.getItems();
        final Pair<Pointer, Integer> mem = _packItems(items.subList(fromIndex, fromIndex + itemsCount), itemsCount, withImages, withText);
        synchronized (scrubber) {
            if (scrubber.myNativePeer.equals(ID.NIL)) {
                return;
            }
            nstLibrary.updateScrubberItems(scrubber.myNativePeer, mem == null ? null : mem.getFirst(), mem == null ? 0 : mem.getSecond(),
                fromIndex);
        }
        if (withImages && scrubber.getStats() != null) {
            scrubber.getStats().incrementCounter(StatsCounters.scrubberIconsProcessingDurationNs, System.nanoTime() - startNs);
        }
    }

    public static void enableScrubberItems(ID scrubObj, Collection<Integer> indices, boolean enabled) {
        if (indices == null || indices.isEmpty() || scrubObj == ID.NIL || scrubObj == null) {
            return;
        }
        final Pointer mem = _makeIndices(indices);
        nstLibrary.enableScrubberItems(scrubObj, mem, indices.size(), enabled);
    }

    public static void showScrubberItem(ID scrubObj, Collection<Integer> indices, boolean show, boolean inverseOthers) {
        if (scrubObj == ID.NIL || scrubObj == null) {
            return;
        }
        final Pointer mem = _makeIndices(indices);
        nstLibrary.showScrubberItems(scrubObj, mem, indices == null ? 0 : indices.size(), show, inverseOthers);
    }

    private static @Nullable Pair<Pointer, Integer> _packItems(
        @Nonnull List<TBItemScrubber.ItemData> items,
        int visibleItems, boolean withImages, boolean withText
    ) {
        if (items.isEmpty()) {
            return null;
        }

        long ptr = 0;
        try {
            // 1. calculate size
            int byteCount = 2; // first 2 bytes contains count of items
            int c = 0;
            for (TBItemScrubber.ItemData id : items) {
                if (c++ >= visibleItems) {
                    byteCount += 6;
                    continue;
                }
                final int textSize = 2 + (withText && id.getTextBytes() != null && id.getTextBytes().length > 0 ? id.getTextBytes().length + 1 : 0);
                byteCount += textSize;

                if (withImages
                    && id.darkIcon == null
                    && id.getIcon() != null
                    && !(id.getIcon() instanceof EmptyIcon)
                    && id.getIcon().getWidth() > 0
                    && id.getIcon().getHeight() > 0
                ) {
                    id.darkIcon = getDarkIconVariant(id.getIcon());
                    if (id.darkIcon != null && (id.darkIcon.getWidth() <= 0 || id.darkIcon.getHeight() <= 0)) {
                        LOG.debug("Can't obtain dark icon for scrubber item '%s', use default icon", id.getText());
                        id.darkIcon = id.getIcon();
                    }
                }

                if (withImages && id.darkIcon != null) {
                    id.fMulX = getIconScaleForTouchbar(id.darkIcon);
                    id.scaledWidth = Math.round(id.darkIcon.getWidth() * id.fMulX);
                    id.scaledHeight = Math.round(id.darkIcon.getHeight() * id.fMulX);
                    final int sizeInBytes = id.scaledWidth * id.scaledHeight * 4;
                    final int totalSize = sizeInBytes + 4;
                    byteCount += totalSize;
                }
                else {
                    byteCount += 4;
                }
            }

            // 2. write items
            final Pointer result = new Pointer(ptr = Native.malloc(byteCount));
            result.setShort(0, (short) items.size());
            int offset = 2;
            c = 0;
            for (TBItemScrubber.ItemData id : items) {
                if (c++ >= visibleItems) {
                    result.setShort(offset, (short) 0);
                    result.setShort(offset + 2, (short) 0);
                    result.setShort(offset + 4, (short) 0);
                    offset += 6;
                    continue;
                }

                final byte[] txtBytes = withText ? id.getTextBytes() : null;
                if (txtBytes != null && txtBytes.length > 0) {
                    result.setShort(offset, (short) txtBytes.length);
                    offset += 2;
                    result.write(offset, txtBytes, 0, txtBytes.length);
                    offset += txtBytes.length;
                    result.setByte(offset, (byte) 0);
                    offset += 1;
                }
                else {
                    result.setShort(offset, (short) 0);
                    offset += 2;
                }

                if (withImages && id.darkIcon != null) {
                    offset += _writeIconRaster(id.darkIcon, id.fMulX, result, offset, byteCount);
                }
                else {
                    final boolean hasIcon = id.getIcon() != null &&
                        !(id.getIcon() instanceof EmptyIcon) &&
                        id.getIcon().getWidth() > 0 &&
                        id.getIcon().getHeight() > 0;
                    result.setShort(offset, hasIcon ? (short) 1 : (short) 0);
                    result.setShort(offset + 2, (short) 0);
                    offset += 4;
                }
            }

            return Pair.create(result, byteCount);
        }
        catch (Throwable e) {
            if (ptr != 0) {
                Native.free(ptr);
            }
            LOG.debug(e);
            return null;
        }
    }

    static Pair<Pointer, Dimension> get4ByteRGBARaster(@Nullable Image icon) {
        if (icon == null || icon.getHeight() <= 0 || icon.getWidth() <= 0) {
            return null;
        }

        float fMulX = getIconScaleForTouchbar(icon);
        BufferedImage img = _getImg4ByteRGBA(icon, fMulX);
        return new Pair<>(_getRaster(img), new Dimension(img.getWidth(), img.getHeight()));
    }

    private static Pointer _getRaster(BufferedImage img) {
        if (img == null) {
            return null;
        }

        final DataBuffer db = img.getRaster().getDataBuffer();
        DirectDataBufferInt dbb = (DirectDataBufferInt) db;
        return dbb.myMemory;
    }

    private static int _getImgW(BufferedImage img) {
        return img == null ? 0 : img.getWidth();
    }

    private static int _getImgH(BufferedImage img) {
        return img == null ? 0 : img.getHeight();
    }

    private static BufferedImage _getImg4ByteRGBA(Image icon, float scale) {
        if (icon == null || icon.getHeight() <= 0 || icon.getWidth() <= 0) {
            return null;
        }

        final int w = Math.round(icon.getWidth() * scale);
        final int h = Math.round(icon.getHeight() * scale);

        final int memLength = w * h * 4;
        Pointer memory = new Memory(memLength);
        return _drawIconIntoMemory(icon, scale, memory, 0);
    }

    private static float getIconScaleForTouchbar(@Nonnull Image icon) {
        // according to https://developer.apple.com/macos/human-interface-guidelines/touch-bar/touch-bar-icons-and-images/
        // icons, generally should not exceed 44px in height (36px for circular icons)
        // Ideal icon size	    36px X 36px (18pt X 18pt @2x)
        // Maximum icon size    44px X 44px (22pt X 22pt @2x)
        int iconHeight = icon.getHeight();
        if (UISettings.getInstance().getPresentationMode()) {
            return 40.f / iconHeight;
        }
        else {
            return iconHeight < 24 ? 40.f / 16 : (44.f / iconHeight);
        }
    }

    private static BufferedImage _getImg4ByteRGBA(@Nullable Image icon) {
        if (icon == null || icon.getHeight() <= 0 || icon.getWidth() <= 0) {
            return null;
        }

        final float fMulX = getIconScaleForTouchbar(icon);
        return _getImg4ByteRGBA(icon, fMulX);
    }

    // returns count of written bytes
    private static int _writeIconRaster(@Nonnull Image icon, float scale, @Nonnull Pointer memory, int offset, int totalMemoryBytes)
        throws Exception {
        final int w = Math.round(icon.getWidth() * scale);
        final int h = Math.round(icon.getHeight() * scale);

        if (w <= 0 || h <= 0) {
            throw new Exception("Incorrect icon sizes: " + icon.getWidth() + "x" + icon.getHeight() + ", scale=" + scale);
        }

        final int rasterSizeInBytes = w * h * 4;
        final int totalSize = rasterSizeInBytes + 4;

        if (offset + totalSize > totalMemoryBytes) {
            throw new Exception(
                "Incorrect memory offset: offset=" + offset + ", rasterSize=" + rasterSizeInBytes + ", totalMemoryBytes=" + totalMemoryBytes);
        }

        memory.setShort(offset, (short) w);
        offset += 2;
        memory.setShort(offset, (short) h);
        offset += 2;

        _drawIconIntoMemory(icon, scale, memory, offset);

        return totalSize;
    }

    // returns count of written bytes
    private static @Nonnull BufferedImage _drawIconIntoMemory(@Nonnull Image icon, float scale, @Nonnull Pointer memory, int offset) {
        int w = Math.round(icon.getWidth() * scale);
        int h = Math.round(icon.getHeight() * scale);
        int rasterSizeInBytes = w * h * 4;

        memory.setMemory(offset, rasterSizeInBytes, (byte) 0);

        DataBuffer dataBuffer = new DirectDataBufferInt(memory, rasterSizeInBytes, offset);
        DirectColorModel colorModel =
            new DirectColorModel(ColorModel.getRGBdefault().getColorSpace(), 32, 0xFF, 0xFF00, 0x00FF0000, 0xff000000/*alpha*/, false,
                DataBuffer.TYPE_INT);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(w, h);
        WritableRaster raster = WritableRasterNativeHacking.createNativeRaster(sampleModel, dataBuffer);
        //noinspection UndesirableClassUsage
        BufferedImage image = new BufferedImage(colorModel, raster, false, null);

        icon = ImageEffects.resize(icon, scale);
        Graphics2D g = image.createGraphics();

        g.setComposite(AlphaComposite.SrcOver);
        TargetAWT.to(icon).paintIcon(null, g, 0, 0);
        g.dispose();

        return image;
    }

    public static Image getDarkIconVariant(Image image) {
        IconLibraryManager iconLibraryManager = IconLibraryManager.get();

        IconLibrary activeLibrary = iconLibraryManager.getActiveLibrary();
        // if icon theme is dark - do no change
        if (activeLibrary.isDark()) {
            return image;
        }

        return iconLibraryManager.inverseIcon(image);
    }
}