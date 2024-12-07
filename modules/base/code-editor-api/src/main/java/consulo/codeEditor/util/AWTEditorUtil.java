/*
 * Copyright 2013-2024 consulo.io
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
package consulo.codeEditor.util;

import consulo.codeEditor.Editor;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 2024-12-06
 */
public final class AWTEditorUtil {
    private AWTEditorUtil() {
    }


    public static int yPositionToLogicalLine(@Nonnull Editor editor, @Nonnull MouseEvent event) {
        return EditorUtil.yPositionToLogicalLine(editor, event.getY());
    }

    public static int yPositionToLogicalLine(@Nonnull Editor editor, @Nonnull Point point) {
        return EditorUtil.yPositionToLogicalLine(editor, point.y);
    }
}
