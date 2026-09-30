/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.uml;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public abstract class UmlProvider<T> {
  public static final ExtensionPointName<UmlProvider> UML_PROVIDER = new ExtensionPointName<UmlProvider>("com.intellij.uml.umlProvider");
  private static final UmlColorManager defaultColorManager = new UmlColorManagerBase();

  public abstract @Pattern("[a-zA-Z0-9_-]*") String getID();
  public abstract UmlVisibilityManager getVisibilityManager();
  public abstract UmlNodeContentManager getNodeContentManager();
  public abstract UmlElementManager<T> getElementManager();
  public abstract UmlVfsResolver<T> getVfsResolver();
  public abstract UmlRelationshipManager<T> getRelationshipManager();
  public abstract UmlDataModel<T> createDataModel(@NotNull Project project, @Nullable T element, @Nullable VirtualFile file);
  public abstract ModificationTracker getModificationTracker(@NotNull Project project);
  public UmlColorManager getColorManager() {
    return defaultColorManager;
  }
  public UmlExtras<T> getExtras() {
    return null;
  }  

  @Nullable
  public static UmlProvider findProvider(DataContext context) {
    for (UmlProvider provider : UML_PROVIDER.getExtensions()) {
      final UmlElementManager mgr = provider.getElementManager();
      final Object element = mgr.findInDataContext(context);
      if (element != null && mgr.isAcceptableAsNode(element)) {
        return provider;
      }
    }
    return null;
  }

  @NotNull
  public static UmlProvider[] findProviders(DataContext context) {
    List<UmlProvider> providers = new ArrayList<UmlProvider>();
    for (UmlProvider provider : UML_PROVIDER.getExtensions()) {
      final UmlElementManager mgr = provider.getElementManager();
      final Object element = mgr.findInDataContext(context);
      if (element != null && mgr.isAcceptableAsNode(element)) {
        providers.add(provider);
      }
    }
    return providers.toArray(new UmlProvider[providers.size()]);
  }

  public static UmlProvider findByID(String id) {
    for (UmlProvider provider : UML_PROVIDER.getExtensions()) {
      if (provider.getID().equals(id)) {
        return provider;
      }
    }
    return null;
  }    
}
