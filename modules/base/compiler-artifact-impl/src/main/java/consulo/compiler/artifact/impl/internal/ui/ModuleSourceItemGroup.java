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
package consulo.compiler.artifact.impl.internal.ui;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.compiler.artifact.internal.SourceItemWeights;
import consulo.compiler.artifact.ui.PackagingSourceItemsProvider;
import consulo.ui.ex.tree.PresentationData;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.compiler.artifact.ui.SourceItemPresentation;
import consulo.module.Module;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.ui.ex.SimpleTextAttributes;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public class ModuleSourceItemGroup extends PackagingSourceItem {
  private final Module myModule;

  public ModuleSourceItemGroup(@Nonnull Module module) {
    super(true);
    myModule = module;
  }

  @Override
  public SourceItemPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    return new ModuleSourceItemPresentation(myModule, context);
  }

  public boolean equals(Object obj) {
    return obj instanceof ModuleSourceItemGroup && myModule.equals(((ModuleSourceItemGroup)obj).myModule);
  }

  public int hashCode() {
    return myModule.hashCode();
  }

  @Override
  @Nonnull
  public List<? extends PackagingElement<?>> createElements(@Nonnull ArtifactEditorContext context) {
    Set<Module> modules = new LinkedHashSet<>();
    collectDependentModules(myModule, modules, context);

    Artifact artifact = context.getArtifact();
    ArtifactType artifactType = artifact.getArtifactType();
    Set<PackagingSourceItem> items = new LinkedHashSet<>();
    for (Module module : modules) {
      Application.get().getExtensionPoint(PackagingSourceItemsProvider.class).forEachExtensionSafe(provider -> {
        ModuleSourceItemGroup parent = new ModuleSourceItemGroup(module);
        for (PackagingSourceItem sourceItem : provider.getSourceItems(context, artifact, parent)) {
          if (artifactType.isSuitableItem(sourceItem) && sourceItem.isProvideElements()) {
            items.add(sourceItem);
          }
        }
      });
    }

    List<PackagingElement<?>> result = new ArrayList<>();
    PackagingElementFactory factory = PackagingElementFactory.getInstance(context.getProject());
    for (PackagingSourceItem item : items) {
      String path = artifactType.getDefaultPathFor(item.getKindOfProducedElements());
      if (path != null) {
        result.addAll(factory.createParentDirectories(path, item.createElements(context)));
      }
    }
    return result;
  }

  private static void collectDependentModules(Module module, Set<Module> modules, ArtifactEditorContext context) {
    if (!modules.add(module)) return;
    
    for (OrderEntry entry : context.getModulesProvider().getRootModel(module).getOrderEntries()) {
      if (entry instanceof ModuleOrderEntry) {
        ModuleOrderEntry moduleEntry = (ModuleOrderEntry)entry;
        Module dependency = moduleEntry.getModule();
        DependencyScope scope = moduleEntry.getScope();
        if (dependency != null && scope.isForProductionRuntime()) {
          collectDependentModules(dependency, modules, context);
        }
      }
    }
  }

  public Module getModule() {
    return myModule;
  }

  private static class ModuleSourceItemPresentation extends SourceItemPresentation {
    private final Module myModule;
    private final ArtifactEditorContext myContext;

    public ModuleSourceItemPresentation(@Nonnull Module module, ArtifactEditorContext context) {
      myModule = module;
      myContext = context;
    }

    @Override
    public String getPresentableName() {
      return myModule.getName();
    }

    @Override
    public void render(@Nonnull PresentationData presentationData, SimpleTextAttributes mainAttributes,
                       SimpleTextAttributes commentAttributes) {
      presentationData.setIcon(AllIcons.Nodes.Module);
      presentationData.addText(myModule.getName(), mainAttributes);
    }

    @Override
    public boolean canNavigateToSource() {
      return true;
    }

    @Override
    public void navigateToSource() {
      myContext.selectModule(myModule);
    }

    @Override
    public int getWeight() {
      return SourceItemWeights.MODULE_WEIGHT;
    }
  }
}
