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
package consulo.ide.impl.idea.find.actions;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.action.CustomComponentAction;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2024-12-06
 */
public class ProgressIconAsAction extends DumbAwareAction implements CustomComponentAction {
    private final JComponent myComponent;

    public ProgressIconAsAction(JComponent component) {
        myComponent = component;
    }

    @Nonnull
    @Override
    public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
        return myComponent;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
    }
}
