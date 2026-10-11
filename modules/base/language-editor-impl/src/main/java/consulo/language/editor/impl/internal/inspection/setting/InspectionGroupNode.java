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
package consulo.language.editor.impl.internal.inspection.setting;

import consulo.localize.LocalizeValue;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public record InspectionGroupNode(List<LocalizeValue> path) implements InspectionTreeNode {
    @Override
    public LocalizeValue getText() {
        return path.get(path.size() - 1);
    }

    public boolean contains(InspectionRow row) {
        List<LocalizeValue> groupPath = row.groupPath();
        return groupPath.size() >= path.size() && groupPath.subList(0, path.size()).equals(path);
    }
}
