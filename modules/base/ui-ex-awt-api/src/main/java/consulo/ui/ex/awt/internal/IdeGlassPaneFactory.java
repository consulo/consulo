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
package consulo.ui.ex.awt.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import javax.swing.JComponent;
import javax.swing.JRootPane;

/**
 * Builds the ide glass pane for a root pane - the layer the platform draws drag images and paint hints onto.
 * The implementation lives in the ide layer, the ui layer only needs to install one on the windows it creates.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface IdeGlassPaneFactory {
    static IdeGlassPaneFactory getInstance() {
        return Application.get().getInstance(IdeGlassPaneFactory.class);
    }

    JComponent create(JRootPane rootPane, boolean installPainters);
}
