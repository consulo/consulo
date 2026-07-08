// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.ui.mac.screenmenu;

import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.application.util.SimpleTimer;
import consulo.application.util.mac.foundation.Foundation;
import consulo.application.util.mac.foundation.ID;
import consulo.awt.hacking.LWCToolkitHacking;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.ex.action.Presentation;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.collection.ArrayUtil;
import kava.beans.PropertyChangeEvent;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Java peer for a native {@code NSMenu}.
 * <p>
 * A menu is filled by buffering items ({@link #beginFill()} / {@link #add(MenuItem)} / {@link #endFill()}) and
 * pushing them to the native peer. Native open/close callbacks ({@link #menuNeedsUpdate()}, {@link #menuWillOpen()},
 * {@link #invokeMenuClosing()}) are invoked on the AppKit thread. The {@code native} methods bind to
 * {@code libmacscreenmenu*.dylib}, loaded on demand by {@link #isJbScreenMenuEnabled()}.
 */
public class Menu extends MenuItem {
    /**
     * System property set to {@code "true"} once the native screen menu is enabled; read by the platform's
     * {@code isEnabledTopMenu()} (which cannot depend on this module).
     */
    public static final String SCREEN_MENU_BAR_PROPERTY = "consulo.mac.screenMenuBar";

    private static final boolean USE_STUB = Boolean.getBoolean("jbScreenMenuBar.useStubItem");
    private static final int CLOSE_DELAY = Integer.getInteger("jbScreenMenuBar.closeDelay", 500);
    private static Boolean IS_ENABLED = null;
    private static Menu ourAppMenu = null;

    private final List<MenuItem> myItems = new ArrayList<>();
    private final List<MenuItem> myBuffer = new ArrayList<>();
    private Runnable myOnOpen;
    private Runnable myOnClose;
    private boolean mySelfManagedFill = false;
    private Component myComponent;
    private long myOpenTimeNs = 0;
    private volatile boolean myIsOpened = false;

    long[] myCachedPeers;

    public Menu(String title) {
        setTitle(title);
    }

    private Menu() {
    }

    public final boolean isAnyChildOpened() {
        for (MenuItem item : myItems) {
            if (item instanceof Menu && ((Menu)item).myIsOpened) {
                return true;
            }
        }
        return false;
    }

    public boolean isOpened() {
        return myIsOpened;
    }

    public long getOpenTimeNs() {
        return myOpenTimeNs;
    }

    /**
     * The application menu: the first item of the main menu, with title = application name, populated by the OS.
     */
    public static Menu getAppMenu() {
        if (ourAppMenu == null) {
            ourAppMenu = new Menu();
            long nsMenu = nativeGetAppMenu();
            ourAppMenu.nativePeer = ourAppMenu.nativeAttachMenu(nsMenu);
            ourAppMenu.isInHierarchy = true;
        }
        return ourAppMenu;
    }

    private static String removeMnemonic(@Nullable String src) {
        if (src == null) {
            return "";
        }
        return TextWithMnemonic.parse(src).getText();
    }

    /**
     * Replaces the OS-provided (English) application-menu item titles with branded/localized ones.
     */
    public static void renameAppMenuItems() {
        String bundleName = getBundleName();
        if (bundleName == null || bundleName.isEmpty()) {
            bundleName = ApplicationInfo.getInstance().getName();
        }

        List<String> replace = new ArrayList<>(16);

        replace.add("About.*");
        replace.add("About " + bundleName);

        replace.add("Preferences.*");
        replace.add("Preferences…");
        replace.add("Settings.*");
        replace.add("Settings…");

        replace.add("Services");
        replace.add("Services");

        replace.add("Hide " + bundleName);
        replace.add("Hide " + bundleName);

        replace.add("Hide Others");
        replace.add("Hide Others");

        replace.add("Show All");
        replace.add("Show All");

        replace.add("Quit.*");
        replace.add("Quit " + bundleName);

        nativeRenameAppMenuItems(ArrayUtil.toStringArray(replace));
    }

    public void setOnOpen(Component component, Runnable fillMenuProcedure) {
        setOnOpen(component, fillMenuProcedure, false);
    }

    /**
     * @param selfManagedFill when {@code true} the fill procedure owns the {@link #beginFill()}/{@link #endFill()}
     *                        lifecycle and may complete it asynchronously; the peer only keeps the native menu
     *                        non-empty (via a stub item) until the fill procedure refills it.
     */
    public void setOnOpen(Component component, Runnable fillMenuProcedure, boolean selfManagedFill) {
        this.myOnOpen = fillMenuProcedure;
        this.myComponent = component;
        this.mySelfManagedFill = selfManagedFill;
    }

    public void setOnClose(Component component, Runnable onClose) {
        this.myOnClose = onClose;
        this.myComponent = component;
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        String propertyName = e.getPropertyName();
        if (Presentation.PROP_TEXT.equals(propertyName) || Presentation.PROP_DESCRIPTION.equals(propertyName)) {
            setTitle(presentation.getText());
        }
    }

    public void setTitle(String label) {
        ensureNativePeer();
        if (label == null) {
            label = "";
        }
        myTitle = label;
        nativeSetTitle(nativePeer, label, isInHierarchy);
    }

    public int findIndexByTitle(String re) {
        if (re == null || re.isEmpty()) {
            return -1;
        }
        ensureNativePeer();
        return nativeFindIndexByTitle(nativePeer, re);
    }

    public synchronized MenuItem findItemByTitle(String re) {
        if (re == null || re.isEmpty()) {
            return null;
        }
        ensureNativePeer();
        long child = nativeFindItemByTitle(nativePeer, re);
        return child == 0 ? null : new MenuItem(child);
    }

    synchronized void disposeChildren(int delayMs) {
        if (delayMs <= 0) {
            for (MenuItem item : myItems) {
                item.dispose();
            }
        }
        else {
            final ArrayList<MenuItem> copy = new ArrayList<>(myItems);
            SimpleTimer.getInstance().setUp(() -> {
                for (MenuItem item : copy) {
                    item.dispose();
                }
            }, delayMs);
        }
        myItems.clear();
    }

    @Override
    public synchronized void dispose() {
        disposeChildren(0);
        myCachedPeers = null;
        super.dispose();
    }

    public void beginFill() {
        for (MenuItem item : myBuffer) {
            if (item != null) {
                Disposer.dispose(item);
            }
        }
        myBuffer.clear();
    }

    public @Nullable MenuItem add(@Nullable MenuItem item) {
        myBuffer.add(item);
        return item;
    }

    public synchronized void endFill(boolean onAppKit) {
        disposeChildren(0);

        if (myBuffer.isEmpty()) {
            return;
        }

        myCachedPeers = new long[myBuffer.size()];
        for (int c = 0; c < myBuffer.size(); ++c) {
            MenuItem menuItem = myBuffer.get(c);
            if (menuItem != null) {
                menuItem.ensureNativePeer();
                myCachedPeers[c] = menuItem.nativePeer;
                myItems.add(menuItem);
                menuItem.isInHierarchy = true;
            }
            else {
                myCachedPeers[c] = 0;
            }
        }
        myBuffer.clear();

        refillImpl(onAppKit);
    }

    public synchronized void endFill() {
        endFill(true);
    }

    synchronized void refillImpl(boolean onAppKit) {
        ensureNativePeer();
        if (myCachedPeers != null) {
            nativeRefill(nativePeer, myCachedPeers, onAppKit);
        }
    }

    public synchronized void add(MenuItem item, int position, boolean onAppKit) {
        if (position < 0) {
            return;
        }
        ensureNativePeer();
        item.ensureNativePeer();
        nativeInsertItem(nativePeer, item.nativePeer, position, onAppKit);
        myItems.add(item);
    }

    @Override
    synchronized void ensureNativePeer() {
        if (nativePeer == 0) {
            nativePeer = nativeCreateMenu();
        }
    }

    public void menuNeedsUpdate() {
        myIsOpened = true;
        if (myOnOpen == null) {
            return;
        }

        myOpenTimeNs = System.nanoTime();
        if (mySelfManagedFill) {
            MenuItem stub = new MenuItem();
            myItems.add(stub);
            stub.isInHierarchy = true;

            ensureNativePeer();
            stub.ensureNativePeer();
            nativeAddItem(nativePeer, stub.nativePeer, false);

            Application.get().invokeLater(myOnOpen);
            return;
        }
        if (USE_STUB) {
            MenuItem stub = new MenuItem();
            myItems.add(stub);
            stub.isInHierarchy = true;

            ensureNativePeer();
            stub.ensureNativePeer();
            nativeAddItem(nativePeer, stub.nativePeer, false);

            Application.get().invokeLater(() -> {
                beginFill();
                myOnOpen.run();
                endFill(true);
            });
        }
        else {
            beginFill();
            LWCToolkitHacking.invoke(myOnOpen, myComponent, true);
            endFill(false);
        }
    }

    public void menuWillOpen() {
        if (!myIsOpened) {
            getLogger().debug("menuNeedsUpdate wasn't called for '" + myTitle + "', will do it now");
            menuNeedsUpdate();
        }
    }

    public void invokeMenuClosing() {
        myIsOpened = false;
        disposeChildren(CLOSE_DELAY);

        if (myOnClose != null) {
            LWCToolkitHacking.invoke(myOnClose, myComponent, false);
        }
    }

    private native long nativeCreateMenu();

    private native long nativeAttachMenu(long nsMenu);

    private native long nativeFindItemByTitle(long menuPtr, String re);

    private native int nativeFindIndexByTitle(long menuPtr, String re);

    private native void nativeSetTitle(long menuPtr, String title, boolean onAppKit);

    private native void nativeAddItem(long menuPtr, long itemPtr, boolean onAppKit);

    private native void nativeInsertItem(long menuPtr, long itemPtr, int position, boolean onAppKit);

    native void nativeRefill(long menuPtr, long[] newItems, boolean onAppKit);

    private static native void nativeInitClass();

    private static native long nativeGetAppMenu();

    private static native void nativeRenameAppMenuItems(String[] replacements);

    /**
     * Loads the native library on the first call (macOS only, and only when the JDK screen menu is not requested via
     * {@code apple.laf.useScreenMenuBar}) and caches whether the native screen menu is usable.
     */
    public static boolean isJbScreenMenuEnabled() {
        if (IS_ENABLED != null) {
            return IS_ENABLED;
        }

        IS_ENABLED = false;

        if (!Platform.current().os().isMac()) {
            return false;
        }

        if (Boolean.getBoolean("apple.laf.useScreenMenuBar")) {
            getLogger().info("apple.laf.useScreenMenuBar==true, default screen menu implementation will be used");
            return false;
        }

        String libFileName = Platform.current().mapLibraryName("macscreenmenu");
        File pluginPath = PluginManager.getPluginPath(Menu.class);
        File libFile = new File(new File(pluginPath, "native"), libFileName);
        try {
            System.load(libFile.getAbsolutePath());
            nativeInitClass();
            Menu test = new Menu("test");
            test.ensureNativePeer();
            Disposer.dispose(test);
        }
        catch (Throwable e) {
            getLogger().warn("can't load menu library: " + libFile + ", exception: " + e.getMessage());
            return false;
        }

        IS_ENABLED = true;
        // let consulo.platform know a native top (screen) menu is in use, so the platform reports isEnabledTopMenu()
        System.setProperty(SCREEN_MENU_BAR_PROPERTY, "true");
        getLogger().info("use new ScreenMenuBar implementation");
        return true;
    }

    private static Logger getLogger() {
        return Logger.getInstance(Menu.class);
    }

    private static @Nullable String getBundleName() {
        String bundleName;
        final ID nativePool = Foundation.invoke("NSAutoreleasePool", "new");
        try {
            final ID bundle = Foundation.invoke("NSBundle", "mainBundle");
            final ID dict = Foundation.invoke(bundle, "infoDictionary");
            final ID nsBundleName = Foundation.invoke(dict, "objectForKey:", Foundation.nsString("CFBundleName"));
            bundleName = Foundation.toStringViaUTF8(nsBundleName);
        }
        finally {
            Foundation.invoke(nativePool, "release");
        }
        return bundleName;
    }
}
