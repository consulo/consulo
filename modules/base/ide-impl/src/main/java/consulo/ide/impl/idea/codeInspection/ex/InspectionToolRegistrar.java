/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInspection.ex;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.openapi.util.Factory;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.inspection.GlobalInspectionTool;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import consulo.logging.Logger;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author max
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class InspectionToolRegistrar {
  private static final Logger LOG = Logger.getInstance(InspectionToolRegistrar.class);

  private final List<Supplier<InspectionToolWrapper>> myInspectionToolFactories = new ArrayList<>();

  private final AtomicBoolean myInspectionComponentsLoaded = new AtomicBoolean(false);

  public void ensureInitialized() {
    if (!myInspectionComponentsLoaded.getAndSet(true)) {
      Application application = Application.get();
      for (final LocalInspectionTool tool : application.getExtensionList(LocalInspectionTool.class)) {
        myInspectionToolFactories.add(() -> new LocalInspectionToolWrapper(tool));
      }

      for (final GlobalInspectionTool tool : application.getExtensionList(GlobalInspectionTool.class)) {
        myInspectionToolFactories.add(() -> new GlobalInspectionToolWrapper(tool));
      }
    }
  }

  public static InspectionToolRegistrar getInstance() {
    return ServiceManager.getService(InspectionToolRegistrar.class);
  }

  /**
   * make sure that it is not too late
   */
  @Nonnull
  public Factory<InspectionToolWrapper> registerInspectionToolFactory(@Nonnull Factory<InspectionToolWrapper> factory, boolean store) {
    if (store) {
      myInspectionToolFactories.add(factory);
    }
    return factory;
  }

  @Nonnull
  private Factory<InspectionToolWrapper> registerLocalInspection(final Class toolClass, boolean store) {
    return registerInspectionToolFactory(() -> new LocalInspectionToolWrapper((LocalInspectionTool)Application.get().getInjectingContainer().getUnbindedInstance(toolClass)), store);
  }

  @Nonnull
  private Factory<InspectionToolWrapper> registerGlobalInspection(@Nonnull final Class aClass, boolean store) {
    return registerInspectionToolFactory(() -> new GlobalInspectionToolWrapper((GlobalInspectionTool)Application.get().getInjectingContainer().getUnbindedInstance(aClass)), store);
  }

  @Nonnull
  public List<InspectionToolWrapper> createTools() {
    ensureInitialized();

    final List<InspectionToolWrapper> tools = ContainerUtil.newArrayListWithCapacity(myInspectionToolFactories.size());
    final Set<Supplier<InspectionToolWrapper>> broken = ContainerUtil.newHashSet();
    for (final Supplier<InspectionToolWrapper> factory : myInspectionToolFactories) {
      ProgressManager.checkCanceled();
      final InspectionToolWrapper toolWrapper = factory.get();
      if (toolWrapper != null && checkTool(toolWrapper) == null) {
        tools.add(toolWrapper);
      }
      else {
        broken.add(factory);
      }
    }
    myInspectionToolFactories.removeAll(broken);

    return tools;
  }

  private static String checkTool(@Nonnull final InspectionToolWrapper toolWrapper) {
    if (!(toolWrapper instanceof LocalInspectionToolWrapper)) {
      return null;
    }
    String message = null;
    try {
      final String id = ((LocalInspectionToolWrapper)toolWrapper).getID();
      if (id == null || !LocalInspectionTool.isValidID(id)) {
        message = InspectionsBundle.message("inspection.disabled.wrong.id", toolWrapper.getShortName(), id, LocalInspectionTool.VALID_ID_PATTERN);
      }
    }
    catch (Throwable t) {
      message = InspectionsBundle.message("inspection.disabled.error", toolWrapper.getShortName(), t.getMessage());
    }
    if (message != null) {
      LOG.error(message);
    }
    return message;
  }
}
