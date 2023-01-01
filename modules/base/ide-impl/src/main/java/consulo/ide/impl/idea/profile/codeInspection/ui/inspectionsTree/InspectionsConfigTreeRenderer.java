/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 14-May-2009
 */
package consulo.ide.impl.idea.profile.codeInspection.ui.inspectionsTree;

import consulo.language.editor.inspection.InspectionsBundle;
import consulo.ide.impl.idea.codeInspection.ex.Descriptor;
import consulo.language.editor.impl.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.ide.impl.idea.profile.codeInspection.ui.ToolDescriptors;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.UIUtil;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public abstract class InspectionsConfigTreeRenderer implements TreeCellRenderer {
  protected abstract String getFilter();

  @Override
  public Component getTreeCellRendererComponent(JTree tree,
                                                Object value,
                                                boolean selected,
                                                boolean expanded,
                                                boolean leaf,
                                                int row,
                                                boolean hasFocus) {
    final SimpleColoredComponent component = new SimpleColoredComponent();
    if (!(value instanceof InspectionConfigTreeNode)) return component;
    InspectionConfigTreeNode node = (InspectionConfigTreeNode)value;

    Object object = node.getUserObject();

    final Color background = selected ? UIUtil.getTreeSelectionBackground() : UIUtil.getTreeTextBackground();
    UIUtil.changeBackGround(component, background);
    Color foreground =
            selected ? UIUtil.getTreeSelectionForeground() : node.isProperSetting() ? JBColor.BLUE : UIUtil.getTreeTextForeground();

    @NonNls String text;
    int style = SimpleTextAttributes.STYLE_PLAIN;
    String hint = null;
    if (object instanceof String) {
      text = (String)object;
      style = SimpleTextAttributes.STYLE_BOLD;
    }
    else {
      final ToolDescriptors descriptors = node.getDescriptors();
      assert descriptors != null;
      final Descriptor defaultDescriptor = descriptors.getDefaultDescriptor();
      text = defaultDescriptor.getText();
      hint = getHint(defaultDescriptor);
    }

    if (text != null) {
      SearchUtil.appendFragments(getFilter(), text, style, foreground, background, component);
    }
    if (hint != null) {
      component.append(" " + hint, selected ? new SimpleTextAttributes(Font.PLAIN, foreground) : SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    component.setForeground(foreground);
    return component;
  }

  @Nullable
  private static String getHint(final Descriptor descriptor) {
    final InspectionToolWrapper toolWrapper = descriptor.getToolWrapper();
    if (toolWrapper instanceof LocalInspectionToolWrapper ||
        toolWrapper instanceof GlobalInspectionToolWrapper && !((GlobalInspectionToolWrapper)toolWrapper).worksInBatchModeOnly()) {
      return null;
    }
    return InspectionsBundle.message("inspection.tool.availability.in.tree.node1");
  }
}