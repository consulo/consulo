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
package consulo.versionControlSystem.change;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

/**
 * Adds VCS-specific nodes to the Local Changes tree, e.g. the shelved changes of a change list kept on the server.
 * <p>
 * Called every time the tree is rebuilt, outside the UI thread: implementations must only read already known state
 * and never run VCS commands.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface ChangesViewModifier {
    void modifyTreeModelBuilder(ChangesViewModelBuilder builder);
}
