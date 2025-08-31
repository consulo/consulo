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

package consulo.execution.configuration;

import consulo.application.AccessRule;
import consulo.application.util.function.ThrowableComputable;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.util.ModuleContentUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for a configuration that is associated with a specific module. For example, Java run configurations use the selected module
 * to determine the run classpath.
 */
public abstract class ModuleBasedConfiguration<ConfigurationModule extends RunConfigurationModule> extends LocatableConfigurationBase implements Cloneable, ModuleRunConfiguration {
  private static final Logger LOG = Logger.getInstance(ModuleBasedConfiguration.class);
  private final ConfigurationModule myModule;

  protected static final String TO_CLONE_ELEMENT_NAME = "toClone";

  public ModuleBasedConfiguration(String name, ConfigurationModule configurationModule, ConfigurationFactory factory) {
    super(configurationModule.getProject(), factory, name);
    myModule = configurationModule;
  }

  public ModuleBasedConfiguration(ConfigurationModule configurationModule, ConfigurationFactory factory) {
    super(configurationModule.getProject(), factory, "");
    myModule = configurationModule;
  }

  public abstract Collection<Module> getValidModules();

  public ConfigurationModule getConfigurationModule() {
    return myModule;
  }

  public void setModule(Module module) {
    myModule.setModule(module);
  }

  public void setModuleName(@Nullable String moduleName) {
    getConfigurationModule().setModuleName(moduleName);
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    super.readExternal(element);

    myModule.readExternal(element);
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    super.writeExternal(element);

    myModule.writeExternal(element);
  }

  @Deprecated
  protected void readModule(Element element) throws InvalidDataException {
  }

  @Deprecated
  protected void writeModule(Element element) throws WriteExternalException {
  }

  public Collection<Module> getAllModules() {
    return Arrays.asList(ModuleManager.getInstance(getProject()).getModules());
  }

  protected ModuleBasedConfiguration createInstance() {
    ModuleBasedConfiguration<ConfigurationModule> configuration = (ModuleBasedConfiguration<ConfigurationModule>)getFactory().createTemplateConfiguration(getProject());
    configuration.setName(getName());
    return configuration;
  }

  @Override
  public ModuleBasedConfiguration clone() {
    Element element = new Element(TO_CLONE_ELEMENT_NAME);
    try {
      writeExternal(element);
      ModuleBasedConfiguration configuration = createInstance();

      configuration.readExternal(element);

      return configuration;
    }
    catch (InvalidDataException e) {
      LOG.error(e);
      return null;
    }
    catch (WriteExternalException e) {
      LOG.error(e);
      return null;
    }
  }

  @Override
  @Nonnull
  public Module[] getModules() {
    ThrowableComputable<Module[],RuntimeException> action = () -> {
      Module module = getConfigurationModule().getModule();
      return module == null ? Module.EMPTY_ARRAY : new Module[]{module};
    };
    return AccessRule.read(action);
  }

  public void restoreOriginalModule(Module originalModule) {
    if (originalModule == null) return;
    Module[] classModules = getModules();
    Set<Module> modules = new HashSet<>();
    for (Module classModule : classModules) {
      ModuleContentUtil.collectModulesDependsOn(classModule, modules);
    }
    if (modules.contains(originalModule)) setModule(originalModule);
  }

  public void onNewConfigurationCreated() {
    RunConfigurationModule configurationModule = getConfigurationModule();
    if (configurationModule.getModule() == null) {
      Module[] modules = ModuleManager.getInstance(getProject()).getModules();
      configurationModule.setModule(modules.length == 1 ? modules[0] : null);
    }
  }
}
