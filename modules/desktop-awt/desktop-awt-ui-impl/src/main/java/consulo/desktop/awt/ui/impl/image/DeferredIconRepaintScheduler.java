// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.impl.image;

import consulo.desktop.awt.ui.impl.components.PaintingParent;
import consulo.desktop.awt.ui.impl.tabs.TabLabel;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.Alarm;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DeferredIconRepaintScheduler {
    private record RepaintSchedulerRequest(Component component, @Nullable Rectangle rectangle) {
    }

    private final Alarm myAlarm = new Alarm();
    private final Set<RepaintSchedulerRequest> myQueue = new LinkedHashSet<>();

    @RequiredUIAccess
    public RepaintRequest createRepaintRequest(@Nullable Component component, int x, int y) {
        Component target = getTarget(component);
        Component paintingParent = SwingUtilities.getAncestorOfClass(PaintingParent.class, component);
        Rectangle paintingParentRec = paintingParent == null || component == null ? null : ((PaintingParent) paintingParent).getChildRec(component);
        return new RepaintRequest(component, x, y, target, paintingParent, paintingParentRec);
    }

    @RequiredUIAccess
    public void scheduleRepaint(RepaintRequest request, int iconWidth, int iconHeight, boolean alwaysSchedule) {
        Component actualTarget = request.getActualTarget();
        if (actualTarget == null) {
            return;
        }
        Component component = request.component();
        if (!alwaysSchedule && component == actualTarget) {
            component.repaint(request.x(), request.y(), iconWidth, iconHeight);
        }
        else {
            UIAccess.assertIsUIThread();
            myAlarm.cancelAllRequests();
            myQueue.add(new RepaintSchedulerRequest(actualTarget, request.paintingParentRec()));
            myAlarm.addRequest(this::flush, 50);
        }
    }

    private void flush() {
        List<RepaintSchedulerRequest> requests = new ArrayList<>(myQueue);
        myQueue.clear();
        for (RepaintSchedulerRequest request : requests) {
            Rectangle rectangle = request.rectangle();
            if (rectangle == null) {
                request.component().repaint();
            }
            else {
                request.component().repaint(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            }
        }
    }

    private static @Nullable Component getTarget(@Nullable Component c) {
        Component target = SwingUtilities.getAncestorOfClass(JList.class, c);
        if (target == null) {
            // check table first to process TreeTable correctly
            target = SwingUtilities.getAncestorOfClass(JTable.class, c);
        }
        if (target == null) {
            target = SwingUtilities.getAncestorOfClass(JTree.class, c);
        }
        if (target == null) {
            target = SwingUtilities.getAncestorOfClass(JComboBox.class, c);
        }
        if (target == null) {
            target = SwingUtilities.getAncestorOfClass(TabLabel.class, c);
        }
        return target == null ? c : target;
    }
}
