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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.util.io.URLUtil;
import consulo.logging.Logger;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

  public static final String ICON_LIBRARY_MARKER = "icon/id.txt";

  private final AtomicLong myModificationCount = new AtomicLong();
  private final AtomicBoolean myInitialized = new AtomicBoolean();

  private Map<String, IconLibrary> myLibraries = new HashMap<>();

  private String myActiveLibraryId;
  private IconLibrary myActiveLibrary;

  @Nonnull
  @Override
  public Set<String> getLibrariesId() {
    return myLibraries.keySet();
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
    if(id == null) {
      myActiveLibraryId = null;
      myActiveLibrary = null;
      myModificationCount.incrementAndGet();
      return;
    }

    IconLibrary iconLibrary = myLibraries.get(id);
    if(iconLibrary != null) {
      myActiveLibraryId = id;
      myActiveLibrary = iconLibrary;
      myModificationCount.incrementAndGet();
    }
    else {
      LOG.error("Can't find icon library with id: " + id);
    }
  }

  @Override
  public void setActiveLibraryFromStyle(@Nonnull Style style) {
    myActiveLibraryId = null;
    myActiveLibrary = null;
    myModificationCount.incrementAndGet();
  }

  @Nullable
  public IconLibrary getLibrary(@Nonnull String id) {
    return myLibraries.get(id);
  }

  @Nonnull
  public IconLibrary getActiveLibrary() {
    if (myActiveLibrary == null) {
      String activeLibraryId = getActiveLibraryId();
      
      myActiveLibrary = myLibraries.get(activeLibraryId);
    }

    if (myActiveLibrary == null) {
      throw new Error("There no default active library. Distribution broken");
    }
    return myActiveLibrary;
  }

  @Nonnull
  protected abstract IconLibrary createLibrary(@Nonnull IconLibraryId id);

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
      myModificationCount.incrementAndGet();
    }
  }

  private void analyzeLibraryJar(@Nonnull String filePath, @Nonnull Set<String> analyzedGroups) throws IOException {
    String libraryText = null;
    File jarFile = new File(filePath);

    Map<String, JarIcon> iconUrls = new HashMap<>();

    try (ZipFile zipFile = new ZipFile(jarFile)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = entries.nextElement();

        final String name = zipEntry.getName();

        if (ICON_LIBRARY_MARKER.equals(name)) {
          try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
            byte[] bytes = StreamUtil.loadFromStream(inputStream);

            libraryText = new String(bytes, StandardCharsets.UTF_8);
          }
        }
        else {
          if (!processMaybeIconFile(name, jarFile, zipFile, zipEntry, iconUrls, "svg", true)) {
            processMaybeIconFile(name, jarFile, zipFile, zipEntry, iconUrls, "png", false);
          }
        }
      }
    }

    if (StringUtil.isEmptyOrSpaces(libraryText)) {
      LOG.error("Icon library identificator not set " + libraryText);
      return;
    }

    String[] split = libraryText.split(":");
    IconLibraryId iconLibraryId = extractName(split[0]);
    String groupId = split[1];

    String prefix = "icon/" + groupId.replace(".", "/") + "/";

    if (!analyzedGroups.add(iconLibraryId.getId() + ":" + groupId)) {
      LOG.error("Can't redefine icon group " + groupId + " in library " + iconLibraryId.getId() + ". Path: " + filePath);
      return;
    }

    IconLibrary lib = myLibraries.computeIfAbsent(iconLibraryId.getId(), it -> createLibrary(iconLibraryId));

    for (Map.Entry<String, JarIcon> entry : iconUrls.entrySet()) {
      String iconPath = entry.getKey();
      JarIcon value = entry.getValue();

      if (iconPath.startsWith(prefix)) {
        if (value._1x == null) {
          LOG.error("There no 1.0 scale icon for path " + iconPath + ". Skipping");
          continue;
        }

        String imagePathNoExtension = iconPath.substring(prefix.length(), iconPath.length());

        String imageId = imagePathNoExtension.replace("/", ".").replace("-", "_");

        lib.registerIcon(groupId, imageId, value._1x, value._2x, value.svgState.toBoolean());
      }
    }
  }

  @Nonnull
  private IconLibraryId extractName(@Nonnull String nameFull) {
    if(nameFull.contains(">")) {
      String[] split = nameFull.split(">");
      return new IconLibraryId(split[0], split[1]);
    }
    return new IconLibraryId(nameFull, null);
  }

  private boolean processMaybeIconFile(@Nonnull final String fileName,
                                       @Nonnull File jarFile,
                                       @Nonnull ZipFile zipFile,
                                       @Nonnull ZipEntry zipEntry,
                                       @Nonnull Map<String, JarIcon> iconUrls,
                                       @Nonnull String requiredExtension,
                                       boolean isSVG) throws IOException {
    if (!fileName.startsWith("icon/")) {
      return false;
    }

    String extension = FileUtil.getExtension(fileName);
    if (fileName.startsWith("icon/") && extension.equalsIgnoreCase(requiredExtension)) {
      // cut extension plus dot
      String noExtensionPath = fileName.substring(0, fileName.length() - (extension.length() + 1));

      if (noExtensionPath.endsWith("_dark")) {
        LOG.warn("Skipping dark old icon " + URLUtil.getJarEntryURL(jarFile, fileName));
        return true;
      }

      boolean is2x = noExtensionPath.endsWith("@2x");
      String iconPath = noExtensionPath;
      if (is2x) {
        // +3 - @2x
        iconPath = iconPath.substring(0, iconPath.length() - 3);
      }

      JarIcon icon = iconUrls.get(iconPath);
      if (icon != null) {
        // if we found svg icon and have not-svg, remove it, we prefer svg
        if (isSVG && icon.svgState == ThreeState.NO) {
          iconUrls.remove(iconPath);
        }

        // if we already found svg icon - ignore another, we prefer svg
        if (!isSVG && icon.svgState == ThreeState.YES) {
          return true;
        }
      }

      JarIcon jarIcon = iconUrls.computeIfAbsent(iconPath, it -> new JarIcon());

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

      return true;
    }

    return false;
  }

  @Nullable
  public Image getIcon(@Nullable String forceIconLibraryId, String libraryId, String id, int width, int height) {
    if(forceIconLibraryId != null) {
      IconLibrary targetIconLibrary = myLibraries.get(forceIconLibraryId);
      if(targetIconLibrary != null) {
        Image icon = targetIconLibrary.getIcon(libraryId, id, width, height);
        if(icon != null) {
          return icon;
        }
      }
    }
    IconLibrary iconLibrary = getActiveLibrary();
    return iconLibrary.getIcon(libraryId, id, width, height);
  }
}
