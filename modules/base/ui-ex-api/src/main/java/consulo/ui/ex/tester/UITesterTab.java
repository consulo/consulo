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
package consulo.ui.ex.tester;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * A tab of the UI tester, a dialog for trying UI components by hand.
 * Tabs are shown in the order of their extensions.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface UITesterTab {
    /**
     * @return the title of the tab
     */
    LocalizeValue getName();

    /**
     * Builds the content of the tab, once per opened tester.
     *
     * @param uiDisposable disposed when the tester closes
     * @return the content of the tab
     */
    @RequiredUIAccess
    Component createComponent(Disposable uiDisposable);

    /**
     * @return whether the tab can be closed
     */
    default boolean isClosable() {
        return false;
    }
}
