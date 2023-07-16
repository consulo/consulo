/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.startup.customize;

import com.google.gson.Gson;
import consulo.application.internal.ApplicationInfo;
import consulo.container.boot.ContainerPathManager;
import consulo.container.plugin.PluginId;
import consulo.externalService.update.UpdateChannel;
import consulo.http.HttpRequests;
import consulo.http.RequestBuilder;
import consulo.ide.impl.externalService.impl.WebServiceApi;
import consulo.ide.impl.idea.ide.plugins.RepositoryHelper;
import consulo.ide.impl.updateSettings.impl.PlatformOrPluginUpdateChecker;
import consulo.ide.util.DownloadUtil;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.io.UnsyncByteArrayInputStream;
import consulo.util.jdom.JDOMUtil;
import org.jdom.Document;
import org.jdom.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author VISTALL
 * @since 09/07/2023
 */
public class FirstTemplateLoader {
  private static final Logger LOG = Logger.getInstance(FirstTemplateLoader.class);

  private static final int IMAGE_SIZE = 100;

  public static CompletableFuture<UpdateChannel> requestPluginChannel() {
    CompletableFuture<UpdateChannel> future = new CompletableFuture<>();

    try {
      PluginId platformId = PlatformOrPluginUpdateChecker.getPlatformPluginId();
      String platfomrVersion = ApplicationInfo.getInstance().getBuild().toString();

      RequestBuilder builder = HttpRequests.request(RepositoryHelper.selectUrlChannel(platformId.getIdString(), platfomrVersion));
      UpdateChannel channel = builder.connect(request -> {
        byte[] bytes = request.readBytes(null);

        SelectChannelResponce responce =
          new Gson().fromJson(new InputStreamReader(new UnsyncByteArrayInputStream(bytes), StandardCharsets.UTF_8),
                              SelectChannelResponce.class);
        return responce.channel;
      });
      future.complete(channel);
    }
    catch (Throwable e) {
      LOG.warn(e);
      future.completeExceptionally(e);
    }
    return future;
  }

  public static CompletableFuture<Map<String, PluginTemplate>> loadPredefinedTemplateSets() {
    CompletableFuture<Map<String, PluginTemplate>> future = new CompletableFuture<>();

    String systemPath = ContainerPathManager.get().getSystemPath();

    File customizeDir = new File(systemPath, "startCustomization");

    FileUtil.delete(customizeDir);
    FileUtil.createDirectory(customizeDir);

    try {
      File zipFile = new File(customizeDir, "remote.zip");

      DownloadUtil.downloadContentToFile(null, WebServiceApi.FIRST_STARTUP_TEMPLATES.buildUrl(), zipFile);

      Map<String, byte[]> datas = new HashMap<>();
      Map<String, URL> images = new HashMap<>();

      try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
        ZipEntry e;
        while ((e = zipInputStream.getNextEntry()) != null) {
          if (e.isDirectory()) {
            continue;
          }

          boolean isSvg = false;
          String name = e.getName();
          if (name.endsWith(".xml") || (isSvg = name.endsWith(".svg"))) {
            byte[] bytes = FileUtil.loadBytes(zipInputStream, (int)e.getSize());

            String templateName = FileUtil.getNameWithoutExtension(onlyFileName(e));
            if (isSvg) {
              images.put(templateName, URLUtil.getJarEntryURL(zipFile, name.replace(" ", "%20")));
            }
            else {
              datas.put(templateName, bytes);
            }
          }
        }
      }

      Map<String, PluginTemplate> map = new LinkedHashMap<>();

      for (Map.Entry<String, byte[]> entry : datas.entrySet()) {
        try {
          String name = entry.getKey();

          Document document = JDOMUtil.loadDocument(new UnsyncByteArrayInputStream(entry.getValue()));

          URL imageUrl = images.get(name);

          Image image = imageUrl == null ? Image.empty(IMAGE_SIZE) : ImageEffects.resize(Image.fromUrl(imageUrl), IMAGE_SIZE, IMAGE_SIZE);

          readPredefinePluginSet(document, name, image, map);
        }
        catch (Exception e) {
          LOG.warn(e);
        }
      }

      future.complete(map);
    }
    catch (Throwable e) {
      LOG.warn(e);
      future.completeExceptionally(e);
    }

    return future;
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

  private static void readPredefinePluginSet(Document document, String setName, Image image, Map<String, PluginTemplate> map) {
    Set<String> pluginIds = new HashSet<>();
    Element rootElement = document.getRootElement();
    String description = rootElement.getChildTextTrim("description");
    for (Element element : rootElement.getChildren("plugin")) {
      String id = element.getAttributeValue("id");
      if (id == null) {
        continue;
      }

      pluginIds.add(id);
    }
    int row = Integer.parseInt(rootElement.getAttributeValue("row", "0"));
    int col = Integer.parseInt(rootElement.getAttributeValue("col", "0"));
    PluginTemplate template = new PluginTemplate(pluginIds, description, image, row, col);
    map.put(setName, template);
  }
}
