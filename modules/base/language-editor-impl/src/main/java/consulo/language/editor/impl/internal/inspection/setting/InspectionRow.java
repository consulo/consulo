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
import consulo.ui.image.Image;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public record InspectionRow(
    String shortName,
    LocalizeValue text,
    List<LocalizeValue> groupPath,
    ThreeState enabled,
    @Nullable Image severityIcon,
    boolean modified,
    boolean batchOnly
) {
    public InspectionLeafNode toNode() {
        return new InspectionLeafNode(shortName, text);
    }
}
