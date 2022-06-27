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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.internal.statistic.AbstractApplicationUsagesCollector;
import consulo.ide.impl.idea.internal.statistic.beans.UsageDescriptor;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.content.library.Library;
import consulo.content.library.LibraryKind;
import consulo.application.util.function.Processor;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * @author nik
 */
@ExtensionImpl
public class LibraryUsageCollector extends AbstractApplicationUsagesCollector {
  @Nonnull
  @Override
  public Set<UsageDescriptor> getProjectUsages(@Nonnull Project project) {
    final Set<LibraryKind> usedKinds = new HashSet<LibraryKind>();
    final Processor<Library> processor = new Processor<Library>() {
      @Override
      public boolean process(Library library) {
        usedKinds.addAll(LibraryPresentationManagerImpl.getLibraryKinds(library, null));
        return true;
      }
    };
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary(processor);
    }

    final HashSet<UsageDescriptor> usageDescriptors = new HashSet<UsageDescriptor>();
    for (LibraryKind kind : usedKinds) {
      usageDescriptors.add(new UsageDescriptor(kind.getKindId(), 1));
    }
    return usageDescriptors;
  }

  @Nonnull
  @Override
  public String getGroupId() {
    return "consulo.platform.base:libraries";
  }
}
