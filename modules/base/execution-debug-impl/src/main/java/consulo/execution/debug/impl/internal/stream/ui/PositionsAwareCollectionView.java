// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.stream.ui;

import consulo.application.ApplicationManager;
import consulo.execution.debug.stream.ui.PaintingListener;
import consulo.execution.debug.stream.ui.ValuesPositionsListener;
import consulo.proxy.EventDispatcher;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class PositionsAwareCollectionView extends CollectionView {
    private final EventDispatcher<ValuesPositionsListener> myDispatcher = EventDispatcher.create(ValuesPositionsListener.class);
    private final List<ValueWithPositionImpl> values;

    public PositionsAwareCollectionView(@Nonnull CollectionTree tree, @Nonnull List<ValueWithPositionImpl> values) {
        super(tree);
        this.values = values;

        getInstancesTree().addPaintingListener(new PaintingListener() {
            @Override
            public void componentPainted() {
                updateValues();
            }
        });
    }

    public void addValuesPositionsListener(@Nonnull ValuesPositionsListener listener) {
        myDispatcher.addListener(listener);
    }

    private void updateValues() {
        boolean changed = false;
        Rectangle visibleRect = getInstancesTree().getVisibleRect();
        for (ValueWithPositionImpl value : values) {
            Rectangle rect = getInstancesTree().getRectByValue(value.getTraceElement());
            if (rect == null) {
                changed = invalidate(value, changed);
            }
            else {
                changed = set(value, changed, rect.y + rect.height / 2 - visibleRect.y,
                    visibleRect.intersects(rect), getInstancesTree().isHighlighted(value.getTraceElement()));
            }
        }

        if (changed) {
            ApplicationManager.getApplication().invokeLater(() -> myDispatcher.getMulticaster().valuesPositionsChanged());
        }
    }

    private boolean invalidate(ValueWithPositionImpl value, boolean modified) {
        if (modified) {
            value.setInvalid();
            return true;
        }
        else {
            return value.updateToInvalid();
        }
    }

    private boolean set(ValueWithPositionImpl value, boolean modified, int pos, boolean visible, boolean highlighted) {
        if (modified) {
            value.setProperties(pos, visible, highlighted);
            return true;
        }
        else {
            return value.updateProperties(pos, visible, highlighted);
        }
    }
}
