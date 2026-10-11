/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions;

import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutElementInfo;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeComponent;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class MovePackagingElementAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    private final LayoutTreeComponent myLayoutTreeComponent;
    private final int myDirection;

    public MovePackagingElementAction(LayoutTreeComponent layoutTreeComponent, boolean up) {
        super(
            up ? ProjectLocalize.artifactActionMoveUpText() : ProjectLocalize.artifactActionMoveDownText(),
            ProjectLocalize.artifactActionMoveDescription(),
            up ? PlatformIconGroup.actionsMoveup() : PlatformIconGroup.actionsMovedown()
        );
        myLayoutTreeComponent = layoutTreeComponent;
        myDirection = up ? -1 : 1;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(isEnabled());
    }

    private boolean isEnabled() {
        if (myLayoutTreeComponent.isSortElements()) {
            return false;
        }

        LayoutElementInfo node = myLayoutTreeComponent.getSelection().getNodeIfSingle();
        LayoutElementInfo parent = node == null ? null : myLayoutTreeComponent.getParentNode(node);
        PackagingElement<?> element = node == null ? null : node.getElementIfSingle();
        CompositePackagingElement<?> parentElement = getParentElement(parent);
        if (parentElement == null || element == null) {
            return false;
        }

        List<PackagingElement<?>> children = parentElement.getChildren();
        int index = children.indexOf(element);
        return index != -1 && 0 <= index + myDirection && index + myDirection < children.size();
    }

    private static @Nullable CompositePackagingElement<?> getParentElement(@Nullable LayoutElementInfo parent) {
        return parent != null && parent.getElementIfSingle() instanceof CompositePackagingElement<?> composite ? composite : null;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        LayoutElementInfo node = myLayoutTreeComponent.getSelection().getNodeIfSingle();
        LayoutElementInfo parent = node == null ? null : myLayoutTreeComponent.getParentNode(node);
        PackagingElement<?> element = node == null ? null : node.getElementIfSingle();
        CompositePackagingElement<?> parentElement = getParentElement(parent);
        if (node == null || parent == null || parentElement == null || element == null) {
            return;
        }

        if (!myLayoutTreeComponent.checkCanModifyChildren(parentElement, parent, List.of(node))) {
            return;
        }

        List<PackagingElement<?>> toSelect = new ArrayList<>();
        myLayoutTreeComponent.editLayout(() -> {
            int index = parentElement.getChildren().indexOf(element);
            PackagingElement<?> moved = parentElement.moveChild(index, myDirection);
            if (moved != null) {
                toSelect.add(moved);
            }
        });

        if (!toSelect.isEmpty()) {
            myLayoutTreeComponent.updateAndSelect(toSelect);
        }
    }
}
