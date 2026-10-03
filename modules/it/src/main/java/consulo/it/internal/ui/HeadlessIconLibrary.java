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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.image.IconLibrary;

/**
 * @author VISTALL
 */
public final class HeadlessIconLibrary implements IconLibrary {
    private final String myId;
    private final boolean myDark;

    public HeadlessIconLibrary(String id, boolean dark) {
        myId = id;
        myDark = dark;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.of(myId);
    }

    @Override
    public boolean isDark() {
        return myDark;
    }

    @Override
    public String toString() {
        return myId;
    }
}
