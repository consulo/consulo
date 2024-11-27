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
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.project.ui.wm.IdeRootPaneNorthExtension;
import consulo.ui.ex.awt.TitlelessDecorator;

/**
 * @author VISTALL
 * @since 2024-11-27
 */
public interface IdeRootPaneNorthExtensionWithDecorator extends IdeRootPaneNorthExtension {
    void setTitlelessDecorator(TitlelessDecorator titlelessDecorator);
}
