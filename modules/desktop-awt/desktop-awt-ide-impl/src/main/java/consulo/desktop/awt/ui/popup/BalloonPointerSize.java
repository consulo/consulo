/*
 * Copyright 2013-2025 consulo.io
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
package consulo.desktop.awt.ui.popup;

/**
 * @author UNV
 * @since 2025-06-09
 */
public interface BalloonPointerSize {
    int getWidth();

    int getLength();

    static BalloonPointerSize of(int width, int length) {
        return new ConstantPointerSize(width, length);
    }

    static BalloonPointerSize cache(BalloonPointerSize pointerSize) {
        return new CachingPointerSize(pointerSize);
    }

    record ConstantPointerSize(int width, int length) implements BalloonPointerSize {
        @Override
        public int getWidth() {
            return width();
        }

        @Override
        public int getLength() {
            return length();
        }
    }

    static final class CachingPointerSize implements BalloonPointerSize {
        private final BalloonPointerSize myPointerSize;
        private int myWidth = -1;
        private int myLength = -1;

        private CachingPointerSize(BalloonPointerSize pointerSize) {
            myPointerSize = pointerSize;
        }

        @Override
        public int getWidth() {
            if (myWidth < 0) {
                myWidth = myPointerSize.getWidth();
            }
            return myWidth;
        }

        @Override
        public int getLength() {
            if (myLength < 0) {
                myLength = myPointerSize.getLength();
            }
            return myLength;
        }
    }
}
