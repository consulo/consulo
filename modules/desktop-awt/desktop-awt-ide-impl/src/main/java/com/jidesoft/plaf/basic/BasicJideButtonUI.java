/*
 * Copyright 2013-2024 consulo.io
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
package com.jidesoft.plaf.basic;

import java.awt.*;

public class BasicJideButtonUI {
    /**
     * Checks if we should wrap text on a button. If the vertical text position is bottom and horizontal text position
     * is center, we will wrap the text.
     *
     * @param c the component
     * @return true or false.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static boolean shouldWrapText(Component c) {
        // return false for now before we support the text wrapping
        return false;

//        boolean wrapText = false;
//        if (c instanceof AbstractButton) {
//            if (((AbstractButton) c).getVerticalTextPosition() == SwingConstants.BOTTOM && ((AbstractButton) c).getHorizontalTextPosition() == SwingConstants.CENTER) {
//                wrapText = true;
//            }
//        }
//        return wrapText;
    }
}
