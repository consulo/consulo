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
package consulo.versionControlSystem.impl.internal.change.ui.awt;

import consulo.ui.ex.SimpleTextAttributes;

/**
 * @author UNV
 * @since 2026-09-27
 */
public class ChangesBrowserChangesNode extends ChangesBrowserNode<Object> {
    public ChangesBrowserChangesNode() {
        super(CHANGES_TAG);
        setAttributes(SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }

    @Override
    public int getSortWeight() {
        return CHANGE_LIST_SORT_WEIGHT;
    }
}
