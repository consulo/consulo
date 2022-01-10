/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.openapi.roots.impl;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.roots.impl.ModuleRootLayerImpl;
import javax.annotation.Nonnull;


/**
 *  @author dsl
 */
public abstract class BaseModuleRootLayerChild implements Disposable {
  protected final ModuleRootLayerImpl myModuleRootLayer;
  private boolean myDisposed;

  public BaseModuleRootLayerChild(@Nonnull ModuleRootLayerImpl moduleRootLayer) {
    myModuleRootLayer = moduleRootLayer;

    Disposer.register(moduleRootLayer, this);
  }

  @Nonnull
  public ModuleRootLayerImpl getModuleRootLayer() {
    return myModuleRootLayer;
  }

  @Nonnull
  public RootModelImpl getRootModel() {
    return myModuleRootLayer.getRootModel();
  }

  @Override
  public void dispose() {
    myDisposed = true;
  }

  public boolean isDisposed() {
    return myDisposed;
  }
}
