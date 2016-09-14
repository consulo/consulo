/*
 * Copyright 2013-2014 must-be.org
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
package consulo.ide.impl;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public class NewModuleContext {
  private Map<String, Pair<String, Icon>> myIdToName = new HashMap<String, Pair<String, Icon>>();
  private List<Pair<String[], NewModuleBuilderProcessor>> mySetup = new ArrayList<Pair<String[], NewModuleBuilderProcessor>>();

  public void addItem(@NotNull String id, @NotNull String name, @NotNull Icon icon) {
    if (myIdToName.containsKey(id)) {
      return;
    }
    myIdToName.put(id, Pair.create(name, icon));
  }

  public void setupItem(@NotNull String[] ids, @NotNull NewModuleBuilderProcessor processor) {
    mySetup.add(Pair.create(ids, processor));
  }

  @NotNull
  public List<Pair<String[], NewModuleBuilderProcessor>> getSetup() {
    return mySetup;
  }

  @NotNull
  public Pair<String, Icon> getItem(@NotNull String id) {
    Pair<String, Icon> pair = myIdToName.get(id);
    return pair == null ? Pair.create(id, AllIcons.Toolbar.Unknown) : pair;
  }
}
