// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import javax.swing.*;
import java.awt.event.MouseEvent;

public enum MouseButton {
    Left,
    Middle,
    Right;

    public static MouseButton fromEvent(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            return Left;
        }
        else if (SwingUtilities.isMiddleMouseButton(e)) {
            return Middle;
        }
        else if (SwingUtilities.isRightMouseButton(e)) {
            return Right;
        }
        else {
            return null;
        }
    }

    public static MouseButton getMouseButton(MouseEvent e) {
        return fromEvent(e);
    }
}
