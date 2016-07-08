/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.ide.customize;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.ide.ui.laf.intellij.IntelliJLaf;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.io.DownloadUtil;
import com.intellij.util.ui.UIUtil;
import lombok.val;
import consulo.lombok.annotations.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

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
@Logger
public class CustomizeUtil {
  private static final String TEMPLATES_URL = "https://github.com/consulo/consulo-firststart-templates/archive/master.zip";

  public static void show(boolean initLaf) {
    if (initLaf) {
      initLaf();
    }

    val downloadDialog = new CustomizeDownloadDialog();

    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        val pluginDescriptors = new MultiMap<String, IdeaPluginDescriptor>();
        val predefinedTemplateSets = new MultiMap<String, String>();
        try {
          List<IdeaPluginDescriptor> ideaPluginDescriptors = RepositoryHelper.loadPluginsFromRepository(null);
          for (IdeaPluginDescriptor ideaPluginDescriptor : ideaPluginDescriptors) {
            pluginDescriptors.putValue(ideaPluginDescriptor.getCategory(), ideaPluginDescriptor);
          }
          loadPredefinedTemplateSets(predefinedTemplateSets);
        }
        catch (Exception e) {
          LOGGER.warn(e);
        }

        UIUtil.invokeLaterIfNeeded(new Runnable() {
          @Override
          public void run() {
            downloadDialog.close(0);
            new CustomizeIDEWizardDialog(pluginDescriptors, predefinedTemplateSets).show();
          }
        });
      }
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

      ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
      try {
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
      finally {
        zipInputStream.close();
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
    String className = null;
    if (SystemInfo.isMac) {
      className = "com.apple.laf.AquaLookAndFeel";
    }
    else {
      className = IntelliJLaf.class.getName();
    }
    try {
      UIManager.setLookAndFeel(className);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

}
