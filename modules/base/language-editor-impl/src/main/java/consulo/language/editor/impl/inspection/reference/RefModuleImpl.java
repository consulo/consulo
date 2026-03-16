/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.editor.impl.inspection.reference;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.RefManager;
import consulo.language.editor.inspection.reference.RefModule;
import consulo.language.editor.inspection.reference.RefVisitor;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;

public class RefModuleImpl extends RefEntityImpl implements RefModule {
  private final Module myModule;

  RefModuleImpl(Module module, RefManager manager) {
    super(module.getName(), manager);
    myModule = module;
    ((RefProjectImpl)manager.getRefProject()).add(this);
  }

  @Override
  public synchronized void add(RefEntity child) {
    if (myChildren == null) {
      myChildren = new ArrayList<>();
    }
    myChildren.add(child);

    if (child.getOwner() == null) {
      ((RefEntityImpl)child).setOwner(this);
    }
  }

  @Override
  public synchronized void removeChild(RefEntity child) {
    if (myChildren != null) {
      myChildren.remove(child);
    }
  }

  @Override
  public void accept(RefVisitor refVisitor) {
    ApplicationManager.getApplication().runReadAction(() -> refVisitor.visitModule(this));
  }

  @Override
  
  public Module getModule() {
    return myModule;
  }

  @Override
  public boolean isValid() {
    return !myModule.isDisposed();
  }

  @Override
  public Image getIcon(boolean expanded) {
    return AllIcons.Nodes.Module;
  }

  @Nullable
  static RefEntity moduleFromName(RefManager manager, String name) {
    return manager.getRefModule(ModuleManager.getInstance(manager.getProject()).findModuleByName(name));
  }
}
