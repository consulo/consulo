/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.app.impl.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import consulo.preferences.internal.ConfigurableWeight;
import consulo.ui.TreeNode;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 2020-05-11
 *
 * from OptionsTree - swing settings dialog
 */
public class UnifiedConfigurableComparator implements Comparator<TreeNode<Configurable>> {
  public static final UnifiedConfigurableComparator INSTANCE = new UnifiedConfigurableComparator();

  @Override
  public int compare(TreeNode<Configurable> o1, TreeNode<Configurable> o2) {
    double weight1 = getWeight(o1);
    double weight2 = getWeight(o2);
    if (weight1 != weight2) {
      return (int)(weight2 - weight1);
    }

    return getConfigurableDisplayName(o1.getValue()).compareToIgnoreCase(getConfigurableDisplayName(o2.getValue()));
  }

  private static int getWeight(TreeNode<Configurable> node) {
    Configurable configurable = node.getValue();
    if (configurable instanceof ConfigurableWeight) {
      return ((ConfigurableWeight)configurable).getConfigurableWeight();
    }
    else if(configurable instanceof ConfigurableWrapper) {
      UnnamedConfigurable wrapped = ((ConfigurableWrapper)configurable).getConfigurable();
      if(wrapped instanceof ConfigurableWeight) {
        return ((ConfigurableWeight)wrapped).getConfigurableWeight();
      }
    }
    return 0;
  }

  public static String getConfigurableDisplayName(final Configurable c) {
    final String name = c.getDisplayName();
    return name != null ? name : "{ Unnamed Page:" + c.getClass().getSimpleName() + " }";
  }
}
