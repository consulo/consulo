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
package consulo.desktop.awt.boot.main;

import consulo.container.impl.classloader.Java9ModuleProcessor;

import java.util.List;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 27/04/2021
 */
public class DesktopAwtJava9Processor implements Java9ModuleProcessor {
  public static final DesktopAwtJava9Processor INSTANCE = new DesktopAwtJava9Processor();

  @Override
  public void process(List<Opens> toOpenMap) {
    toOpenMap.add(new Opens("java.base", "java.lang", "consulo.hacking.java.base"));

    toOpenMap.add(new Opens("java.desktop", "sun.awt", "consulo.desktop.awt.hacking"));
    toOpenMap.add(new Opens("java.desktop", "sun.swing", "consulo.desktop.awt.hacking"));
    toOpenMap.add(new Opens("java.desktop", "sun.awt.image", "consulo.desktop.awt.hacking"));
    toOpenMap.add(new Opens("java.desktop", "sun.java2d", "consulo.desktop.awt.hacking"));
    toOpenMap.add(new Opens("java.desktop", "sun.font", "consulo.desktop.awt.hacking"));
    toOpenMap.add(new Opens("java.desktop", "java.awt", "consulo.desktop.awt.hacking"));
    toOpenMap.add(new Opens("java.desktop", "javax.swing", "consulo.desktop.awt.hacking"));
    toOpenMap.add(new Opens("java.desktop", "javax.swing.plaf.basic", "consulo.desktop.awt.hacking"));
    toOpenMap.add(new Opens("java.desktop", "javax.swing.text.html", "consulo.desktop.awt.hacking"));
    toOpenMap.add(new Opens("java.desktop", "java.awt.peer", "consulo.desktop.awt.hacking"));
    toOpenMap.add(new Opens("java.desktop", "java.awt.event", "consulo.desktop.awt.hacking"));

    boolean isMac = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("mac");
    if (isMac) {
      toOpenMap.add(new Opens("java.desktop", "com.apple.laf", "consulo.desktop.awt.hacking"));
      toOpenMap.add(new Opens("java.desktop", "com.apple.eawt", "consulo.desktop.awt.eawt.wrapper"));
      toOpenMap.add(new Opens("java.desktop", "com.apple.eawt.event", "consulo.desktop.awt.eawt.wrapper"));
    }
  }

  @Override
  public boolean isEnabledModules() {
    return System.getProperty("jdk.module.path") != null;
  }
}
