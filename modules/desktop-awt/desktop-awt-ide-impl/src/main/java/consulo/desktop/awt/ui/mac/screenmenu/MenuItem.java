// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.ui.mac.screenmenu;

import consulo.disposer.Disposable;
import consulo.ui.ex.action.Presentation;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Java peer for a native {@code NSMenuItem}.
 * <p>
 * Each instance owns a retained native peer ({@link #nativePeer}) which must be released via {@link #dispose()}.
 * The {@code native} methods bind to {@code libmacscreenmenu*.dylib}.
 */
public class MenuItem implements Disposable, PropertyChangeListener {
    long nativePeer;
    Runnable actionDelegate;
    boolean isInHierarchy = false;
    Presentation presentation;
    String myTitle;

    public MenuItem() {
    }

    MenuItem(long nsMenuItem) {
        if (nsMenuItem != 0) {
            nativePeer = nativeAttach(nsMenuItem);
            isInHierarchy = true;
        }
    }

    public void setActionDelegate(Runnable actionDelegate) {
        this.actionDelegate = actionDelegate;
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
    }

    public void listenPresentationChanges(Presentation newPresentation) {
        if (presentation != null) {
            presentation.removePropertyChangeListener(this);
        }
        newPresentation.addPropertyChangeListener(this);
        setEnabled(newPresentation.isEnabled());
        presentation = newPresentation;
    }

    public void setSubmenu(Menu subMenu, boolean onAppKit) {
        ensureNativePeer();
        subMenu.ensureNativePeer();
        nativeSetSubmenu(nativePeer, subMenu.nativePeer, isInHierarchy || subMenu.isInHierarchy);
    }

    public void setState(boolean isToggled) {
        ensureNativePeer();
        nativeSetState(nativePeer, isToggled, isInHierarchy);
    }

    public void setEnabled(boolean isEnabled) {
        ensureNativePeer();
        nativeSetEnabled(nativePeer, isEnabled, isInHierarchy);
    }

    public void setLabel(String label, @Nullable KeyStroke ks) {
        ensureNativePeer();

        char keyChar = ks == null ? 0 : ks.getKeyChar();
        int keyCode = ks == null ? 0 : ks.getKeyCode();
        int modifiers = ks == null ? 0 : ks.getModifiers();
        if (label == null) {
            label = "";
        }
        if (keyChar == KeyEvent.CHAR_UNDEFINED) {
            keyChar = 0;
        }

        myTitle = label;
        nativeSetTitleAndAccelerator(nativePeer, label, keyChar, keyCode, modifiers, isInHierarchy);
    }

    public void setLabel(String label) {
        ensureNativePeer();
        if (label == null) {
            label = "";
        }
        myTitle = label;
        nativeSetTitle(nativePeer, label, isInHierarchy);
    }

    public void setIcon(@Nullable Icon icon) {
        if (icon == null) {
            return;
        }

        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        try {
            icon.paintIcon(null, g, 0, 0);
        }
        finally {
            g.dispose();
        }

        int[] bytes = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        ensureNativePeer();
        nativeSetImage(nativePeer, bytes, w, h, image.getWidth(), image.getHeight(), isInHierarchy);
    }

    public void setAcceleratorText(String acceleratorText) {
        ensureNativePeer();
        nativeSetAcceleratorText(nativePeer, acceleratorText, isInHierarchy);
    }

    synchronized void ensureNativePeer() {
        if (nativePeer == 0) {
            nativePeer = nativeCreate(false);
        }
    }

    void handleAction(final int modifiers) {
        if (actionDelegate != null) {
            actionDelegate.run();
        }
    }

    @Override
    public synchronized void dispose() {
        if (presentation != null) {
            presentation.removePropertyChangeListener(this);
        }
        presentation = null;

        if (nativePeer != 0) {
            nativeDispose(nativePeer);
            nativePeer = 0;
        }
    }

    private native long nativeCreate(boolean isSeparator);

    private native long nativeAttach(long nsMenuItem);

    native void nativeDispose(long nativePeer);

    private native void nativeSetTitleAndAccelerator(long nativePeer, String label, char keyChar, int keyCode, int modifiers, boolean onAppKit);

    private native void nativeSetTitle(long nativePeer, String title, boolean onAppKit);

    private native void nativeSetImage(long nativePeer, int[] buffer, int pointsWidth, int pointsHeight, int pixelsWidth, int pixelsHeight, boolean onAppKit);

    private native void nativeSetEnabled(long nativePeer, boolean isEnabled, boolean onAppKit);

    private native void nativeSetAcceleratorText(long nativePeer, String acceleratorText, boolean onAppKit);

    private native void nativeSetState(long nativePeer, boolean isToggled, boolean onAppKit);

    private native void nativeSetSubmenu(long nativePeer, long submenu, boolean onAppKit);
}
