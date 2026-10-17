/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.project.ui.wm;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * A widget which shows a component of its own instead of a {@link StatusBarWidget.WidgetPresentation}. It is meant for
 * content a button cannot show, such as a gauge: a text, an icon, a click or a popup is a presentation, never a custom
 * widget built from labels. The status bar adds the padding and the context menu around the component.
 *
 * @author spLeaner
 */
public interface CustomStatusBarWidget extends StatusBarWidget {
    @RequiredUIAccess
    Component getUIComponent();
}
