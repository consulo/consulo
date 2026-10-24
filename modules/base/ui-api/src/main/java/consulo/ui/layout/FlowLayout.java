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
package consulo.ui.layout;

import consulo.ui.Component;
import consulo.ui.HasTransferHandler;
import consulo.ui.PseudoComponent;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

/**
 * Places its children side by side at their own width, in the order they were added, and wraps them into a new row when
 * the width runs out. Every child of a row is as tall as the tallest of them.
 * <p>
 * Given a {@link consulo.ui.DragAndDropTransferHandler}, its children can be dragged. A drag carries the dragged child as
 * its only item, and a drop lands before ({@link consulo.ui.DragAndDropTransferHandler.DropPosition#ABOVE}) or after
 * ({@link consulo.ui.DragAndDropTransferHandler.DropPosition#BELOW}) the child it is made on; a drop past the last child
 * lands after it.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public interface FlowLayout extends Layout<LayoutConstraint>, HasTransferHandler<Component> {
    static FlowLayout create() {
        return create(Space.MEDIUM);
    }

    static FlowLayout create(Space gap) {
        return UIInternal.get()._Layouts_flow(gap);
    }

    @RequiredUIAccess
    default FlowLayout add(PseudoComponent component) {
        return add(component.getComponent());
    }

    @RequiredUIAccess
    default FlowLayout add(Component component) {
        return (FlowLayout) add(component, LayoutConstraint.NONE);
    }
}
