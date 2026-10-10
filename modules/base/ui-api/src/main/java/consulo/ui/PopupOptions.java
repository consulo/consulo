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
package consulo.ui;

/**
 * @author VISTALL
 * @since 2026-08-02
 */
public final class PopupOptions extends ComponentOptions {
    public static final class Builder {
        private boolean myCancelOnClickOutside = true;
        private boolean myCancelOnEscape = true;
        private boolean myRequestFocus = true;
        private boolean myResizable = false;
        private boolean myArrow = false;
        private PopupPosition myPosition = PopupPosition.BOTTOM;

        private Builder() {
        }

        public Builder position(PopupPosition position) {
            myPosition = position;
            return this;
        }

        /**
         * Keeps the popup up when the user works elsewhere, which a popup driving something under it needs.
         */
        public Builder disableCancelOnClickOutside() {
            myCancelOnClickOutside = false;
            return this;
        }

        public Builder disableCancelOnEscape() {
            myCancelOnEscape = false;
            return this;
        }

        public Builder disableRequestFocus() {
            myRequestFocus = false;
            return this;
        }

        public Builder resizable() {
            myResizable = true;
            return this;
        }

        /**
         * Draws a pointer from the popup to the point it was shown at, for a popup which describes that exact point.
         * A popup shown against a whole component has nothing to point at and is drawn without one.
         */
        public Builder arrow() {
            myArrow = true;
            return this;
        }

        public PopupOptions build() {
            return new PopupOptions(myCancelOnClickOutside, myCancelOnEscape, myRequestFocus, myResizable, myArrow, myPosition);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final boolean myCancelOnClickOutside;
    private final boolean myCancelOnEscape;
    private final boolean myRequestFocus;
    private final boolean myResizable;
    private final boolean myArrow;
    private final PopupPosition myPosition;

    private PopupOptions(
        boolean cancelOnClickOutside,
        boolean cancelOnEscape,
        boolean requestFocus,
        boolean resizable,
        boolean arrow,
        PopupPosition position
    ) {
        super(true);

        myCancelOnClickOutside = cancelOnClickOutside;
        myCancelOnEscape = cancelOnEscape;
        myRequestFocus = requestFocus;
        myResizable = resizable;
        myArrow = arrow;
        myPosition = position;
    }

    public PopupPosition getPosition() {
        return myPosition;
    }

    public boolean isCancelOnClickOutside() {
        return myCancelOnClickOutside;
    }

    public boolean isCancelOnEscape() {
        return myCancelOnEscape;
    }

    public boolean isRequestFocus() {
        return myRequestFocus;
    }

    public boolean isResizable() {
        return myResizable;
    }

    public boolean hasArrow() {
        return myArrow;
    }
}
