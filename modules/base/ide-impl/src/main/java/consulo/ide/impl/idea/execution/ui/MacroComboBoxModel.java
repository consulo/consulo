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
package consulo.ide.impl.idea.execution.ui;

import consulo.application.macro.PathMacros;
import consulo.component.macro.PathMacroUtil;
import consulo.util.collection.SmartList;

import javax.swing.*;
import java.util.List;

final class MacroComboBoxModel extends AbstractListModel<String> implements ComboBoxModel<String> {
  private boolean withModuleDir;
  private List<String> macros;
  private Object selected;

  public void useModuleDir(boolean withModuleDir) {
    if (this.withModuleDir == withModuleDir) return;
    this.withModuleDir = withModuleDir;
    if (macros == null) return;
    macros = createMacros();
    fireContentsChanged(this, -1, -1);
  }

  @Override
  public Object getSelectedItem() {
    return selected;
  }

  @Override
  public void setSelectedItem(Object item) {
    if (item == null ? selected == null : item.equals(selected)) return;
    selected = item;
    fireContentsChanged(this, -1, -1);
  }

  @Override
  public int getSize() {
    List<String> list = getMacros();
    return list.size();
  }

  @Override
  public String getElementAt(int index) {
    List<String> list = getMacros();
    return 0 <= index && index < list.size() ? list.get(index) : null;
  }

  public List<String> getMacros() {
    if (macros == null) macros = createMacros();
    return macros;
  }

  private List<String> createMacros() {
    List<String> list = new SmartList<>();
    for (String name : PathMacros.getInstance().getUserMacroNames()) {
      list.add("$" + name + "$");
    }
    if (withModuleDir) {
      list.add(PathMacroUtil.MODULE_DIR_MACRO);
      list.add(PathMacroUtil.MODULE_WORKING_DIR);
    }
    return list;
  }
}
