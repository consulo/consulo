/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.internal.ui;

import com.vaadin.flow.component.page.ColorScheme;
import consulo.ui.impl.style.StyleImpl;

/**
 * @author VISTALL
 * @since 2017-09-15
 */
public class WebStyleImpl extends StyleImpl {
    private final String myId;
    private final String myName;
    private final boolean myIsDark;
    private final ColorScheme.Value myVaadinThemeId;

    public WebStyleImpl(String id, String name, boolean isDark, ColorScheme.Value value) {
        myId = id;
        myName = name;
        myIsDark = isDark;
        myVaadinThemeId = value;
    }

    public ColorScheme.Value getVaadinThemeId() {
        return myVaadinThemeId;
    }

    @Override
    public boolean isDark() {
        return myIsDark;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public String getName() {
        return myName;
    }
}
