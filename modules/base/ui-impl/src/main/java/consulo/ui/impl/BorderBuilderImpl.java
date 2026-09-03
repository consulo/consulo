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
package consulo.ui.impl;

import consulo.ui.BorderBuilder;
import consulo.ui.color.ColorValue;
import consulo.ui.internal.BorderPosition;
import consulo.ui.style.ComponentColors;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-09-03
 */
public class BorderBuilderImpl implements BorderBuilder {
    public static final BorderBuilder NOOP = new NoOp();

    private final UIDataObject myDataObject;
    private final Runnable myOnApply;
    private final Map<BorderPosition, ColorValue> mySet = new EnumMap<>(BorderPosition.class);
    private final Set<BorderPosition> myReset = EnumSet.noneOf(BorderPosition.class);

    public BorderBuilderImpl(UIDataObject dataObject, Runnable onApply) {
        myDataObject = dataObject;
        myOnApply = onApply;
    }

    private BorderBuilder set(BorderPosition position, ColorValue color) {
        mySet.put(position, color);
        myReset.remove(position);
        return this;
    }

    private BorderBuilder reset(BorderPosition position) {
        mySet.remove(position);
        myReset.add(position);
        return this;
    }

    private BorderBuilder reuse(BorderPosition position) {
        mySet.remove(position);
        myReset.remove(position);
        return this;
    }

    @Override
    public BorderBuilder topSet() {
        return topSet(ComponentColors.BORDER);
    }

    @Override
    public BorderBuilder topSet(ColorValue color) {
        return set(BorderPosition.TOP, color);
    }

    @Override
    public BorderBuilder topReset() {
        return reset(BorderPosition.TOP);
    }

    @Override
    public BorderBuilder topReuse() {
        return reuse(BorderPosition.TOP);
    }

    @Override
    public BorderBuilder bottomSet() {
        return bottomSet(ComponentColors.BORDER);
    }

    @Override
    public BorderBuilder bottomSet(ColorValue color) {
        return set(BorderPosition.BOTTOM, color);
    }

    @Override
    public BorderBuilder bottomReset() {
        return reset(BorderPosition.BOTTOM);
    }

    @Override
    public BorderBuilder bottomReuse() {
        return reuse(BorderPosition.BOTTOM);
    }

    @Override
    public BorderBuilder leftSet() {
        return leftSet(ComponentColors.BORDER);
    }

    @Override
    public BorderBuilder leftSet(ColorValue color) {
        return set(BorderPosition.LEFT, color);
    }

    @Override
    public BorderBuilder leftReset() {
        return reset(BorderPosition.LEFT);
    }

    @Override
    public BorderBuilder leftReuse() {
        return reuse(BorderPosition.LEFT);
    }

    @Override
    public BorderBuilder rightSet() {
        return rightSet(ComponentColors.BORDER);
    }

    @Override
    public BorderBuilder rightSet(ColorValue color) {
        return set(BorderPosition.RIGHT, color);
    }

    @Override
    public BorderBuilder rightReset() {
        return reset(BorderPosition.RIGHT);
    }

    @Override
    public BorderBuilder rightReuse() {
        return reuse(BorderPosition.RIGHT);
    }

    @Override
    public BorderBuilder allSet() {
        return allSet(ComponentColors.BORDER);
    }

    @Override
    public BorderBuilder allSet(ColorValue color) {
        for (BorderPosition position : BorderPosition.values()) {
            set(position, color);
        }
        return this;
    }

    @Override
    public BorderBuilder allReset() {
        for (BorderPosition position : BorderPosition.values()) {
            reset(position);
        }
        return this;
    }

    @Override
    public void apply() {
        if (mySet.isEmpty() && myReset.isEmpty()) {
            return;
        }

        myDataObject.applyBorders(mySet, myReset);
        myOnApply.run();
    }

    private static class NoOp implements BorderBuilder {
        @Override
        public BorderBuilder topSet() {
            return this;
        }

        @Override
        public BorderBuilder topSet(ColorValue color) {
            return this;
        }

        @Override
        public BorderBuilder topReset() {
            return this;
        }

        @Override
        public BorderBuilder topReuse() {
            return this;
        }

        @Override
        public BorderBuilder bottomSet() {
            return this;
        }

        @Override
        public BorderBuilder bottomSet(ColorValue color) {
            return this;
        }

        @Override
        public BorderBuilder bottomReset() {
            return this;
        }

        @Override
        public BorderBuilder bottomReuse() {
            return this;
        }

        @Override
        public BorderBuilder leftSet() {
            return this;
        }

        @Override
        public BorderBuilder leftSet(ColorValue color) {
            return this;
        }

        @Override
        public BorderBuilder leftReset() {
            return this;
        }

        @Override
        public BorderBuilder leftReuse() {
            return this;
        }

        @Override
        public BorderBuilder rightSet() {
            return this;
        }

        @Override
        public BorderBuilder rightSet(ColorValue color) {
            return this;
        }

        @Override
        public BorderBuilder rightReset() {
            return this;
        }

        @Override
        public BorderBuilder rightReuse() {
            return this;
        }

        @Override
        public BorderBuilder allSet() {
            return this;
        }

        @Override
        public BorderBuilder allSet(ColorValue color) {
            return this;
        }

        @Override
        public BorderBuilder allReset() {
            return this;
        }

        @Override
        public void apply() {
        }
    }
}
