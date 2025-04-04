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
package consulo.desktop.awt.ui.plaf2;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2024-11-27
 */
@TopicImpl(ComponentScope.APPLICATION)
public class SmoothScrollingListener implements UISettingsListener {
    @Override
    public void uiSettingsChanged(UISettings source) {
        set(source);
    }

    public static void set(UISettings settings) {
        UIManager.put("ScrollPane.smoothScrolling", settings.SMOOTH_SCROLLING);
    }
}
