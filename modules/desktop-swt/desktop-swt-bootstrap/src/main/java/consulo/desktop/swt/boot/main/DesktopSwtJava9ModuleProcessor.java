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
package consulo.desktop.swt.boot.main;

import consulo.container.internal.plugin.classloader.Java9ModuleProcessor;

import java.util.List;

/**
 * @author VISTALL
 * @since 18/12/2021
 */
public class DesktopSwtJava9ModuleProcessor implements Java9ModuleProcessor {
  @Override
  public void process(List<Opens> toOpenMap) {
    toOpenMap.add(new Opens("java.base", "java.lang", "consulo.hacking.java.base"));
  }

  @Override
  public boolean isEnabledModules() {
    return System.getProperty("jdk.module.path") != null;
  }
}
