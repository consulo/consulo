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

import com.intellij.compiler.impl.ModuleCompileScope;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.vfs.impl.local.FileWatcher;
import org.consulo.compiler.server.application.CompilerServerApplication;
import org.consulo.compiler.server.rmi.CompilerSwapper;
import org.consulo.compiler.server.rmi.CompilerSwapperClient;
import org.consulo.compiler.server.rmi.impl.RemoteCompilerSwapperClientImpl;
import org.consulo.compiler.server.rmi.impl.RemoteCompilerSwapperImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * @author VISTALL
 * @since 5:48/09.08.13
 */
public class Main {
  private static final String PROJECT = "G:\\consuloTest";

  public static void main(String[] args) throws Exception{
    System.setProperty(PathManager.PROPERTY_CONFIG_PATH, "C:\\Users\\VISTALL\\.ConsuloData\\config");
    System.setProperty(PathManager.PROPERTY_SYSTEM_PATH, "C:\\Users\\VISTALL\\.ConsuloData\\system");
    System.setProperty(PathManager.PROPERTY_HOME_PATH, "F:\\github.com\\consulo\\consulo\\out\\artifacts\\dist");
    System.setProperty(FileWatcher.PROPERTY_WATCHER_DISABLED, "true");

    ApplicationEx app = CompilerServerApplication.createApplication();
    Messages.setTestDialog(new TestDialog() {
      @Override
      public int show(String message) {
        System.out.println(message);
        return 0;
      }
    }) ;

    app.load(PathManager.getOptionsPath());

    final Project project = ProjectManagerEx.getInstanceEx().loadProject("G:\\consuloTest");

    final ModuleManager moduleManager = ModuleManager.getInstance(project);


    CompilerManager.getInstance(project).compile(new ModuleCompileScope(moduleManager.getModules()[1], true), null);

   // System.exit(0);

  }

  private static void createServer() throws Exception {
    final Registry registry = LocateRegistry.createRegistry(5433);

    RemoteCompilerSwapperImpl compilerSwapper = new RemoteCompilerSwapperImpl();

    registry.rebind(CompilerSwapper.LOOKUP_ID, compilerSwapper);
  }

  private static void createClient() throws Exception {
    final Registry registry = LocateRegistry.getRegistry("localhost", 5433);

    final CompilerSwapper lookup = (CompilerSwapper) registry.lookup(CompilerSwapper.LOOKUP_ID);

    CompilerSwapperClient client = new RemoteCompilerSwapperClientImpl();

    lookup.compileProject(client, PROJECT);
  }
}
