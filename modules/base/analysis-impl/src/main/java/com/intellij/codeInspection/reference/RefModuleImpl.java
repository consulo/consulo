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
package com.intellij.codeInspection.reference;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;

class RefModuleImpl extends RefEntityImpl implements RefModule {
  private final Module myModule;

  RefModuleImpl(@Nonnull Module module, @Nonnull RefManager manager) {
    super(module.getName(), manager);
    myModule = module;
    ((RefProjectImpl)manager.getRefProject()).add(this);
  }

  @Override
  public synchronized void add(@Nonnull final RefEntity child) {
    if (myChildren == null) {
      myChildren = new ArrayList<>();
    }
    myChildren.add(child);

    if (child.getOwner() == null) {
      ((RefEntityImpl)child).setOwner(this);
    }
  }

  @Override
  public synchronized void removeChild(@Nonnull final RefEntity child) {
    if (myChildren != null) {
      myChildren.remove(child);
    }
  }

  @Override
  public void accept(@Nonnull final RefVisitor refVisitor) {
    ApplicationManager.getApplication().runReadAction(() -> refVisitor.visitModule(this));
  }

  @Override
  @Nonnull
  public Module getModule() {
    return myModule;
  }

  @Override
  public boolean isValid() {
    return !myModule.isDisposed();
  }

  @Override
  public Icon getIcon(final boolean expanded) {
    return AllIcons.Nodes.Module;
  }

  @Nullable
  static RefEntity moduleFromName(final RefManager manager, final String name) {
    return manager.getRefModule(ModuleManager.getInstance(manager.getProject()).findModuleByName(name));
  }
}
