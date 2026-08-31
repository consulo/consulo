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
 * A size in both directions, each a {@link Length}, so a window asked for in fonts comes out as big as the text it
 * holds on whichever frontend draws it - the same dialog is wider in the browser than on the desktop.
 *
 * @author VISTALL
 * @since 2026-08-24
 */
public record WidthAndHeight(Length width, Length height) {
    public static WidthAndHeight of(Length width, Length height) {
        return new WidthAndHeight(width, height);
    }

    public static WidthAndHeight ofFont(float width, float height) {
        return new WidthAndHeight(Length.ofFont(width), Length.ofFont(height));
    }

    public static WidthAndHeight ofPixel(int width, int height) {
        return new WidthAndHeight(Length.ofPixel(width), Length.ofPixel(height));
    }
}
