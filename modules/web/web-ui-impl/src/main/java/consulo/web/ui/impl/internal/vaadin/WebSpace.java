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
package consulo.web.ui.impl.internal.vaadin;

import consulo.ui.Space;
import consulo.ui.internal.BorderPosition;

import java.util.List;
import java.util.Map;

/**
 * What a step of the {@link Space} scale is worth on this frontend - a class of the theme rather than a count of
 * anything, so what the browser draws stays a decision of the style sheet and can be restyled without a rebuild.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
public final class WebSpace {
    private static final Map<Space, String> ourSuffixes = Map.of(
        Space.NONE, "0",
        Space.X_SMALL, "2xs",
        Space.SMALL, "xs",
        Space.MEDIUM, "s",
        Space.LARGE, "m",
        Space.X_LARGE, "l",
        Space.XX_LARGE, "xl",
        Space.XXX_LARGE, "2xl"
    );

    public static String toGapClass(Space space) {
        return "gap-" + suffix(space);
    }

    public static String toPaddingClass(BorderPosition position, Space space) {
        return "p" + edge(position) + "-" + suffix(space);
    }

    /**
     * Every padding class this frontend can put on one edge, so the one in place can be taken off without the
     * component having to remember which step it was given.
     */
    public static List<String> allPaddingClasses(BorderPosition position) {
        return ourSuffixes.values().stream().map(suffix -> "p" + edge(position) + "-" + suffix).toList();
    }

    private static String suffix(Space space) {
        return ourSuffixes.getOrDefault(space, "0");
    }

    private static String edge(BorderPosition position) {
        return switch (position) {
            case TOP -> "t";
            case BOTTOM -> "b";
            case LEFT -> "l";
            case RIGHT -> "r";
        };
    }

    private WebSpace() {
    }
}
