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

import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeComponent;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.CompositePackagingElementNode;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.PackagingElementNode;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.ui.image.Image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class MovePackagingElementAction extends DumbAwareAction {
  private final LayoutTreeComponent myLayoutTreeComponent;
  private final int myDirection;

  public MovePackagingElementAction(LayoutTreeComponent layoutTreeComponent, String text, String description, Image icon, int direction) {
    super(text, description, icon);
    myLayoutTreeComponent = layoutTreeComponent;
    myDirection = direction;
  }

  @Override
  public void update(AnActionEvent e) {
    boolean b = isEnabled();
    e.getPresentation().setEnabled(b);
    e.getPresentation().setText(getTemplatePresentation().getText() + " (disabled if elements are sorted)");
  }

  private boolean isEnabled() {
    if (myLayoutTreeComponent.isSortElements()) {
      return false;
    }
    PackagingElementNode<?> node = myLayoutTreeComponent.getSelection().getNodeIfSingle();
    if (node == null) {
      return false;
    }
    CompositePackagingElementNode parent = node.getParentNode();
    if (parent == null) return false;

    PackagingElement<?> element = node.getElementIfSingle();
    CompositePackagingElement<?> parentElement = parent.getElementIfSingle();
    if (parentElement == null || element == null) return false;
    List<PackagingElement<?>> children = parentElement.getChildren();
    int index = children.indexOf(element);
    return index != -1 && 0 <= index + myDirection && index + myDirection < children.size();
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    PackagingElementNode<?> node = myLayoutTreeComponent.getSelection().getNodeIfSingle();
    if (node == null) return;
    CompositePackagingElementNode parent = node.getParentNode();
    if (parent == null) return;

    final PackagingElement<?> element = node.getElementIfSingle();
    final CompositePackagingElement<?> parentElement = parent.getElementIfSingle();
    if (parentElement == null || element == null) return;


    if (!myLayoutTreeComponent.checkCanModifyChildren(parentElement, parent, Arrays.asList(node))) return;

    final List<PackagingElement<?>> toSelect = new ArrayList<PackagingElement<?>>();
    myLayoutTreeComponent.editLayout(new Runnable() {
      @Override
      public void run() {
        int index = parentElement.getChildren().indexOf(element);
        PackagingElement<?> moved = parentElement.moveChild(index, myDirection);
        if (moved != null) {
          toSelect.add(moved);
        }
      }
    });
    if (!toSelect.isEmpty()) {
      myLayoutTreeComponent.updateAndSelect(parent, toSelect);
    }
  }
}
