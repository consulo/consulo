// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inlay;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for implementing {@link InlayPresentation}, handling listener registration and event firing.
 */
public abstract class BasePresentation implements InlayPresentation {
    private final Set<PresentationListener> listeners = new HashSet<>();

    @Override
    public void fireSizeChanged(Dimension previous, Dimension current) {
        for (PresentationListener listener : listeners) {
            listener.sizeChanged(previous, current);
        }
    }

    @Override
    public void fireContentChanged(Rectangle area) {
        for (PresentationListener listener : listeners) {
            listener.contentChanged(area);
        }
    }

    @Override
    public void addListener(PresentationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(PresentationListener listener) {
        listeners.remove(listener);
    }
}
