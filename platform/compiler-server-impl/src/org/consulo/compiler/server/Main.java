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
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.impl.local.FileWatcher;
import org.apache.log4j.Level;
import org.consulo.compiler.server.application.CompilerServerApplication;
import org.consulo.compiler.server.rmi.CompilerClientInterface;
import org.consulo.compiler.server.rmi.CompilerServerInterface;
import org.consulo.compiler.server.rmi.impl.CompilerServerInterfaceImpl;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * @author VISTALL
 * @since 5:48/09.08.13
 */
@org.consulo.lombok.annotations.Logger
public class Main {
  public static void main(String[] args) throws Exception{
    File t = FileUtil.createTempDirectory("consulo", "data");
    System.setProperty(PathManager.PROPERTY_CONFIG_PATH, t.getAbsolutePath() + "/config");
    System.setProperty(PathManager.PROPERTY_SYSTEM_PATH, t.getAbsolutePath() + "/system");

    System.setProperty(PathManager.PROPERTY_PLUGINS_PATH, "G:\\consulo-ext-plugins");
    System.setProperty(PathManager.PROPERTY_HOME_PATH, "F:\\github.com\\consulo\\consulo\\out\\artifacts\\dist");
    System.setProperty(FileWatcher.PROPERTY_WATCHER_DISABLED, "true");

    initLogger();

    CompilerServerInterfaceImpl server = createServer();

    ApplicationEx app = CompilerServerApplication.createApplication();
    Messages.setTestDialog(new TestDialog() {
      @Override
      public int show(String message) {
        LOGGER.info(message);
        return 0;
      }
    }) ;

    app.load(PathManager.getOptionsPath());

    setupSdk("JDK", "1.6", "I:\\Programs\\jdk6");
    setupSdk("Consulo Plugin SDK", "Consulo 1.SNAPSHOT", "F:\\github.com\\consulo\\consulo\\out\\artifacts\\dist");

    server.compile(new CompilerClientInterface() {
      @Override
      public void addMessage(@NotNull CompilerMessageCategory category, String message, String url, int lineNum, int columnNum)
        throws RemoteException {
        System.out.println(category + ": " + message);
      }

      @Override
      public void compilationFinished(boolean aborted, int errors, int warnings) throws RemoteException {
      }

      @NotNull
      @Override
      public String getProjectDir() {
        return "F:\\github.com\\consulo\\consulo-osgi";
      }
    });
  }

  private static void setupSdk(String sdkTypeName, String name, String home) {
    SdkType sdkType = null;
    for (SdkType temp : SdkType.EP_NAME.getExtensions()) {
      if(temp.getName().equals(sdkTypeName)) {
        sdkType = temp;
        break;
      }
    }

    assert sdkType != null;
    SdkImpl sdk = new SdkImpl(name, sdkType, home, sdkType.getVersionString(home));

    sdkType.setupSdkPaths(sdk);

    SdkTable.getInstance().addSdk(sdk);
  }

  private static void initLogger() {
    Logger.setFactory(new Logger.Factory() {
      @Override
      public Logger getLoggerInstance(String category) {
        return new Logger() {
          @Override
          public boolean isDebugEnabled() {
            return false;
          }

          @Override
          public void debug(@NonNls String message) {

          }

          @Override
          public void debug(@Nullable Throwable t) {

          }

          @Override
          public void debug(@NonNls String message, @Nullable Throwable t) {

          }

          @Override
          public void error(@NonNls String message, @Nullable Throwable t, @NonNls String... details) {
            System.out.println(message);
            if(t != null) {
              t.printStackTrace();
            }
          }

          @Override
          public void info(@NonNls String message) {
            System.out.println(message);
          }

          @Override
          public void info(@NonNls String message, @Nullable Throwable t) {
            System.out.println(message);
            if(t != null) {
              t.printStackTrace();
            }
          }

          @Override
          public void warn(@NonNls String message, @Nullable Throwable t) {
            System.out.println(message);
            if(t != null) {
              t.printStackTrace();
            }
          }

          @Override
          public void setLevel(Level level) {
          }
        };
      }
    });
  }

  private static CompilerServerInterfaceImpl createServer() throws Exception {
    final Registry registry = LocateRegistry.createRegistry(5433);

    CompilerServerInterfaceImpl compilerSwapper = new CompilerServerInterfaceImpl();

    registry.rebind(CompilerServerInterface.LOOKUP_ID, compilerSwapper);

    return compilerSwapper;
  }
}
