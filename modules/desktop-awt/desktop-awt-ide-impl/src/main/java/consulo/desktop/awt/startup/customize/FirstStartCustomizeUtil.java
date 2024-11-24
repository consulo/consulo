/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.startup.customize;

import com.formdev.flatlaf.FlatLaf;
import consulo.application.Application;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.container.boot.ContainerPathManager;
import consulo.container.plugin.PluginDescriptor;
import consulo.desktop.awt.ui.plaf.LafManagerImpl;
import consulo.externalService.update.UpdateSettings;
import consulo.ide.impl.idea.ide.plugins.RepositoryHelper;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.util.DownloadUtil;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.collection.MultiMap;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.io.UnsyncByteArrayInputStream;
import consulo.util.lang.Couple;
import jakarta.annotation.Nullable;
import org.jdom.Document;
import org.jdom.Element;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author VISTALL
 * @since 2014-11-27
 */
public class FirstStartCustomizeUtil {
    private static final Logger LOG = Logger.getInstance(FirstStartCustomizeUtil.class);

    private static final String TEMPLATES_URL = "https://codeload.github.com/consulo/consulo-firststart-templates/zip/master";

    private static final int IMAGE_SIZE = 100;

    @RequiredUIAccess
    public static void showDialog(boolean initLaf, boolean isDark) {
        if (initLaf) {
            initLaf(isDark);
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
                return new JLabel("Connecting to plugin repository");
            }
        };

        Application.get().executeOnPooledThread(() -> {
            MultiMap<String, PluginDescriptor> pluginDescriptors = new MultiMap<>();
            Map<String, PluginTemplate> predefinedTemplateSets = new TreeMap<>();
            try {
                List<PluginDescriptor> ideaPluginDescriptors = RepositoryHelper.loadOnlyPluginsFromRepository(null,
                    UpdateSettings.getInstance().getChannel(),
                    EarlyAccessProgramManager.getInstance()
                );
                for (PluginDescriptor pluginDescriptor : ideaPluginDescriptors) {
                    Set<String> tags = pluginDescriptor.getTags();
                    if (tags.isEmpty()) {
                        pluginDescriptors.putValue("unknown", pluginDescriptor);
                    }
                    else {
                        for (String tag : tags) {
                            pluginDescriptors.putValue(tag, pluginDescriptor);
                        }
                    }
                }
                loadPredefinedTemplateSets(predefinedTemplateSets);
            }
            catch (Exception e) {
                LOG.warn(e);
            }

            UIUtil.invokeLaterIfNeeded(() -> {
                downloadDialog.close(DialogWrapper.OK_EXIT_CODE);
                new CustomizeIDEWizardDialog(isDark, pluginDescriptors, predefinedTemplateSets).show();
            });
        });
        downloadDialog.showAsync();
    }

    public static void loadPredefinedTemplateSets(Map<String, PluginTemplate> predefinedTemplateSets) {
        String systemPath = ContainerPathManager.get().getSystemPath();

        File customizeDir = new File(systemPath, "startCustomization");

        FileUtil.delete(customizeDir);
        FileUtil.createDirectory(customizeDir);

        try {
            File zipFile = new File(customizeDir, "remote.zip");

            DownloadUtil.downloadContentToFile(null, TEMPLATES_URL, zipFile);

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

            for (Map.Entry<String, byte[]> entry : datas.entrySet()) {
                try {
                    String name = entry.getKey();

                    Document document = JDOMUtil.loadDocument(new UnsyncByteArrayInputStream(entry.getValue()));

                    URL imageUrl = images.get(name);

                    Image image =
                        imageUrl == null ? Image.empty(IMAGE_SIZE) : ImageEffects.resize(Image.fromUrl(imageUrl), IMAGE_SIZE, IMAGE_SIZE);

                    readPredefinePluginSet(document, name, image, predefinedTemplateSets);
                }
                catch (Exception e) {
                    LOG.warn(e);
                }
            }
        }
        catch (IOException e) {
            LOG.warn(e);
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

    private static void initLaf(boolean isDark) {
        try {
            Couple<Class<? extends FlatLaf>> defaultLafs = LafManagerImpl.getDefaultLafs();
            Class<? extends FlatLaf> themeClass = isDark ? defaultLafs.getSecond() : defaultLafs.getFirst();
            UIManager.setLookAndFeel(themeClass.getName());
        }
        catch (Exception ignored) {
        }
    }
}
