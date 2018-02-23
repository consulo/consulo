/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.server;

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
import consulo.compiler.server.application.CompilerServerApplication;
import consulo.compiler.server.rmi.CompilerClientInterface;
import consulo.compiler.server.rmi.CompilerServerInterface;
import consulo.compiler.server.rmi.impl.CompilerServerInterfaceImpl;
import javax.annotation.Nonnull;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * @author VISTALL
 * @since 5:48/09.08.13
 */
public class Main {
  private static final Logger LOGGER;

  static {
    Logger.setFactory(CompilerServerLoggerFactory.class);
    LOGGER = Logger.getInstance(Main.class);
  }

  public static void main(String[] args) throws Exception {
    File t = FileUtil.createTempDirectory("consulo", "data");
    System.setProperty(PathManager.PROPERTY_CONFIG_PATH, t.getAbsolutePath() + "/config");
    System.setProperty(PathManager.PROPERTY_SYSTEM_PATH, t.getAbsolutePath() + "/system");

    System.setProperty(PathManager.PROPERTY_PLUGINS_PATH, "G:\\target_for_build\\distMain\\plugins");
    System.setProperty(PathManager.PROPERTY_HOME_PATH, "G:\\target_for_build\\distMain");
    System.setProperty("idea.filewatcher.disabled", "true");

    LOGGER.info("Data dir: " + t.getAbsolutePath());

    CompilerServerInterfaceImpl server = createServer();

    ApplicationEx app = CompilerServerApplication.createApplication();
    Messages.setTestDialog(new TestDialog() {
      @Override
      public int show(String message) {
        LOGGER.info(message);
        return 0;
      }
    });

    app.load(PathManager.getOptionsPath());

    setupSdk("JDK", "1.6", "I:\\Programs\\jdk6");
    setupSdk("JDK", "1.7", "I:\\Programs\\jdk7");
    setupSdk("Consulo Plugin SDK", "Consulo 1.SNAPSHOT", "G:\\target_for_build\\distMainn");

    server.compile(new CompilerClientInterface() {
      @Override
      public void addMessage(@Nonnull CompilerMessageCategory category, String message, String url, int lineNum, int columnNum)
        throws RemoteException {
        LOGGER.info(category + ": " + message +  ". Url: " + url);
      }

      @Override
      public void compilationFinished(boolean aborted, int errors, int warnings) throws RemoteException {
      }

      @Nonnull
      @Override
      public String getProjectDir() {
        return "G:\\target_for_build\\consulo";
      }
    });
  }

  private static void setupSdk(String sdkTypeName, String name, String home) {
    SdkType sdkType = null;
    for (SdkType temp : SdkType.EP_NAME.getExtensions()) {
      if (temp.getName().equals(sdkTypeName)) {
        sdkType = temp;
        break;
      }
    }

    assert sdkType != null;
    SdkImpl sdk = new SdkImpl(name, sdkType, home, sdkType.getVersionString(home));

    sdkType.setupSdkPaths(sdk);

    SdkTable.getInstance().addSdk(sdk);
  }

  private static CompilerServerInterfaceImpl createServer() throws Exception {
    final Registry registry = LocateRegistry.createRegistry(5433);

    CompilerServerInterfaceImpl compilerSwapper = new CompilerServerInterfaceImpl();

    registry.rebind(CompilerServerInterface.LOOKUP_ID, compilerSwapper);

    return compilerSwapper;
  }
}
