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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.vfs.util.ArchiveVfsUtil;
import consulo.compiler.server.application.CompilerServerApplication;

import java.io.File;

/**
 * @author VISTALL
 * @since 0:53/11.09.13
 */
@consulo.lombok.annotations.Logger
public class MainTest {
  public static void main(String[] args) throws Exception{
    File t = FileUtil.createTempDirectory("consulo", "data");
    System.setProperty(PathManager.PROPERTY_CONFIG_PATH, t.getAbsolutePath() + "/config");
    System.setProperty(PathManager.PROPERTY_SYSTEM_PATH, t.getAbsolutePath() + "/system");
    //System.setProperty(PathManager.PROPERTY_CONFIG_PATH, "C:\\Users\\VISTALL\\.ConsuloData\\config");
   // System.setProperty(PathManager.PROPERTY_SYSTEM_PATH, "C:\\Users\\VISTALL\\.ConsuloData\\system");
    System.setProperty(PathManager.PROPERTY_HOME_PATH, "F:\\github.com\\consulo\\consulo\\out\\artifacts\\dist");
    System.setProperty("idea.filewatcher.disabled", "true");

    initLogger();

    ApplicationEx app = CompilerServerApplication.createApplication();
    Messages.setTestDialog(new TestDialog() {
      @Override
      public int show(String message) {
        MainTest.LOGGER.info(message);
        return 0;
      }
    }) ;

    app.load(PathManager.getOptionsPath());

    System.out.println("---------------------------------------------------------------------------------------------------------");

   /* File file = new File("../../../platform/compiler-server-impl/testData/zip1.zip");

    VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);

    VirtualFile jarRootForLocalFile = ArchiveVfsUtil.getJarRootForLocalFile(virtualFile);


    StringBuilder builder = new StringBuilder();
    printTree(jarRootForLocalFile, 0, builder);
    System.out.println(builder);    */

    File file = new File("F:\\github.com\\consulo\\consulo\\out\\artifacts\\dist\\lib\\idea.jar");

    VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);

    VirtualFile archiveRootForLocalFile = ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile);

    System.out.println(archiveRootForLocalFile.findFileByRelativePath("consulo/module/extension/ui"));
  }

  private static void printTree(VirtualFile virtualFile, int indent, StringBuilder builder) {
    builder.append("|");
    builder.append(StringUtil.repeat("-", indent));
    builder.append(" ");
    builder.append(virtualFile.getUrl());
    builder.append("\n");

    for (VirtualFile file : virtualFile.getChildren()) {
      printTree(file, indent + 1, builder);
    }
  }

  private static void initLogger() {
    Logger.setFactory(CompilerServerLoggerFactory.class);
  }
}

