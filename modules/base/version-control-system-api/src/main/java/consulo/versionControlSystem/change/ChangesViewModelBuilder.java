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

import consulo.localize.LocalizeValue;

import java.util.Collection;

/**
 * The Local Changes tree under construction, as seen by a {@link ChangesViewModifier}.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
public interface ChangesViewModelBuilder {
    /**
     * Adds a titled group of changes as the last child of the node of {@code changeList}. The changes are shown and can
     * be selected (diff, VCS actions), but they are not part of the change list: selecting the change list node does not
     * select them. Does nothing when the list is not shown or {@code changes} is empty.
     */
    void insertChangesGroup(ChangeList changeList, LocalizeValue title, Collection<? extends Change> changes);
}
