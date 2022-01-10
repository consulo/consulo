/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.boot.main;

import consulo.container.impl.classloader.Java9ModuleInitializer;
import consulo.container.impl.classloader.Java9ModuleProcessor;

import java.util.List;

import static consulo.container.impl.classloader.Java9ModuleInitializer.*;

/**
 * @author VISTALL
 * @since 27/04/2021
 */
public class DesktopAwtJava9Processor implements Java9ModuleProcessor {
  public static final DesktopAwtJava9Processor INSTANCE = new DesktopAwtJava9Processor();

  @Override
  public void process(Object bootModuleLayer, Object controller) {
    aberto(bootModuleLayer, controller);
    alohomora(bootModuleLayer, controller);
  }

  @Override
  public void addBaseResolveModules(List<String> toResolve) {
    toResolve.add("consulo.desktop.awt.hacking");
  }

  private static void aberto(Object bootModuleLayer, Object controller) {
    Object javaBaseModule = Java9ModuleInitializer.findModuleUnwrap(bootModuleLayer, "java.base");

    Object plaformModuleLayer = instanceInvoke(java_lang_ModuleLayer$Controller_layout, controller);

    Object hackingJavaBaseModule = findModuleUnwrap(plaformModuleLayer, "consulo.hacking.java.base");

    instanceInvoke(java_lang_Module_addOpens, javaBaseModule, "java.lang", hackingJavaBaseModule);
  }

  private static void alohomora(Object bootModuleLayer, Object controller) {
    Object javaDesktopModule = findModuleUnwrap(bootModuleLayer, "java.desktop");

    Object plaformModuleLayer = instanceInvoke(java_lang_ModuleLayer$Controller_layout, controller);

    Object desktopHackingModule = findModuleUnwrap(plaformModuleLayer, "consulo.desktop.awt.hacking");

    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "sun.awt", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "sun.swing", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "sun.awt.image", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "sun.java2d", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "sun.font", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "java.awt", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "javax.swing", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "javax.swing.plaf.basic", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "javax.swing.text.html", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "java.awt.peer", desktopHackingModule);
    instanceInvoke(java_lang_Module_addOpens, javaDesktopModule, "java.awt.event", desktopHackingModule);
  }
}
