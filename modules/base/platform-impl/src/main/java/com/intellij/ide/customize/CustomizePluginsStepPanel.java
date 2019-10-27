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
package com.intellij.ide.customize;

import com.intellij.openapi.extensions.PluginId;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.tree.TreeUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.ide.customize.CustomizeSelectTemplateStepPanel;
import gnu.trove.THashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class CustomizePluginsStepPanel extends AbstractCustomizeWizardStep {

  private final MultiMap<String, PluginDescriptor> myPluginDescriptors;
  private final CustomizeSelectTemplateStepPanel myTemplateStepPanel;
  private final CheckedTreeNode myRoot;

  public CustomizePluginsStepPanel(MultiMap<String, PluginDescriptor> pluginDescriptors, @Nullable CustomizeSelectTemplateStepPanel templateStepPanel) {
    myPluginDescriptors = pluginDescriptors;
    myTemplateStepPanel = templateStepPanel;
    setLayout(new BorderLayout());

    myRoot = new CheckedTreeNode(null);
    for (Map.Entry<String, Collection<PluginDescriptor>> entry : pluginDescriptors.entrySet()) {
      String key = entry.getKey();
      Collection<PluginDescriptor> value = entry.getValue();

      DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(key);
      for (PluginDescriptor ideaPluginDescriptor : value) {
        CheckedTreeNode newChild = new CheckedTreeNode(ideaPluginDescriptor);
        newChild.setChecked(false);
        groupNode.add(newChild);
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
        if(userObject instanceof PluginDescriptor) {
          Set<String> deepDependencies = new THashSet<String>();
          collectDeepDependencies(deepDependencies, (PluginDescriptor)userObject);
          setupChecked(myRoot, deepDependencies, state);
        }
        repaint();
      }
    };
    TreeUtil.sort(myRoot, new Comparator() {
      @Override
      public int compare(Object o1, Object o2) {
        String stringValue1 = getValueOfNode(o1);
        String stringValue2 = getValueOfNode(o2);
        if (stringValue1 != null && stringValue2 != null) {
          return stringValue1.compareToIgnoreCase(stringValue2);
        }
        return 0;
      }
    });
    checkboxTree.setRootVisible(false);
    TreeUtil.expandAll(checkboxTree);
    add(ScrollPaneFactory.createScrollPane(checkboxTree, true), BorderLayout.CENTER);
  }

  private void collectDeepDependencies(Set<String> deepDependencies, PluginDescriptor ideaPluginDescriptor) {
    for (PluginId depPluginId : ideaPluginDescriptor.getDependentPluginIds()) {
      String idString = depPluginId.getIdString();
      deepDependencies.add(idString);

      for (PluginDescriptor pluginDescriptor : myPluginDescriptors.values()) {
        if(pluginDescriptor.getPluginId().equals(depPluginId)) {
          collectDeepDependencies(deepDependencies, pluginDescriptor);
        }
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

  @Override
  public boolean beforeShown(boolean forward) {
    if (myTemplateStepPanel == null) {
      return false;
    }
    Set<String> enablePluginSet = myTemplateStepPanel.getEnablePluginSet();

    setupChecked(myRoot, enablePluginSet, null);
    return false;
  }

  private static void setupChecked(DefaultMutableTreeNode treeNode, Set<String> set, Boolean state) {
    Object userObject = treeNode.getUserObject();
    if (userObject instanceof PluginDescriptor) {
      String id = ((PluginDescriptor)userObject).getPluginId().getIdString();
      boolean contains = set.contains(id);
      if(state == null) {
        ((CheckedTreeNode)treeNode).setChecked(contains);
      }
      else if(contains) {
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
    Set<PluginDescriptor> set = new THashSet<PluginDescriptor>();
    collect(myRoot, set);
    return set;
  }

  private static void collect(DefaultMutableTreeNode treeNode, Set<PluginDescriptor> set) {
    Object userObject = treeNode.getUserObject();
    if (userObject instanceof PluginDescriptor) {
      CheckedTreeNode checkedTreeNode = (CheckedTreeNode)treeNode;
      if (checkedTreeNode.isChecked()) {
        set.add(((PluginDescriptor)userObject));
      }
    }

    int childCount = treeNode.getChildCount();
    for (int i = 0; i < childCount; i++) {
      DefaultMutableTreeNode childAt = (DefaultMutableTreeNode)treeNode.getChildAt(i);
      collect(childAt, set);
    }
  }

  @Override
  public String getTitle() {
    return "Plugins";
  }

  @Override
  public String getHTMLHeader() {
    return "<html><body><h2>Select Plugins For Download</h2></body></html>";
  }

  @Override
  public String getHTMLFooter() {
    return null;
  }
}
