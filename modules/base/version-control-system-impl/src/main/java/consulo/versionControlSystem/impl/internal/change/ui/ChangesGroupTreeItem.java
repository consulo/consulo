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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.localize.LocalizeValue;

/**
 * A titled group of changes added under a change list by a {@link consulo.versionControlSystem.change.ChangesViewModifier}.
 * Its changes are not part of the change list.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
public record ChangesGroupTreeItem(ChangeListTreeItem changeList, LocalizeValue title) implements ChangesTreeItem {
}
