/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container.impl.classloader;

import consulo.container.impl.ContainerLogger;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * @author VISTALL
 * @since 2019-11-19
 */
public class Java9ModuleInitializer {
  private static final Method java_io_File_toPath = findMethod(File.class, "toPath");

  private static final Class java_nio_file_Path = findClass("java.nio.file.Path");
  private static final Class java_lang_Module = findClass("java.lang.Module");
  private static final Class java_lang_module_ModuleFinder = findClass("java.lang.module.ModuleFinder");
  private static final Class java_lang_module_ModuleReference = findClass("java.lang.module.ModuleReference");
  private static final Class java_lang_module_Configuration = findClass("java.lang.module.Configuration");
  private static final Class java_lang_module_ModuleDescriptor = findClass("java.lang.module.ModuleDescriptor");
  private static final Class java_lang_ModuleLayer = findClass("java.lang.ModuleLayer");
  private static final Class java_lang_ModuleLayer$Controller = findClass("java.lang.ModuleLayer$Controller");
  private static final Class java_util_function_Function = findClass("java.util.function.Function");
  private static final Class java_util_Optional = findClass("java.util.Optional");

  private static final Method java_lang_ModuleLayer_boot = findMethod(java_lang_ModuleLayer, "boot");
  private static final Method java_lang_ModuleLayer_findModule = findMethod(java_lang_ModuleLayer, "findModule", String.class);
  private static final Method java_lang_ModuleLayer_configuration = findMethod(java_lang_ModuleLayer, "configuration");
  private static final Method java_lang_ModuleLayer_defineModules = findMethod(java_lang_ModuleLayer, "defineModules", java_lang_module_Configuration, List.class, java_util_function_Function);
  private static final Method java_lang_ModuleLayer$Controller_layout = findMethod(java_lang_ModuleLayer$Controller, "layer");

  private static final Method java_lang_Module_addOpens = findMethod(java_lang_Module, "addOpens", String.class, java_lang_Module);

  private static final Method java_util_Optional_get = findMethod(java_util_Optional, "get");
  private static final Method java_lang_module_ModuleFinder_of = findMethod(java_lang_module_ModuleFinder, "of", Array.newInstance(java_nio_file_Path, 0).getClass());
  private static final Method java_lang_module_ModuleFinder_findAll = findMethod(java_lang_module_ModuleFinder, "findAll");
  private static final Method java_lang_module_ModuleReference_descriptor = findMethod(java_lang_module_ModuleReference, "descriptor");
  private static final Method java_lang_module_ModuleDescriptor_name = findMethod(java_lang_module_ModuleDescriptor, "name");
  private static final Method java_lang_module_Configuration_resolve =
          findMethod(java_lang_module_Configuration, "resolve", java_lang_module_ModuleFinder, List.class, java_lang_module_ModuleFinder, Collection.class);

  private static final Object empyArray_java_nio_file_Path = Array.newInstance(java_nio_file_Path, 0);

  private static final boolean ourConsuloModulePathBoot = Boolean.getBoolean("consulo.module.path.boot");

  private static Object moduleFinderOf(List<File> files) {
    Object paths = Array.newInstance(java_nio_file_Path, files.size());
    for (int i = 0; i < files.size(); i++) {
      File file = files.get(i);

      Array.set(paths, i, instanceInvoke(java_io_File_toPath, file));
    }

    return staticInvoke(java_lang_module_ModuleFinder_of, paths);
  }

  private static Object directFunction(final Object returnValue) {
    return Proxy.newProxyInstance(Java9ModuleInitializer.class.getClassLoader(), new Class[]{java_util_function_Function}, new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("apply".equals(method.getName())) {
          return returnValue;
        }
        throw new UnsupportedOperationException(method.getName());
      }
    });
  }

  /**
   * @return ModuleLayer
   */
  public static Object initializeBaseModules(List<File> files, final ClassLoader targetClassLoader, ContainerLogger containerLogger) {
    Object moduleFinder = moduleFinderOf(files);

    List<String> toResolve = new ArrayList<String>();

    containerLogger.info("Java 9 modules: " + (ourConsuloModulePathBoot ? "enabled" : "disabled"));
    if (ourConsuloModulePathBoot) {
      // we can't enable it, due jvm will initialize all unnamed modules
      //toResolve.add("jakarta.inject");
      toResolve.add("jsr305");

      //toResolve.add("org.apache.commons.compress");
      toResolve.add("org.apache.logging.log4j");
      // consulo internal
      toResolve.add("org.jdom");
      toResolve.add("gnu.trove");
      toResolve.add("kava.beans");
      // google
      //toResolve.add("com.google.common");
      toResolve.add("com.google.gson");

      toResolve.add("consulo.annotation");
      toResolve.add("consulo.logging.api");
      //toResolve.add("consulo.injecting.api");
      toResolve.add("consulo.disposer.api");
      toResolve.add("consulo.localize.api");

      toResolve.add("consulo.hacking.java.base");

      // TODO [VISTALL] this module must be moved to desktop impl
      toResolve.add("consulo.desktop.awt.hacking");

      toResolve.add("consulo.util.lang");
      toResolve.add("consulo.util.collection");
      toResolve.add("consulo.util.rmi");

      //toResolve.add("jakarta.activation");
      // requires java.desktop???
      //toResolve.add("java.xml.bind");

      //toResolve.add("com.sun.jna");
      //toResolve.add("com.sun.jna.platform");

      //toResolve.add("consulo.ui.api.shared");
      //toResolve.add("svg.salamander");
      //toResolve.add("org.slf4j");

      Set findAll = instanceInvoke(java_lang_module_ModuleFinder_findAll, moduleFinder);

      for (Object moduleReference : findAll) {
        Object moduleDescriptor = instanceInvoke(java_lang_module_ModuleReference_descriptor, moduleReference);

        String moduleName = instanceInvoke(java_lang_module_ModuleDescriptor_name, moduleDescriptor);

        if (!toResolve.contains(moduleName)) {
          containerLogger.warn("Module '" + moduleName + "' is not resolved");
        }
      }
    }

    Object bootModuleLayer = staticInvoke(java_lang_ModuleLayer_boot);

    Object confBootModuleLayer = instanceInvoke(java_lang_ModuleLayer_configuration, bootModuleLayer);

    Object configuration = staticInvoke(java_lang_module_Configuration_resolve, moduleFinder, Collections.singletonList(confBootModuleLayer),
                                        staticInvoke(java_lang_module_ModuleFinder_of, empyArray_java_nio_file_Path), toResolve);

    Object functionLambda = directFunction(targetClassLoader);

    Object controller = staticInvoke(java_lang_ModuleLayer_defineModules, configuration, Collections.singletonList(bootModuleLayer), functionLambda);

    if (ourConsuloModulePathBoot) {
      alohomora(bootModuleLayer, controller);
      aberto(bootModuleLayer, controller);
    }

    return instanceInvoke(java_lang_ModuleLayer$Controller_layout, controller);
  }

  public static Object initializeEtcModules(List<Object> moduleLayers, List<File> files, final ClassLoader targetClassLoader) {
    Object moduleFinder = moduleFinderOf(files);

    List<String> toResolve = new ArrayList<String>();

    //TODO [VISTALL] we need resolve all modules, but for now - nothing

    List<Object> layerConfiguration = new ArrayList<Object>(moduleLayers.size());
    for (Object moduleLayer : moduleLayers) {
      layerConfiguration.add(instanceInvoke(java_lang_ModuleLayer_configuration, moduleLayer));
    }

    Object configuration =
            staticInvoke(java_lang_module_Configuration_resolve, moduleFinder, layerConfiguration, staticInvoke(java_lang_module_ModuleFinder_of, empyArray_java_nio_file_Path), toResolve);

    Object functionLambda = directFunction(targetClassLoader);

    Object controller = staticInvoke(java_lang_ModuleLayer_defineModules, configuration, moduleLayers, functionLambda);

    return instanceInvoke(java_lang_ModuleLayer$Controller_layout, controller);
  }

  private static void aberto(Object bootModuleLayer, Object controller) {
    Object javaBaseModule = findModuleUnwrap(bootModuleLayer, "java.base");

    Object plaformModuleLayer = instanceInvoke(java_lang_ModuleLayer$Controller_layout, controller);

    Object hackingJavaBaseModule = findModuleUnwrap(plaformModuleLayer, "consulo.hacking.java.base");

    instanceInvoke(java_lang_Module_addOpens, javaBaseModule, "java.lang", hackingJavaBaseModule);
  }

  private static void alohomora(Object bootModuleLayer, Object controller) {
    Object javaDesktopModule = findModuleUnwrap(bootModuleLayer, "java.desktop");

    Object plaformModuleLayer = instanceInvoke(java_lang_ModuleLayer$Controller_layout, controller);

    Object desktopHackingModule = findModuleUnwrap(plaformModuleLayer, "consulo.desktop.awt.hacking");

    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "sun.awt", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "sun.awt.image", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "sun.java2d", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "sun.font", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "java.awt", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "javax.swing", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "javax.swing.plaf.basic", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "javax.swing.text.html", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "java.awt.peer", desktopHackingModule);
  }

  private static <T> T findModuleUnwrap(Object moduleLayer, String moduleName) {
    Object optionalValue = instanceInvoke(java_lang_ModuleLayer_findModule, moduleLayer, moduleName);

    return instanceInvoke(java_util_Optional_get, optionalValue);
  }

  @SuppressWarnings("unchecked")
  private static <T> T instanceInvoke(Method method, Object instance, Object... args) {
    try {
      return (T)method.invoke(instance, args);
    }
    catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T staticInvoke(Method method, Object... args) {
    try {
      return (T)method.invoke(null, args);
    }
    catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private static Method findMethod(Class<?> cls, String methodName, Class... args) {
    try {
      Method declaredMethod = cls.getDeclaredMethod(methodName, args);
      declaredMethod.setAccessible(true);
      return declaredMethod;
    }
    catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private static Class findClass(String cls) {
    try {
      return Class.forName(cls);
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
