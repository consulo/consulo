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

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindowInternalDecorator;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import consulo.ui.ex.toolWindow.WindowInfo;
import org.jspecify.annotations.Nullable;

/**
 * Headless {@link ToolWindowStripeButton}: never rendered, it only exists so that
 * {@code ToolWindowManagerBase.registerToolWindow} gets the non-null button it registers and disposes.
 *
 * @author VISTALL
 */
public class HeadlessToolWindowStripeButton implements ToolWindowStripeButton {
    private final ToolWindowInternalDecorator myDecorator;

    public HeadlessToolWindowStripeButton(ToolWindowInternalDecorator decorator) {
        myDecorator = decorator;
    }

    @Override
    public WindowInfo getWindowInfo() {
        return myDecorator.getWindowInfo();
    }

    @Override
    public void apply(WindowInfo windowInfo) {
    }

    @Override
    @RequiredUIAccess
    public void updatePresentation() {
    }

    @Override
    public @Nullable Component getComponent() {
        return null;
    }

    @Override
    public void dispose() {
    }
}
