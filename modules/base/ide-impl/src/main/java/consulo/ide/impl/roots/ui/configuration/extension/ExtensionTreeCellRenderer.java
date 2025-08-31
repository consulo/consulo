/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.roots.ui.configuration.extension;

import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.image.ImageEffects;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 11:40/19.05.13
 */
public class ExtensionTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
  public ExtensionTreeCellRenderer() {
    super(false, false);
  }

  @Override
  public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    if (value instanceof ExtensionCheckedTreeNode) {
      ExtensionCheckedTreeNode extensionCheckedTreeNode = (ExtensionCheckedTreeNode)value;

      ModuleExtensionProvider providerEP = extensionCheckedTreeNode.getProvider();
      if (providerEP == null) {
        return;
      }

      ColoredTreeCellRenderer textRenderer = getTextRenderer();

      boolean enabled = extensionCheckedTreeNode.isEnabled();
      textRenderer.setIcon(enabled ? providerEP.getIcon() : ImageEffects.transparent(providerEP.getIcon()));
      if (enabled) {
        textRenderer.append(providerEP.getName().get(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
      else {
        textRenderer.append(providerEP.getName().get(), SimpleTextAttributes.GRAY_ATTRIBUTES);
      }
    }
  }
}
