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
package consulo.execution;

import consulo.content.bundle.Sdk;
import consulo.execution.localize.ExecutionLocalize;
import consulo.module.Module;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.process.ExecutionException;


public class CantRunException extends ExecutionException {
  public CantRunException(String message) {
    super(message);
  }

  public CantRunException(String s, Throwable cause) {
    super(s, cause);
  }

  public static CantRunException noModuleConfigured(String moduleName) {
    if (moduleName.trim().length() == 0) {
      return new CantRunException(ExecutionLocalize.noModuleDefinedErrorMessage().get());
    }
    return new CantRunException(ExecutionLocalize.moduleDoesNotExistErrorMessage(moduleName).get());
  }

  @Deprecated
  public static CantRunException noJdkForModule(Module module) {
    return new CantRunException(ExecutionLocalize.noJdkForModuleErrorMessage(module.getName()).get());
  }

  public static CantRunException noModuleExtension(Module module, Class<? extends ModuleExtension> extensionName) {

    return new CantRunException(ExecutionLocalize.noSdkForModuleExtensionErrorMessage(extensionName.getName(), module.getName()).get());
  }

  public static CantRunException noSdkForModuleExtension(ModuleExtension e) {
    String moduleExtensionName = ModuleExtensionHelper.getInstance(e.getProject()).getModuleExtensionName(e);
    return new CantRunException(ExecutionLocalize.noSdkForModuleExtensionErrorMessage(moduleExtensionName, e.getModule().getName()).get());
  }

  public static CantRunException jdkMisconfigured(Sdk jdk, Module module) {
    return new CantRunException(ExecutionLocalize.jdkIsBadConfiguredErrorMessage(jdk.getName()).get());
  }

  public static CantRunException classNotFound(String className, Module module) {
    return new CantRunException(ExecutionLocalize.classNotFoundInModuleErrorMessage(className, module.getName()).get());
  }

  public static CantRunException packageNotFound(String packageName) {
    return new CantRunException(ExecutionLocalize.packageNotFoundErrorMessage(packageName).get());
  }

  public static CantRunException noJdkConfigured(String jdkName) {
    if (jdkName != null) {
      return new CantRunException(ExecutionLocalize.jdkNotConfiguredErrorMessage(jdkName).get());
    }
    return new CantRunException(ExecutionLocalize.projectHasNoJdkErrorMessage().get());
  }

  public static CantRunException badModuleDependencies() {
    return new CantRunException(ExecutionLocalize.someModulesHasCircularDependencyErrorMessage().get());
  }

  public static CantRunException noJdkConfigured() {
    return new CantRunException(ExecutionLocalize.projectHasNoJdkConfiguredErrorMessage().get());
  }
}
