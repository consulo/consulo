/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
 * @author max
 */
package com.intellij.ide.impl;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;

public class ModuleStructureComponent extends SimpleToolWindowPanel implements Disposable, DataProvider {
  private final ModuleStructurePane myStructurePane;

  public ModuleStructureComponent(Module module) {
    super(true, true);

    myStructurePane = new ModuleStructurePane(module);
    Disposer.register(this, myStructurePane);

    setContent(myStructurePane.createComponent());
  }

  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    return myStructurePane.getData(dataId);
  }

  @Override
  public void dispose() {

  }
}
