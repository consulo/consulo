/*
 * Copyright 2013 Consulo.org
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
package org.consulo.compiler.server;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.util.InvalidDataException;
import org.jdom.JDOMException;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 5:48/09.08.13
 */
public class Main {
  public static void main(String[] args) {
    System.setProperty(PathManager.PROPERTY_CONFIG_PATH, "C:\\Users\\VISTALL\\.Consulo\\config");
    System.setProperty(PathManager.PROPERTY_HOME_PATH, "F:\\github.com\\consulo\\consulo\\out\\artifacts\\dist");

    CompilerServerApplication.getInstance();

    ApplicationEx app = ApplicationManagerEx.getApplicationEx();
    try {
      app.load(PathManager.getOptionsPath());
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (InvalidDataException e) {
      e.printStackTrace();
    }

    try {
      final Project project = ProjectManagerEx.getInstanceEx().loadProject("G:\\consuloTest");

      final ModuleManager instance = ModuleManager.getInstance(project);

      for (Module module : instance.getModules()) {
        System.out.println(module.getName());
      }

      project.dispose();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (JDOMException e) {
      e.printStackTrace();
    }
    catch (InvalidDataException e) {
      e.printStackTrace();
    }
    app.dispose();
  }
}
