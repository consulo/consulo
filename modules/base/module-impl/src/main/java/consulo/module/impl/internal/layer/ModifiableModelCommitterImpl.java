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
package consulo.module.impl.internal.layer;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.component.util.graph.DFSTBuilder;
import consulo.component.util.graph.GraphGenerator;
import consulo.component.util.graph.InboundSemiGraph;
import consulo.logging.Logger;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.content.ModifiableModelCommitter;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.RootPolicy;
import consulo.module.impl.internal.ModuleManagerImpl;
import consulo.module.impl.internal.ModuleRootManagerImpl;
import consulo.util.collection.ArrayUtil;
import jakarta.inject.Singleton;

import java.util.*;

@Singleton
@ServiceImpl
public class ModifiableModelCommitterImpl implements ModifiableModelCommitter {
  private static final Logger LOG = Logger.getInstance(ModifiableModelCommitterImpl.class);

  @Override
  @RequiredWriteAction
  public void multiCommit(ModifiableRootModel[] rootModels, ModifiableModuleModel moduleModel) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();

    final List<RootModelImpl> modelsToCommit = getSortedChangedModels(rootModels, moduleModel);

    final List<ModifiableRootModel> modelsToDispose = new ArrayList<ModifiableRootModel>(Arrays.asList(rootModels));
    modelsToDispose.removeAll(modelsToCommit);

    ModuleManagerImpl.commitModelWithRunnable(moduleModel, new Runnable() {
      @Override
      public void run() {
        for (RootModelImpl rootModel : modelsToCommit) {
          rootModel.doCommitAndDispose();
        }

        for (ModifiableRootModel model : modelsToDispose) {
          model.dispose();
        }
      }
    });
  }

  private static List<RootModelImpl> getSortedChangedModels(ModifiableRootModel[] _rootModels, final ModifiableModuleModel moduleModel) {
    List<RootModelImpl> rootModels = new ArrayList<RootModelImpl>();
    for (ModifiableRootModel _rootModel : _rootModels) {
      RootModelImpl rootModel = (RootModelImpl)_rootModel;
      if (rootModel.isChanged()) {
        rootModels.add(rootModel);
      }
    }

    sortRootModels(rootModels, moduleModel);
    return rootModels;
  }

  private static void sortRootModels(List<RootModelImpl> rootModels, final ModifiableModuleModel moduleModel) {
    DFSTBuilder<RootModelImpl> builder = createDFSTBuilder(rootModels, moduleModel);

    final Comparator<RootModelImpl> comparator = builder.comparator();
    Collections.sort(rootModels, comparator);
  }

  private static DFSTBuilder<RootModelImpl> createDFSTBuilder(List<RootModelImpl> rootModels, final ModifiableModuleModel moduleModel) {
    final Map<String, RootModelImpl> nameToModel = new HashMap<String, RootModelImpl>();
    for (final RootModelImpl rootModel : rootModels) {
      final String name = rootModel.getModule().getName();
      LOG.assertTrue(!nameToModel.containsKey(name), name);
      nameToModel.put(name, rootModel);
    }
    final Module[] modules = moduleModel.getModules();
    for (final Module module : modules) {
      final String name = module.getName();
      if (!nameToModel.containsKey(name)) {
        final RootModelImpl rootModel = ((ModuleRootManagerImpl)ModuleRootManager.getInstance(module)).getRootModel();
        nameToModel.put(name, rootModel);
      }
    }
    final Collection<RootModelImpl> allRootModels = nameToModel.values();
    return new DFSTBuilder<>(GraphGenerator.generate(new InboundSemiGraph<>() {
      @Override
      public Collection<RootModelImpl> getNodes() {
        return allRootModels;
      }

      @Override
      public Iterator<RootModelImpl> getIn(RootModelImpl rootModel) {
        final List<String> namesList = rootModel.orderEntries().withoutSdk().withoutLibraries().withoutModuleSourceEntries().process(new RootPolicy<ArrayList<String>>() {
          @Override
          public ArrayList<String> visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, ArrayList<String> strings) {
            final Module module = moduleOrderEntry.getModule();
            if (module != null && !module.isDisposed()) {
              strings.add(module.getName());
            }
            else {
              final Module moduleToBeRenamed = moduleModel.getModuleToBeRenamed(moduleOrderEntry.getModuleName());
              if (moduleToBeRenamed != null && !moduleToBeRenamed.isDisposed()) {
                strings.add(moduleToBeRenamed.getName());
              }
            }
            return strings;
          }
        }, new ArrayList<>());

        final String[] names = ArrayUtil.toStringArray(namesList);
        List<RootModelImpl> result = new ArrayList<RootModelImpl>();
        for (String name : names) {
          final RootModelImpl depRootModel = nameToModel.get(name);
          if (depRootModel != null) { // it is ok not to find one
            result.add(depRootModel);
          }
        }
        return result.iterator();
      }
    }));
  }
}
