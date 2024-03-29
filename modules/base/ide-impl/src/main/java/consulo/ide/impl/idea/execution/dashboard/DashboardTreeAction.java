/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.dashboard;

import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author konstantin.aleev
 */
public abstract class DashboardTreeAction<T, C extends TreeContent> extends AnAction {
  protected DashboardTreeAction(String text, String description, Image icon) {
    super(text, description, icon);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    List<T> targetNodes = getTargetNodes(e);

    boolean visible = isVisibleForAnySelection(e) || (!targetNodes.isEmpty() && targetNodes.stream().allMatch(this::isVisible4));
    boolean enabled = visible && (!targetNodes.isEmpty() && targetNodes.stream().allMatch(this::isEnabled4));

    presentation.setVisible(visible);
    presentation.setEnabled(enabled);
    updatePresentation(presentation, ContainerUtil.getFirstItem(targetNodes));
  }

  /**
   * Invokes {@link #collectNodes(AbstractTreeBuilder) collectNodes()} to collect nodes.
   * If each collected node could be casted to tree action node class,
   * returns a list of collected nodes casted to tree action node class, otherwise returns empty list.
   *
   * @param e Action event.
   * @return List of target nodes for this action.
   */
  @Nonnull
  protected List<T> getTargetNodes(AnActionEvent e) {
    C content = getTreeContent(e);
    if (content == null) {
      return Collections.emptyList();
    }
    Set<?> selectedElements = collectNodes(content.getBuilder());
    int selectionCount = selectedElements.size();
    if (selectionCount == 0 || selectionCount > 1 && !isMultiSelectionAllowed()) {
      return Collections.emptyList();
    }
    Class<T> targetNodeClass = getTargetNodeClass();
    List<T> result = new ArrayList<>();
    for (Object selectedElement : selectedElements) {
      if (!targetNodeClass.isInstance(selectedElement)) {
        return Collections.emptyList();
      }
      result.add(targetNodeClass.cast(selectedElement));
    }
    return result;
  }

  /**
   * This implementation returns a set of selected nodes.
   * Subclasses may override this method to return modified nodes set.
   *
   * @param treeBuilder Tree builder.
   * @return Set of tree nodes for which action should be performed.
   */
  @Nonnull
  protected Set<?> collectNodes(@Nonnull AbstractTreeBuilder treeBuilder) {
    return treeBuilder.getSelectedElements();
  }

  protected abstract C getTreeContent(AnActionEvent e);

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    List<T> verifiedTargetNodes = getTargetNodes(e).stream().filter(node -> isVisible4(node) && isEnabled4(node))
      .collect(Collectors.toList());
    doActionPerformed(getTreeContent(e), e, verifiedTargetNodes);
  }

  protected boolean isVisibleForAnySelection(@Nonnull AnActionEvent e) {
    return false;
  }

  protected boolean isMultiSelectionAllowed() {
    return false;
  }

  protected boolean isVisible4(T node) {
    return true;
  }

  protected boolean isEnabled4(T node) {
    return true;
  }

  protected void updatePresentation(@Nonnull Presentation presentation, @jakarta.annotation.Nullable T node) {
  }

  protected void doActionPerformed(@Nonnull C content, AnActionEvent e, List<T> nodes) {
    nodes.forEach(node -> doActionPerformed(content, e , node));
  }

  protected void doActionPerformed(@Nonnull C content, AnActionEvent e, T node) {
    doActionPerformed(node);
  }

  protected void doActionPerformed(T node) {
    throw new UnsupportedOperationException();
  }

  protected abstract Class<T> getTargetNodeClass();
}
