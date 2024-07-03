/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.execution.configuration;

import consulo.application.AccessRule;
import consulo.component.util.pointer.NamedPointer;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.localize.ExecutionLocalize;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.ModulePointerManager;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import consulo.util.xml.serializer.annotation.Transient;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.List;

@Tag("module")
public class RunConfigurationModule implements JDOMExternalizable {
  private static final Logger LOG = Logger.getInstance(RunConfigurationModule.class);

  @NonNls
  private static final String ELEMENT = "module";
  @NonNls
  private static final String ATTRIBUTE = "name";

  @Nullable
  private NamedPointer<Module> myModulePointer;

  private final Project myProject;

  public RunConfigurationModule(@Nonnull Project project) {
    myProject = project;
  }

  @Override
  public void readExternal(@Nonnull Element element) {
    List<Element> modules = element.getChildren(ELEMENT);
    if (!modules.isEmpty()) {
      if (modules.size() > 1) {
        LOG.warn("Module serialized more than one time");
      }
      // we are unable to set 'null' module from 'not null' one
      String moduleName = modules.get(0).getAttributeValue(ATTRIBUTE);
      if (!StringUtil.isEmpty(moduleName)) {
        myModulePointer = AccessRule.read(() -> ModulePointerManager.getInstance(myProject).create(moduleName));
      }
    }
  }

  @Override
  public void writeExternal(@Nonnull Element parent) {
    Element prev = parent.getChild(ELEMENT);
    if (prev == null) {
      prev = new Element(ELEMENT);
      parent.addContent(prev);
    }
    prev.setAttribute(ATTRIBUTE, getModuleName());
  }

  public void init() {
    if (StringUtil.isEmptyOrSpaces(getModuleName())) {
      Module[] modules = getModuleManager().getModules();
      if (modules.length > 0) {
        setModule(modules[0]);
      }
    }
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Nullable
  @Transient
  public Module getModule() {
    return myModulePointer != null ? myModulePointer.get() : null;
  }

  @Nullable
  public Module findModule(@Nonnull String moduleName) {
    if (myProject.isDisposed()) {
      return null;
    }
    return getModuleManager().findModuleByName(moduleName);
  }

  public void setModule(final Module module) {
    myModulePointer = module != null ? ModulePointerManager.getInstance(myProject).create(module) : null;
  }

  public void setModuleName(@Nullable String moduleName) {
    if (myModulePointer != null && !myModulePointer.getName().equals(moduleName) || myModulePointer == null && moduleName != null) {
      myModulePointer = moduleName != null ? ModulePointerManager.getInstance(myProject).create(moduleName) : null;
    }
  }

  @Attribute("name")
  @Nonnull
  public String getModuleName() {
    return myModulePointer != null ? myModulePointer.getName() : "";
  }

  private ModuleManager getModuleManager() {
    return ModuleManager.getInstance(myProject);
  }

  public void checkForWarning() throws RuntimeConfigurationException {
    final Module module = getModule();
    if (module == null) {
      if (myModulePointer != null) {
        String moduleName = myModulePointer.getName();
        if (ModuleManager.getInstance(myProject).getUnloadedModuleDescription(moduleName) != null) {
          throw new RuntimeConfigurationError(ExecutionLocalize.moduleIsUnloadedFromProjectErrorText(moduleName).get());
        }
        throw new RuntimeConfigurationError(ExecutionLocalize.moduleDoesnTExistInProjectErrorText(moduleName).get());
      }
      throw new RuntimeConfigurationError(ExecutionLocalize.moduleNotSpecifiedErrorText().get());
    }
  }
}
