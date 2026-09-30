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

package com.intellij.diagram;

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
public abstract class DiagramProvider<T> {
  public static final ExtensionPointName<DiagramProvider> DIAGRAM_PROVIDER = new ExtensionPointName<DiagramProvider>("com.intellij.diagram.Provider");
  private static final DiagramColorManager defaultColorManager = new DiagramColorManagerBase();

  public abstract @Pattern("[a-zA-Z0-9_-]*") String getID();
  public abstract DiagramVisibilityManager getVisibilityManager();
  public abstract DiagramNodeContentManager getNodeContentManager();
  public abstract DiagramElementManager<T> getElementManager();
  public abstract DiagramVfsResolver<T> getVfsResolver();
  public abstract DiagramRelationshipManager<T> getRelationshipManager();
  public abstract DiagramDataModel<T> createDataModel(@NotNull Project project, @Nullable T element, @Nullable VirtualFile file);
  public abstract ModificationTracker getModificationTracker(@NotNull Project project);
  public DiagramColorManager getColorManager() {
    return defaultColorManager;
  }
  public DiagramExtras<T> getExtras() {
    return null;
  }  

  @Nullable
  public static DiagramProvider findProvider(DataContext context) {
    for (DiagramProvider provider : DIAGRAM_PROVIDER.getExtensions()) {
      final DiagramElementManager mgr = provider.getElementManager();
      final Object element = mgr.findInDataContext(context);
      if (element != null && mgr.isAcceptableAsNode(element)) {
        return provider;
      }
    }
    return null;
  }

  @NotNull
  public static DiagramProvider[] findProviders(DataContext context) {
    List<DiagramProvider> providers = new ArrayList<DiagramProvider>();
    for (DiagramProvider provider : DIAGRAM_PROVIDER.getExtensions()) {
      final DiagramElementManager mgr = provider.getElementManager();
      final Object element = mgr.findInDataContext(context);
      if (element != null && mgr.isAcceptableAsNode(element)) {
        providers.add(provider);
      }
    }
    return providers.toArray(new DiagramProvider[providers.size()]);
  }

  public static DiagramProvider findByID(String id) {
    for (DiagramProvider provider : DIAGRAM_PROVIDER.getExtensions()) {
      if (provider.getID().equals(id)) {
        return provider;
      }
    }
    return null;
  }    
}
