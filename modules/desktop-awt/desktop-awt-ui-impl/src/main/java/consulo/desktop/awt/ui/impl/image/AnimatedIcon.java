// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.ui.impl.image;

import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.ComponentUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Sergey.Malenkov
 */
abstract class AnimatedIcon implements Icon {
    private static final Logger LOG = Logger.getInstance(AnimatedIcon.class);

    interface Frame {
        Icon getIcon();

        int getDelay();
    }

    private final Frame[] myFrames;
    private final Set<Component> myRequested = Collections.newSetFromMap(new IdentityHashMap<>());
    private long myTime;
    private int myIndex;

    protected AnimatedIcon(Function<AnimatedIcon, Frame[]> frameProducer) {
        myFrames = frameProducer.apply(this);
        myTime = System.currentTimeMillis();
    }

    private void updateFrameAt(long current) {
        int next = myIndex + 1;
        myIndex = next < getFrames().length ? next : 0;
        myTime = current;
    }

    protected Frame[] getFrames() {
        return myFrames;
    }

    private Icon getUpdatedIcon() {
        int index = getCurrentIndex();
        return getFrames()[index].getIcon();
    }

    private int getCurrentIndex() {
        long current = System.currentTimeMillis();
        Frame[] frames = getFrames();
        if (myIndex >= frames.length) {
            myIndex = 0;
        }
        Frame frame = frames[myIndex];
        if (frame.getDelay() <= (current - myTime)) {
            updateFrameAt(current);
        }
        return myIndex;
    }

    @RequiredUIAccess
    private void requestRefresh(@Nullable Component c) {
        if (c == null || myRequested.contains(c) || !canRefresh(c)) {
            return;
        }

        Frame frame = getFrames()[myIndex];
        int delay = frame.getDelay();
        if (delay > 0) {
            myRequested.add(c);
            UIAccess.current().getScheduler().schedule(() -> {
                myRequested.remove(c);
                if (canRefresh(c)) {
                    doRefresh(c);
                }
            }, delay, MILLISECONDS);
        }
        else {
            doRefresh(c);
        }
    }

    @Override
    public final void paintIcon(Component c, Graphics g, int x, int y) {
        Icon icon = getUpdatedIcon();
        if (EventQueue.isDispatchThread()) {
            CellRendererPane pane = ComponentUtil.getParentOfType(CellRendererPane.class, c);
            requestRefresh(pane == null ? c : getRendererOwner(pane.getParent()));
        }
        else if (LOG.isDebugEnabled()) {
            LOG.debug(new IllegalStateException("Unexpected thread " + Thread.currentThread().getName()));
        }
        icon.paintIcon(c, g, x, y);
    }

    @Override
    public final int getIconWidth() {
        return getUpdatedIcon().getIconWidth();
    }

    @Override
    public final int getIconHeight() {
        return getUpdatedIcon().getIconHeight();
    }

    protected boolean canRefresh(Component component) {
        return component.isShowing();
    }

    protected void doRefresh(Component component) {
        component.repaint();
    }

    protected abstract @Nullable Component getRendererOwner(@Nullable Component component);
}
