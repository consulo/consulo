/*
 * Copyright 2013 Consulo.org
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
package com.intellij.openapi.roots.ui.configuration.extension;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.SimpleTextAttributes;
import org.consulo.module.extension.ModuleExtensionProvider;
import org.consulo.module.extension.ModuleExtensionProviderEP;

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
      final ModuleExtensionProviderEP providerEP = ((ExtensionCheckedTreeNode)value).getProviderEP();
      if (providerEP == null) {
        return;
      }
      final ModuleExtensionProvider instance = providerEP.getInstance();

      final boolean enabled = ((ExtensionCheckedTreeNode)value).isEnabled();
      final Icon icon = instance.getIcon();
      if (icon != null) {
        getTextRenderer().setIcon(enabled ? instance.getIcon() : IconLoader.getTransparentIcon(icon));
      }
      getTextRenderer()
        .append(instance.getName(), enabled ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
  }
}
