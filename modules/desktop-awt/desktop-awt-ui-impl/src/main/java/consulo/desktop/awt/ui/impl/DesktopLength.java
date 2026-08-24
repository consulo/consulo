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
package consulo.desktop.awt.ui.impl;

import consulo.ui.Length;
import consulo.ui.ex.awt.JBUI;

import java.util.List;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-24
 */
public final class DesktopLength {
    public static int toPixels(java.awt.Component component, Length length) {
        return length.accept(new Length.Visitor<Integer>() {
            @Override
            public Integer visitPixel(int pixels) {
                return JBUI.scale(pixels);
            }

            @Override
            public Integer visitFont(float fonts) {
                return Math.round(fonts * lineHeight(component));
            }

            @Override
            public Integer visitComposite(List<Length> parts) {
                int sum = 0;
                for (Length part : parts) {
                    sum += toPixels(component, part);
                }
                return sum;
            }
        });
    }

    public static int lineHeight(java.awt.Component component) {
        java.awt.Font font = component.getFont();
        return font == null ? JBUI.scale(16) : component.getFontMetrics(font).getHeight();
    }

    private DesktopLength() {
    }
}
