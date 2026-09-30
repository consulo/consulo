/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.diagram.settings;

import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * @author Konstantin Bulenkov
 */
public enum DiagramLayout {
    BALLOON,
    CIRCULAR,
    HIERARCHIC_GROUP,
    ORGANIC,
    ORTHOGONAL,
    DIRECTED_ORTHOGONAL;

    public static DiagramLayout getDefault() {
        return HIERARCHIC_GROUP;
    }

    public String getPresentableName() {
        return StringUtil.capitalizeWords(name().toLowerCase(Locale.ROOT).replace('_', ' '), true);
    }

    public static DiagramLayout fromString(@Nullable Object obj) {
        if (obj == null) {
            return getDefault();
        }
        try {
            return valueOf(obj.toString().toUpperCase(Locale.ROOT).replace(' ', '_'));
        }
        catch (IllegalArgumentException e) {
            return getDefault();
        }
    }
}
