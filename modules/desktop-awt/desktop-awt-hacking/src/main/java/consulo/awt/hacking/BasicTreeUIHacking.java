/*
 * Copyright 2013-2019 consulo.io
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
package consulo.awt.hacking;

import consulo.awt.hacking.util.FieldAccessor;

import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreePath;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 2019-11-23
 */
public class BasicTreeUIHacking {
  private static final Method ourGetRowX = AWTHackingUtil.findMethodSilent(BasicTreeUI.class, "getRowX", int.class, int.class);
  private static final Method ourIsLocationInExpandControl = AWTHackingUtil.findMethodSilent(BasicTreeUI.class, "isLocationInExpandControl", TreePath.class, int.class, int.class);

  private static final FieldAccessor<BasicTreeUI, AbstractLayoutCache> ourTreeState = new FieldAccessor<>(BasicTreeUI.class, "treeState", AbstractLayoutCache.class);

  public static int getRowX(TreeUI treeUI, int row, int depth) {
    if (!(treeUI instanceof BasicTreeUI) || ourGetRowX == null) {
      return -1;
    }
    try {
      return (int)ourGetRowX.invoke(treeUI, row, depth);
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean isLocationInExpandControl(TreeUI treeUI, TreePath path, int mouseX, int mouseY) {
    if (!(treeUI instanceof BasicTreeUI) || ourIsLocationInExpandControl == null) {
      return false;
    }

    try {
      return (boolean)ourIsLocationInExpandControl.invoke(treeUI, path, mouseX, mouseY);
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static AbstractLayoutCache getTreeState(BasicTreeUI ui) {
    return ourTreeState.isAvailable() ? ourTreeState.get(ui) : null;
  }
}
