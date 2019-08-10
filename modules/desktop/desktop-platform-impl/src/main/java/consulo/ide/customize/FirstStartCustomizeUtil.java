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
package consulo.ide.customize;

import com.intellij.ide.customize.CustomizeIDEWizardDialog;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.PathManager;
import consulo.logging.Logger;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.io.DownloadUtil;
import com.intellij.util.ui.UIUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.ide.updateSettings.UpdateSettings;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author VISTALL
 * @since 27.11.14
 */
public class FirstStartCustomizeUtil {
  public static final Logger LOGGER = Logger.getInstance(FirstStartCustomizeUtil.class);

  private static final String TEMPLATES_URL = "https://github.com/consulo/consulo-firststart-templates/archive/2.0.zip";

  public static void show(boolean initLaf) {
    if (initLaf) {
      initLaf();
    }

    DialogWrapper downloadDialog = new DialogWrapper(false) {
      {
        setResizable(false);
        pack();
        init();
      }

      @Nullable
      @Override
      protected JComponent createSouthPanel() {
        return null;
      }

      @Nullable
      @Override
      protected JComponent createCenterPanel() {
        return new JLabel("Connecting to plugin manager");
      }
    };

    Application.get().executeOnPooledThread(() -> {
      MultiMap<String, PluginDescriptor> pluginDescriptors = new MultiMap<>();
      MultiMap<String, String> predefinedTemplateSets = new MultiMap<>();
      try {
        List<PluginDescriptor> ideaPluginDescriptors = RepositoryHelper.loadOnlyPluginsFromRepository(null, UpdateSettings.getInstance().getChannel());
        for (PluginDescriptor ideaPluginDescriptor : ideaPluginDescriptors) {
          pluginDescriptors.putValue(ideaPluginDescriptor.getCategory(), ideaPluginDescriptor);
        }
        loadPredefinedTemplateSets(predefinedTemplateSets);
      }
      catch (Exception e) {
        LOGGER.warn(e);
      }

      UIUtil.invokeLaterIfNeeded(() -> {
        downloadDialog.close(DialogWrapper.OK_EXIT_CODE);
        new CustomizeIDEWizardDialog(pluginDescriptors, predefinedTemplateSets).show();
      });
    });
    downloadDialog.show();
  }

  public static void loadPredefinedTemplateSets(MultiMap<String, String> predefinedTemplateSets) {

    String systemPath = PathManager.getSystemPath();

    File customizeDir = new File(systemPath, "startCustomization");

    FileUtil.delete(customizeDir);
    FileUtil.createDirectory(customizeDir);

    try {
      File zipFile = new File(customizeDir, "remote.zip");

      DownloadUtil.downloadContentToFile(null, TEMPLATES_URL, zipFile);

      try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
        ZipEntry e;
        while ((e = zipInputStream.getNextEntry()) != null) {
          if (e.isDirectory()) {
            continue;
          }

          byte[] bytes = FileUtil.loadBytes(zipInputStream, (int)e.getSize());

          String name = e.getName();
          if (name.endsWith(".xml")) {
            Document document = JDOMUtil.loadDocument(bytes);

            String onlyFileName = onlyFileName(e);
            readPredefinePluginSet(document, FileUtilRt.getNameWithoutExtension(onlyFileName), predefinedTemplateSets);
          }
        }

      }
      catch (JDOMException e) {
        LOGGER.warn(e);
      }
    }
    catch (IOException e) {
      LOGGER.warn(e);
    }
  }

  private static String onlyFileName(ZipEntry entry) {
    String name = entry.getName();
    int i = name.lastIndexOf('/');
    if (i != -1) {
      return name.substring(i + 1, name.length());
    }
    else {
      return name;
    }
  }

  private static void readPredefinePluginSet(Document document, String setName, MultiMap<String, String> map) {
    for (Element element : document.getRootElement().getChildren()) {
      String id = element.getAttributeValue("id");
      map.putValue(setName, id);
    }
  }

  private static void initLaf() {
    String className;
    if (SystemInfo.isMac) {
      className = "com.apple.laf.AquaLookAndFeel";
    }
    else {
      className = "com.intellij.ide.ui.laf.intellij.IntelliJLaf";
    }
    try {
      UIManager.setLookAndFeel(className);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
