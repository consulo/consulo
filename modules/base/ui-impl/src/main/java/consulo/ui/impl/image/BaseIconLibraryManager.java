/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.impl.image;

import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.util.io.URLUtil;
import consulo.annotation.ReviewAfterMigrationToJRE;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.ui.image.IconLibrary;
import consulo.ui.image.IconLibraryDescriptor;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author VISTALL
 * @since 2020-09-26
 */
public abstract class BaseIconLibraryManager implements IconLibraryManager {
  private static class JarIcon {
    public ThreeState svgState = ThreeState.UNSURE;

    public byte[] _1x;
    public byte[] _2x;
  }

  private static final Logger LOG = Logger.getInstance(BaseIconLibraryManager.class);

  public static final String ICON_DIRECTORY = "icon/";
  public static final String ICON_DIRECTORY_LIB_START = ICON_DIRECTORY + "_";
  public static final String ICON_LIBRARY_MARKER = ICON_DIRECTORY + "marker.txt";

  private final AtomicLong myModificationCount = new AtomicLong();
  private final AtomicBoolean myInitialized = new AtomicBoolean();

  private Map<String, IconLibrary> myLibraries = new HashMap<>();

  private String myActiveLibraryId;
  private BaseIconLibraryImpl myActiveLibrary;

  @Nonnull
  @Override
  public Map<String, IconLibrary> getLibraries() {
    return Collections.unmodifiableMap(myLibraries);
  }

  @Nonnull
  @Override
  public String getActiveLibraryId() {
    return getActiveLibraryId(StyleManager.get().getCurrentStyle());
  }

  @Nonnull
  public String getActiveLibraryId(@Nonnull Style currentStyle) {
    if (myActiveLibraryId == null) {
      return currentStyle.getIconLibraryId();
    }
    return myActiveLibraryId;
  }

  @Override
  public boolean isFromStyle() {
    return myActiveLibraryId == null;
  }

  @Override
  public long getModificationCount() {
    return myModificationCount.get();
  }

  @Override
  public void setActiveLibrary(@Nullable String id) {
    if (id == null) {
      myActiveLibraryId = null;
      myActiveLibrary = null;
      myModificationCount.incrementAndGet();
      return;
    }

    IconLibrary iconLibrary = myLibraries.get(id);
    if (iconLibrary != null) {
      myActiveLibraryId = id;
      myActiveLibrary = (BaseIconLibraryImpl)iconLibrary;
      myModificationCount.incrementAndGet();
    }
    else {
      LOG.error("Can't find icon library with id: " + id);
    }
  }

  @Override
  public void setActiveLibraryFromActiveStyle() {
    myActiveLibraryId = null;
    myActiveLibrary = null;
    myModificationCount.incrementAndGet();
  }

  @Nullable
  public BaseIconLibraryImpl getLibrary(@Nonnull String id) {
    return (BaseIconLibraryImpl)myLibraries.get(id);
  }

  @Override
  @Nonnull
  public BaseIconLibraryImpl getActiveLibrary() {
    if (myActiveLibrary == null) {
      String activeLibraryId = getActiveLibraryId();

      myActiveLibrary = (BaseIconLibraryImpl)myLibraries.get(activeLibraryId);
    }

    if (myActiveLibrary == null) {
      throw new Error("There no default active library. Distribution broken");
    }
    return myActiveLibrary;
  }

  @Nonnull
  protected abstract BaseIconLibraryImpl createLibrary(@Nonnull String id);

  public void initialize(@Nullable List<String> files) {
    if (myInitialized.compareAndSet(false, true)) {
      if (files == null) {
        return;
      }

      Set<String> analyzedGroups = new HashSet<>();

      for (String file : files) {
        try {
          analyzeLibraryJar(file, analyzedGroups);
        }
        catch (IOException e) {
          LOG.error("Fail to analyze library from url: " + file, e);
        }
      }

      Map<String, IconLibraryDescriptor> descriptors = getAllDescriptors();

      for (Map.Entry<String, IconLibrary> entry : myLibraries.entrySet()) {
        String libraryId = entry.getKey();

        BaseIconLibraryImpl iconLibrary = (BaseIconLibraryImpl)entry.getValue();

        IconLibraryDescriptor descriptor = descriptors.get(libraryId);
        if (descriptor != null) {
          iconLibrary.setBaseId(descriptor.getBaseLibraryId());
          iconLibrary.setName(descriptor.getName());
        }
      }
      myModificationCount.incrementAndGet();
    }
  }

  @Nonnull
  @ReviewAfterMigrationToJRE(value = 9, description = "Use consulo.container.plugin.util.PlatformServiceLocator#findImplementation after migration")
  private static Map<String, IconLibraryDescriptor> getAllDescriptors() {
    Map<String, IconLibraryDescriptor> list = new HashMap<>();

    for (IconLibraryDescriptor value : ServiceLoader.load(IconLibraryDescriptor.class, BaseIconLibraryManager.class.getClassLoader())) {
      list.putIfAbsent(value.getLibraryId(), value);
    }

    for (PluginDescriptor descriptor : PluginManager.getEnabledPlugins()) {
      ServiceLoader<IconLibraryDescriptor> loader = ServiceLoader.load(IconLibraryDescriptor.class, descriptor.getPluginClassLoader());

      for (IconLibraryDescriptor libraryDescriptor : loader) {
        list.putIfAbsent(libraryDescriptor.getLibraryId(), libraryDescriptor);
      }
    }

    return list;
  }

  private void analyzeLibraryJar(@Nonnull String filePath, @Nonnull Set<String> analyzedGroups) throws IOException {
    File jarFile = new File(filePath);

    Map<Couple<String>, Map<String, JarIcon>> libraries = new HashMap<>();

    try (ZipFile zipFile = new ZipFile(jarFile)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        try {
          ZipEntry zipEntry = entries.nextElement();

          final String name = zipEntry.getName();

          if (name.startsWith(ICON_DIRECTORY_LIB_START) && !zipEntry.isDirectory() && (name.endsWith("svg") || name.endsWith("png"))) {
            String nameWithoutIcon = name.substring(ICON_DIRECTORY_LIB_START.length(), name.length());

            String libraryId = nameWithoutIcon.substring(0, nameWithoutIcon.indexOf('/'));

            String pathNoLibId = nameWithoutIcon.substring(libraryId.length() + 1, nameWithoutIcon.length());

            String groupId = pathNoLibId.substring(0, pathNoLibId.indexOf('/'));

            String imagePath = pathNoLibId.substring(groupId.length() + 1, pathNoLibId.length());

            Map<String, JarIcon> iconUrls = libraries.computeIfAbsent(Couple.of(libraryId, groupId), it -> new HashMap<>());

            processImage(imagePath, jarFile, zipFile, zipEntry, iconUrls);
          }
        }
        catch (Exception e) {
          LOG.error(e);
        }
      }
    }

    for (Map.Entry<Couple<String>, Map<String, JarIcon>> entry : libraries.entrySet()) {
      Couple<String> key = entry.getKey();
      Map<String, JarIcon> value = entry.getValue();

      String iconLibraryId = key.getFirst();
      String groupId = key.getSecond();

      if (!analyzedGroups.add(iconLibraryId + ":" + groupId)) {
        LOG.error("Can't redefine icon group " + groupId + " in library " + iconLibraryId + ". Path: " + filePath);
        return;
      }

      BaseIconLibraryImpl lib = (BaseIconLibraryImpl)myLibraries.computeIfAbsent(iconLibraryId, it -> createLibrary(iconLibraryId));

      for (Map.Entry<String, JarIcon> image : value.entrySet()) {
        String imageId = image.getKey();
        JarIcon jarIcon = image.getValue();

        if (jarIcon._1x == null) {
          LOG.error("There no x1 scale icon for imageId: " + imageId);
          continue;
        }

        lib.registerIcon(groupId, imageId, jarIcon._1x, jarIcon._2x, jarIcon.svgState.toBoolean());
      }
    }
  }


  private void processImage(@Nonnull String imagePath, @Nonnull File jarFile, @Nonnull ZipFile zipFile, @Nonnull ZipEntry zipEntry, @Nonnull Map<String, JarIcon> iconUrls) throws IOException {
    boolean isSVG = imagePath.endsWith("svg");
    int dotIndex = imagePath.lastIndexOf('.');

    // cut extension plus dot
    String imagePathNoExtension = imagePath.substring(0, dotIndex);

    if (imagePathNoExtension.endsWith("_dark")) {
      LOG.warn("Skipping dark old icon " + URLUtil.getJarEntryURL(jarFile, imagePath));
      return;
    }

    boolean is2x = imagePathNoExtension.endsWith("@2x");
    String iconPath = imagePathNoExtension;
    if (is2x) {
      // +3 - @2x
      iconPath = iconPath.substring(0, iconPath.length() - 3);
    }

    String imageId = iconPath.replace("/", ".").replace("-", "_").toLowerCase(Locale.ROOT);

    JarIcon icon = iconUrls.get(imageId);
    if (icon != null) {
      // if we found svg icon and have not-svg, remove it, we prefer svg
      if (isSVG && icon.svgState == ThreeState.NO) {
        iconUrls.remove(imageId);
      }

      // if we already found svg icon - ignore another, we prefer svg
      if (!isSVG && icon.svgState == ThreeState.YES) {
        return;
      }
    }

    JarIcon jarIcon = iconUrls.computeIfAbsent(imageId, it -> new JarIcon());

    byte[] bytes;
    try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
      bytes = StreamUtil.loadFromStream(inputStream);
    }

    jarIcon.svgState = isSVG ? ThreeState.YES : ThreeState.NO;

    if (is2x) {
      jarIcon._2x = bytes;
    }
    else {
      jarIcon._1x = bytes;
    }
  }

  @Nullable
  public Image getIcon(@Nullable String forceIconLibraryId, String libraryId, String imageId, int width, int height) {
    if (forceIconLibraryId != null) {
      BaseIconLibraryImpl targetIconLibrary = (BaseIconLibraryImpl)myLibraries.get(forceIconLibraryId);
      if (targetIconLibrary != null) {
        Image icon = targetIconLibrary.getIcon(libraryId, imageId, width, height);
        if (icon != null) {
          return icon;
        }
      }
    }
    BaseIconLibraryImpl iconLibrary = getActiveLibrary();
    return iconLibrary.getIcon(libraryId, imageId, width, height);
  }
}
