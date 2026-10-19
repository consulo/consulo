/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.layout;

import consulo.ui.ComponentOptions;

/**
 * @author VISTALL
 * @since 2021-10-30
 */
public final class ScrollableLayoutOptions extends ComponentOptions {
    public static class Builder extends ComponentOptionsBuilder<Builder> {
        private ScrollPolicy myHorizontalScrollPolicy = ScrollPolicy.IF_NEEDED;
        private ScrollPolicy myVerticalScrollPolicy = ScrollPolicy.IF_NEEDED;
        private VerticalScrollBarPosition myVerticalScrollBarPosition = VerticalScrollBarPosition.RIGHT;

        public Builder horizontalScrollPolicy(ScrollPolicy horizontalScrollPolicy) {
            myHorizontalScrollPolicy = horizontalScrollPolicy;
            return this;
        }

        public Builder verticalScrollPolicy(ScrollPolicy verticalScrollPolicy) {
            myVerticalScrollPolicy = verticalScrollPolicy;
            return this;
        }

        public Builder verticalScrollBarPosition(VerticalScrollBarPosition verticalScrollBarPosition) {
            myVerticalScrollBarPosition = verticalScrollBarPosition;
            return this;
        }

        @Override
        public ScrollableLayoutOptions build() {
            return new ScrollableLayoutOptions(myBackgroundPaint, myHorizontalScrollPolicy, myVerticalScrollPolicy, myVerticalScrollBarPosition);
        }
    }

    public enum ScrollPolicy {
        ALWAYS,
        IF_NEEDED,
        NEVER
    }

    public static Builder builder() {
        return new Builder();
    }

    private final ScrollPolicy myHorizontalScrollPolicy;
    private final ScrollPolicy myVerticalScrollPolicy;
    private final VerticalScrollBarPosition myVerticalScrollBarPosition;

    private ScrollableLayoutOptions(
        boolean backgroundPaint,
        ScrollPolicy horizontalScrollPolicy,
        ScrollPolicy verticalScrollPolicy,
        VerticalScrollBarPosition verticalScrollBarPosition
    ) {
        super(backgroundPaint);
        myHorizontalScrollPolicy = horizontalScrollPolicy;
        myVerticalScrollPolicy = verticalScrollPolicy;
        myVerticalScrollBarPosition = verticalScrollBarPosition;
    }

    public ScrollPolicy getHorizontalScrollPolicy() {
        return myHorizontalScrollPolicy;
    }

    public ScrollPolicy getVerticalScrollPolicy() {
        return myVerticalScrollPolicy;
    }

    public VerticalScrollBarPosition getVerticalScrollBarPosition() {
        return myVerticalScrollBarPosition;
    }
}
