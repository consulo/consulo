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
package consulo.desktop.awt.startup.customizeNew;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposable;
import consulo.ide.impl.startup.customize.CustomizeWizardContext;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.awt.tree.TreeUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.*;
import java.util.List;

public class CustomizePluginsStepPanel extends AbstractCustomizeWizardStep {
  private CheckedTreeNode myRoot;

  public CustomizePluginsStepPanel() {
  }

  private void collectDeepDependencies(Set<String> deepDependencies, PluginDescriptor ideaPluginDescriptor, CustomizeWizardContext context) {
    for (PluginId depPluginId : ideaPluginDescriptor.getDependentPluginIds()) {
      String idString = depPluginId.getIdString();
      deepDependencies.add(idString);

      PluginDescriptor depPluginDescriptor = context.getPluginDescriptors().get(depPluginId);
      if (depPluginDescriptor != null) {
        collectDeepDependencies(deepDependencies, depPluginDescriptor, context);
      }
    }
  }

  private static String getValueOfNode(Object value) {
    if (value instanceof CheckedTreeNode) {
      Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
      if (!(userObject instanceof PluginDescriptor)) {
        return null;
      }

      return ((PluginDescriptor)userObject).getName();
    }
    else if (value instanceof DefaultMutableTreeNode) {
      Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
      if (!(userObject instanceof String)) {
        return null;
      }
      return (String)userObject;
    }
    return null;
  }

  private static void setupChecked(DefaultMutableTreeNode treeNode, Set<String> set, Boolean state) {
    Object userObject = treeNode.getUserObject();
    if (userObject instanceof PluginDescriptor) {
      String id = ((PluginDescriptor)userObject).getPluginId().getIdString();
      boolean contains = set.contains(id);
      if (state == null) {
        ((CheckedTreeNode)treeNode).setChecked(contains);
      }
      else if (contains) {
        ((CheckedTreeNode)treeNode).setChecked(state);
      }
    }

    int childCount = treeNode.getChildCount();
    for (int i = 0; i < childCount; i++) {
      DefaultMutableTreeNode childAt = (DefaultMutableTreeNode)treeNode.getChildAt(i);
      setupChecked(childAt, set, state);
    }
  }

  @Nonnull
  public Set<PluginDescriptor> getPluginsForDownload() {
    Set<PluginDescriptor> set = new HashSet<>();
    collect(myRoot, set);
    return set;
  }

  private static void collect(DefaultMutableTreeNode treeNode, Set<PluginDescriptor> set) {
    Object userObject = treeNode.getUserObject();
    if (userObject instanceof PluginDescriptor) {
      CheckedTreeNode checkedTreeNode = (CheckedTreeNode)treeNode;
      if (checkedTreeNode.isChecked()) {
        PluginDescriptor pluginDescriptor = (PluginDescriptor)userObject;

        PluginDescriptor idePlugin = PluginManager.findPlugin(pluginDescriptor.getPluginId());
        if (idePlugin == null) {
          set.add(pluginDescriptor);
        }
      }
    }

    int childCount = treeNode.getChildCount();
    for (int i = 0; i < childCount; i++) {
      DefaultMutableTreeNode childAt = (DefaultMutableTreeNode)treeNode.getChildAt(i);
      collect(childAt, set);
    }
  }

  @Nonnull
  @Override
  public JPanel createComponnent(CustomizeWizardContext context, @Nonnull Disposable uiDisposable) {
    JPanel panel = new JPanel(new BorderLayout());

    myRoot = new CheckedTreeNode(null);
    for (Map.Entry<String, Collection<PluginDescriptor>> entry : context.getPluginDescriptorsByTag().entrySet()) {
      String key = entry.getKey();
      List<PluginDescriptor> value = new ArrayList<>(entry.getValue());
      value.sort(Comparator.comparing(PluginDescriptor::getName));

      DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(key);
      for (PluginDescriptor descriptor : value) {
        CheckedTreeNode newChild = new CheckedTreeNode(descriptor);
        newChild.setChecked(context.getPluginsForDownload().contains(descriptor.getPluginId()));
        newChild.setEnabled(PluginManager.findPlugin(descriptor.getPluginId()) == null);
        
        groupNode.add(newChild);

        PluginDescriptor plugin = PluginManager.findPlugin(descriptor.getPluginId());
        if (plugin != null) {
          newChild.setChecked(true);
          newChild.setEnabled(false);
        }
      }
      myRoot.add(groupNode);
    }

    CheckboxTree checkboxTree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer() {
      @Override
      public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        String valueOfNode = getValueOfNode(value);
        if (valueOfNode != null) {
          getTextRenderer().append(valueOfNode);
        }
      }
    }, myRoot) {
      @Override
      protected void onNodeStateChanged(CheckedTreeNode node) {
        boolean state = node.isChecked();

        Object userObject = node.getUserObject();
        if (userObject instanceof PluginDescriptor) {
          Set<String> deepDependencies = new HashSet<String>();
          collectDeepDependencies(deepDependencies, (PluginDescriptor)userObject, context);
          setupChecked(myRoot, deepDependencies, state);
        }
        repaint();
      }
    };
    TreeUtil.sort(myRoot, (o1, o2) -> {
      String stringValue1 = getValueOfNode(o1);
      String stringValue2 = getValueOfNode(o2);
      if (stringValue1 != null && stringValue2 != null) {
        return stringValue1.compareToIgnoreCase(stringValue2);
      }
      return 0;
    });
    checkboxTree.setRootVisible(false);
    TreeUtil.expandAll(checkboxTree);
    panel.add(ScrollPaneFactory.createScrollPane(checkboxTree), BorderLayout.CENTER);
    return panel;
  }

  @Override
  public boolean isVisible(@Nonnull CustomizeWizardContext customizeWizardContext) {
    return !customizeWizardContext.getPluginDescriptors().isEmpty();
  }

  @Override
  public String getHTMLHeader() {
    return "<html><body><h2>Select plugins for download</h2></body></html>";
  }
}
