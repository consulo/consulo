/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.profile.codeInspection.ui;

import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.ide.impl.idea.profile.codeInspection.ui.inspectionsTree.InspectionConfigTreeNode;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.collection.Queue;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Batkovich
 */
public class InspectionsAggregationUtil {
  public static List<HighlightDisplayKey> getInspectionsKeys(InspectionConfigTreeNode node) {
    return ContainerUtil.map(getInspectionsNodes(node), InspectionConfigTreeNode::getKey);
  }

  public static List<InspectionConfigTreeNode> getInspectionsNodes(InspectionConfigTreeNode node) {
    Queue<InspectionConfigTreeNode> q = new Queue<InspectionConfigTreeNode>(1);
    q.addLast(node);
    return getInspectionsNodes(q);
  }

  public static List<InspectionConfigTreeNode> getInspectionsNodes(TreePath[] paths) {
    Queue<InspectionConfigTreeNode> q = new Queue<InspectionConfigTreeNode>(paths.length);
    for (TreePath path : paths) {
      if (path != null) {
        q.addLast((InspectionConfigTreeNode)path.getLastPathComponent());
      }
    }
    return getInspectionsNodes(q);
  }

  private static List<InspectionConfigTreeNode> getInspectionsNodes(Queue<InspectionConfigTreeNode> queue) {
    Set<InspectionConfigTreeNode> nodes = new HashSet<InspectionConfigTreeNode>();
    while (!queue.isEmpty()) {
      InspectionConfigTreeNode node = queue.pullFirst();
      if (node.getDescriptors() == null) {
        for (int i = 0; i < node.getChildCount(); i++) {
          InspectionConfigTreeNode childNode = (InspectionConfigTreeNode) node.getChildAt(i);
          queue.addLast(childNode);
        }
      } else {
        nodes.add(node);
      }
    }
    return new ArrayList<InspectionConfigTreeNode>(nodes);
  }
}
