/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.progress.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindow;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ProgressDialogFactory {
    static ProgressDialogFactory getInstance() {
        return ServiceManager.getService(ProgressDialogFactory.class);
    }

    @Nonnull
    ProgressDialog create(ProgressWindow progressWindow,
                          boolean shouldShowBackground,
                          JComponent parent,
                          Project project,
                          @Nonnull LocalizeValue cancelText);
}
