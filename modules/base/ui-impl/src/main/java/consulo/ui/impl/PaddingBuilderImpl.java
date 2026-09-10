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

import consulo.ui.PaddingBuilder;
import consulo.ui.Space;
import consulo.ui.internal.BorderPosition;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-09-03
 */
public class PaddingBuilderImpl implements PaddingBuilder {
    public static final PaddingBuilder NOOP = new NoOp();

    private final UIDataObject myDataObject;
    private final Runnable myOnApply;
    private final Map<BorderPosition, Space> mySet = new EnumMap<>(BorderPosition.class);
    private final Set<BorderPosition> myReset = EnumSet.noneOf(BorderPosition.class);

    public PaddingBuilderImpl(UIDataObject dataObject, Runnable onApply) {
        myDataObject = dataObject;
        myOnApply = onApply;
    }

    private PaddingBuilder set(BorderPosition position, Space space) {
        if (space == Space.NONE) {
            return reset(position);
        }

        mySet.put(position, space);
        myReset.remove(position);
        return this;
    }

    private PaddingBuilder reset(BorderPosition position) {
        mySet.remove(position);
        myReset.add(position);
        return this;
    }

    private PaddingBuilder reuse(BorderPosition position) {
        mySet.remove(position);
        myReset.remove(position);
        return this;
    }

    @Override
    public PaddingBuilder topSet(Space space) {
        return set(BorderPosition.TOP, space);
    }

    @Override
    public PaddingBuilder topReset() {
        return reset(BorderPosition.TOP);
    }

    @Override
    public PaddingBuilder topReuse() {
        return reuse(BorderPosition.TOP);
    }

    @Override
    public PaddingBuilder bottomSet(Space space) {
        return set(BorderPosition.BOTTOM, space);
    }

    @Override
    public PaddingBuilder bottomReset() {
        return reset(BorderPosition.BOTTOM);
    }

    @Override
    public PaddingBuilder bottomReuse() {
        return reuse(BorderPosition.BOTTOM);
    }

    @Override
    public PaddingBuilder leftSet(Space space) {
        return set(BorderPosition.LEFT, space);
    }

    @Override
    public PaddingBuilder leftReset() {
        return reset(BorderPosition.LEFT);
    }

    @Override
    public PaddingBuilder leftReuse() {
        return reuse(BorderPosition.LEFT);
    }

    @Override
    public PaddingBuilder rightSet(Space space) {
        return set(BorderPosition.RIGHT, space);
    }

    @Override
    public PaddingBuilder rightReset() {
        return reset(BorderPosition.RIGHT);
    }

    @Override
    public PaddingBuilder rightReuse() {
        return reuse(BorderPosition.RIGHT);
    }

    @Override
    public PaddingBuilder verticalSet(Space space) {
        set(BorderPosition.TOP, space);
        set(BorderPosition.BOTTOM, space);
        return this;
    }

    @Override
    public PaddingBuilder horizontalSet(Space space) {
        set(BorderPosition.LEFT, space);
        set(BorderPosition.RIGHT, space);
        return this;
    }

    @Override
    public PaddingBuilder allSet(Space space) {
        for (BorderPosition position : BorderPosition.values()) {
            set(position, space);
        }
        return this;
    }

    @Override
    public PaddingBuilder allReset() {
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

        myDataObject.applyPaddings(mySet, myReset);
        myOnApply.run();
    }

    private static class NoOp implements PaddingBuilder {
        @Override
        public PaddingBuilder topSet(Space space) {
            return this;
        }

        @Override
        public PaddingBuilder topReset() {
            return this;
        }

        @Override
        public PaddingBuilder topReuse() {
            return this;
        }

        @Override
        public PaddingBuilder bottomSet(Space space) {
            return this;
        }

        @Override
        public PaddingBuilder bottomReset() {
            return this;
        }

        @Override
        public PaddingBuilder bottomReuse() {
            return this;
        }

        @Override
        public PaddingBuilder leftSet(Space space) {
            return this;
        }

        @Override
        public PaddingBuilder leftReset() {
            return this;
        }

        @Override
        public PaddingBuilder leftReuse() {
            return this;
        }

        @Override
        public PaddingBuilder rightSet(Space space) {
            return this;
        }

        @Override
        public PaddingBuilder rightReset() {
            return this;
        }

        @Override
        public PaddingBuilder rightReuse() {
            return this;
        }

        @Override
        public PaddingBuilder verticalSet(Space space) {
            return this;
        }

        @Override
        public PaddingBuilder horizontalSet(Space space) {
            return this;
        }

        @Override
        public PaddingBuilder allSet(Space space) {
            return this;
        }

        @Override
        public PaddingBuilder allReset() {
            return this;
        }

        @Override
        public void apply() {
        }
    }
}
