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

package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.application.AllIcons;
import consulo.module.Module;
import consulo.ui.ex.SimpleTextAttributes;

/**
 * @author yole
 */
public class ChangesBrowserModuleNode extends ChangesBrowserNode<Module> {
  protected ChangesBrowserModuleNode(Module userObject) {
    super(userObject);
  }

  @Override
  public void render(final ChangesBrowserNodeRenderer renderer, final boolean selected, final boolean expanded, final boolean hasFocus) {
    final Module module = (Module)userObject;

    renderer.append(module.isDisposed() ? "" : module.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    appendCount(renderer);
    renderer.setIcon(AllIcons.Nodes.Module);
  }

  @Override
  public String getTextPresentation() {
    return getUserObject().getName();
  }

  @Override
  public int getSortWeight() {
    return 3;
  }

  @Override
  public int compareUserObjects(final Object o2) {
    return o2 instanceof Module module ? getUserObject().getName().compareToIgnoreCase(module.getName()) : 0;
  }
}
