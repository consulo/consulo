// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.stickyLine;

import consulo.codeEditor.EditorEx;
import consulo.ui.ex.awt.JBLayeredPane;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StickyLineComponents {
    private final EditorEx editor;
    private final JBLayeredPane layeredPane;
    private final List<StickyLineComponent> components = new ArrayList<>();

    public StickyLineComponents(EditorEx editor, JBLayeredPane layeredPane) {
        this.editor = editor;
        this.layeredPane = layeredPane;
    }

    public Iterable<StickyLineComponent> components() {
        List<StickyLineComponent> result = new ArrayList<>();
        for (StickyLineComponent component : components) {
            if (!component.isEmpty()) {
                result.add(component);
            }
        }
        return result;
    }

    public Iterable<StickyLineComponent> unboundComponents() {
        return () -> new Iterator<StickyLineComponent>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public StickyLineComponent next() {
                StickyLineComponent component;
                if (index < components.size()) {
                    component = components.get(index);
                }
                else {
                    component = new StickyLineComponent(editor);
                    layeredPane.add(component, Integer.valueOf(200 - components.size()));
                    components.add(component);
                }
                index++;
                return component;
            }
        };
    }

    public void resetAfterIndex(int index) {
        for (int i = index; i < components.size(); i++) {
            StickyLineComponent component = components.get(i);
            component.resetLine();
            component.setVisible(false);
        }
    }

    public boolean clear() {
        if (components.isEmpty()) {
            return false;
        }
        components.clear();
        layeredPane.removeAll();
        return true;
    }

    public int size() {
        int i = 0;
        for (StickyLineComponent component : components) {
            if (component.isVisible()) {
                i++;
            }
        }
        return i;
    }
}
