/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.fileTemplate.impl.internal;

import consulo.container.boot.ContainerPathManager;
import consulo.container.plugin.PluginManager;
import consulo.fileTemplate.FileTemplateManager;
import consulo.language.file.FileTypeManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.io.UriUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

/**
 * Serves as a container for all existing template manager types and loads corresponding templates upon creation (at construction time).
 *
 * @author Rustam Vishnyakov
 */
public class FileTemplatesLoader {
  private static final Logger LOG = Logger.getInstance(FileTemplatesLoader.class);

  static final String TEMPLATES_DIR = "fileTemplates";
  private static final String DEFAULT_TEMPLATES_ROOT = TEMPLATES_DIR;
  public static final String DESCRIPTION_FILE_EXTENSION = "html";
  private static final String DESCRIPTION_EXTENSION_SUFFIX = "." + DESCRIPTION_FILE_EXTENSION;
  //static final String DESCRIPTION_FILE_NAME = "default." + DESCRIPTION_FILE_EXTENSION;

  private final FTManager myDefaultTemplatesManager;
  private final FTManager myInternalTemplatesManager;
  private final FTManager myPatternsManager;
  private final FTManager myCodeTemplatesManager;
  private final FTManager myJ2eeTemplatesManager;

  private final Map<String, FTManager> myDirToManagerMap = new HashMap<>();
  private final FTManager[] myAllManagers;

  private static final String INTERNAL_DIR = "internal";
  private static final String INCLUDES_DIR = "includes";
  private static final String CODE_TEMPLATES_DIR = "code";
  private static final String J2EE_TEMPLATES_DIR = "j2ee";

  private URL myDefaultTemplateDescription;
  private URL myDefaultIncludeDescription;

  protected FileTemplatesLoader(@Nonnull FileTypeManager typeManager, @Nullable Project project) {
    File configDir = project == null || project.isDefault()
                     ? new File(ContainerPathManager.get().getConfigPath(), TEMPLATES_DIR)
                     : new File(UriUtil.trimTrailingSlashes(project.getBaseDir() + "/" + Project.DIRECTORY_STORE_FOLDER + "/" + TEMPLATES_DIR));
    myDefaultTemplatesManager = new FTManager(FileTemplateManager.DEFAULT_TEMPLATES_CATEGORY, configDir);
    myInternalTemplatesManager = new FTManager(FileTemplateManager.INTERNAL_TEMPLATES_CATEGORY, new File(configDir, INTERNAL_DIR), true);
    myPatternsManager = new FTManager(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY, new File(configDir, INCLUDES_DIR));
    myCodeTemplatesManager = new FTManager(FileTemplateManager.CODE_TEMPLATES_CATEGORY, new File(configDir, CODE_TEMPLATES_DIR));
    myJ2eeTemplatesManager = new FTManager(FileTemplateManager.J2EE_TEMPLATES_CATEGORY, new File(configDir, J2EE_TEMPLATES_DIR));
    myAllManagers = new FTManager[]{myDefaultTemplatesManager, myInternalTemplatesManager, myPatternsManager, myCodeTemplatesManager, myJ2eeTemplatesManager};

    myDirToManagerMap.put("", myDefaultTemplatesManager);
    myDirToManagerMap.put(INTERNAL_DIR + "/", myInternalTemplatesManager);
    myDirToManagerMap.put(INCLUDES_DIR + "/", myPatternsManager);
    myDirToManagerMap.put(CODE_TEMPLATES_DIR + "/", myCodeTemplatesManager);
    myDirToManagerMap.put(J2EE_TEMPLATES_DIR + "/", myJ2eeTemplatesManager);

    loadDefaultTemplates();
    for (FTManager manager : myAllManagers) {
      manager.loadCustomizedContent();
    }
  }

  @Nonnull
  public FTManager[] getAllManagers() {
    return myAllManagers;
  }

  @Nonnull
  public FTManager getDefaultTemplatesManager() {
    return new FTManager(myDefaultTemplatesManager);
  }

  @Nonnull
  public FTManager getInternalTemplatesManager() {
    return new FTManager(myInternalTemplatesManager);
  }

  public FTManager getPatternsManager() {
    return new FTManager(myPatternsManager);
  }

  public FTManager getCodeTemplatesManager() {
    return new FTManager(myCodeTemplatesManager);
  }

  public FTManager getJ2eeTemplatesManager() {
    return new FTManager(myJ2eeTemplatesManager);
  }

  public URL getDefaultTemplateDescription() {
    return myDefaultTemplateDescription;
  }

  public URL getDefaultIncludeDescription() {
    return myDefaultIncludeDescription;
  }

  private void loadDefaultTemplates() {
    final Set<URL> processedUrls = new HashSet<>();
    PluginManager.forEachEnabledPlugin(plugin -> {
      final ClassLoader loader = plugin.getPluginClassLoader();
      try {
        final Enumeration<URL> systemResources = loader.getResources(DEFAULT_TEMPLATES_ROOT);
        if (systemResources.hasMoreElements()) {
          while (systemResources.hasMoreElements()) {
            final URL url = systemResources.nextElement();
            if (processedUrls.contains(url)) {
              continue;
            }
            processedUrls.add(url);
            loadDefaultsFromRoot(url);
          }
        }
      }
      catch (IOException e) {
        LOG.error(e);
      }
    });
  }

  private void loadDefaultsFromRoot(final URL root) throws IOException {
    final List<String> children = UrlUtil.getChildrenRelativePaths(root);
    if (children.isEmpty()) {
      return;
    }
    final Set<String> descriptionPaths = new HashSet<>();
    for (String path : children) {
      if (path.equals("default.html")) {
        myDefaultTemplateDescription = URLUtil.internProtocol(new URL(UriUtil.trimTrailingSlashes(root.toExternalForm()) + "/" + path));
      }
      else if (path.equals("includes/default.html")) {
        myDefaultIncludeDescription = URLUtil.internProtocol(new URL(UriUtil.trimTrailingSlashes(root.toExternalForm()) + "/" + path));
      }
      else if (path.endsWith(DESCRIPTION_EXTENSION_SUFFIX)) {
        descriptionPaths.add(path);
      }
    }
    for (final String path : children) {
      for (Map.Entry<String, FTManager> entry : myDirToManagerMap.entrySet()) {
        final String prefix = entry.getKey();
        if (matchesPrefix(path, prefix)) {
          if (path.endsWith(FTManager.TEMPLATE_EXTENSION_SUFFIX)) {
            final String filename = path.substring(prefix.length(), path.length() - FTManager.TEMPLATE_EXTENSION_SUFFIX.length());
            final String extension = FileUtil.getExtension(filename);
            final String templateName = filename.substring(0, filename.length() - extension.length() - 1);
            final URL templateUrl = URLUtil.internProtocol(new URL(UriUtil.trimTrailingSlashes(root.toExternalForm()) + "/" + path));
            final String descriptionPath = getDescriptionPath(prefix, templateName, extension, descriptionPaths);
            final URL descriptionUrl = descriptionPath == null ? null : URLUtil.internProtocol(new URL(UriUtil.trimTrailingSlashes(root.toExternalForm()) + "/" + descriptionPath));
            assert templateUrl != null;
            entry.getValue().addDefaultTemplate(new DefaultTemplate(templateName, extension, templateUrl, descriptionUrl));
          }
          break; // FTManagers loop
        }
      }
    }
  }

  private static boolean matchesPrefix(String path, String prefix) {
    if (prefix.length() == 0) {
      return !path.contains("/");
    }
    return FileUtil.startsWith(path, prefix) && !path.substring(prefix.length()).contains("/");
  }

  //Example: templateName="NewClass"   templateExtension="java"
  @Nullable
  private static String getDescriptionPath(String pathPrefix, String templateName, String templateExtension, Set<String> descriptionPaths) {
    final Locale locale = Locale.getDefault();

    String descName = MessageFormat.format("{0}.{1}_{2}_{3}" + DESCRIPTION_EXTENSION_SUFFIX, templateName, templateExtension, locale.getLanguage(), locale.getCountry());
    String descPath = pathPrefix.length() > 0 ? pathPrefix + descName : descName;
    if (descriptionPaths.contains(descPath)) {
      return descPath;
    }

    descName = MessageFormat.format("{0}.{1}_{2}" + DESCRIPTION_EXTENSION_SUFFIX, templateName, templateExtension, locale.getLanguage());
    descPath = pathPrefix.length() > 0 ? pathPrefix + descName : descName;
    if (descriptionPaths.contains(descPath)) {
      return descPath;
    }

    descName = templateName + "." + templateExtension + DESCRIPTION_EXTENSION_SUFFIX;
    descPath = pathPrefix.length() > 0 ? pathPrefix + descName : descName;
    if (descriptionPaths.contains(descPath)) {
      return descPath;
    }
    return null;
  }
}
