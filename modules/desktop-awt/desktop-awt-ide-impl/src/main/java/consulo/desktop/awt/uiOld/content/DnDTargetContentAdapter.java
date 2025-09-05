/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.uiOld.content;

import consulo.ide.impl.idea.ui.content.impl.ContentImpl;
import consulo.ui.ex.awt.dnd.DnDEvent;
import consulo.ui.ex.awt.dnd.DnDTarget;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DnDTargetContentAdapter extends ContentImpl implements DnDTarget {
    private final Consumer<DnDEvent> myDropHandler;
    private final Predicate<DnDEvent> myUpdateHandler;

    protected DnDTargetContentAdapter(JComponent component,
                                      String displayName,
                                      boolean isLockable,
                                      Consumer<DnDEvent> dropHandler,
                                      Predicate<DnDEvent> updateHandler) {
        super(component, displayName, isLockable);
        myDropHandler = dropHandler;
        myUpdateHandler = updateHandler;
    }

    @Override
    public boolean update(DnDEvent event) {
        return myUpdateHandler.test(event);
    }

    @Override
    public void drop(DnDEvent event) {
        myDropHandler.accept(event);
    }

    @Override
    public void cleanUpOnLeave() {
    }

    @Override
    public void updateDraggedImage(Image image, Point dropPoint, Point imageOffset) {
    }
}