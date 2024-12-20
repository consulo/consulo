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
package consulo.desktop.swt.ui.impl;

import consulo.ui.color.ColorValue;
import consulo.ui.impl.style.StyleImpl;
import consulo.ui.style.StyleColorValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtStyleImpl extends StyleImpl {
    private final String myId;
    private final String myName;

    public DesktopSwtStyleImpl(String id, String name) {
        myId = id;
        myName = name;
    }

    @Nonnull
    @Override
    public String getId() {
        return myId;
    }

    @Nonnull
    @Override
    public String getName() {
        return myName;
    }

    @Nonnull
    @Override
    public ColorValue getColorValue(@Nonnull StyleColorValue colorKey) {
        return colorKey.toRGB();
    }
}
