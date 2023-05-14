// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.file.exclude;

import com.google.common.collect.Sets;
import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.component.persist.PersistentStateComponent;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.plain.PlainTextFileType;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.ide.impl.idea.openapi.vfs.newvfs.impl.CachedFileType;
import consulo.document.util.FileContentUtilCore;
import org.jdom.Attribute;
import org.jdom.Element;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.*;

/**
 * A persistent {@code Set<VirtualFile>} or persistent {@code Map<VirtualFile, String>}
 */
abstract class PersistentFileSetManager implements PersistentStateComponent<Element> {
  private static final String FILE_ELEMENT = "file";
  private static final String URL_ATTR = "url";
  private static final String VALUE_ATTR = "value";

  private final Map<VirtualFile, String> myMap = new HashMap<>();

  boolean addFile(@Nonnull VirtualFile file, @Nonnull FileType type) {
    if (!(file instanceof VirtualFileWithId)) {
      throw new IllegalArgumentException("file must be instanceof VirtualFileWithId but got: " + file + " (" + file.getClass() + ")");
    }
    if (file.isDirectory()) {
      throw new IllegalArgumentException("file must not be directory but got: " + file + "; File.isDirectory():" + new File(file.getPath()).isDirectory());
    }
    String value = type.getName();
    String prevValue = myMap.put(file, value);
    if (!value.equals(prevValue)) {
      onFileSettingsChanged(Collections.singleton(file));
    }
    return true;
  }

  boolean removeFile(@Nonnull VirtualFile file) {
    boolean isRemoved = myMap.remove(file) != null;
    if (isRemoved) {
      onFileSettingsChanged(Collections.singleton(file));
    }
    return isRemoved;
  }

  String getFileValue(@Nonnull VirtualFile file) {
    return myMap.get(file);
  }

  private static void onFileSettingsChanged(@Nonnull Collection<? extends VirtualFile> files) {
    // later because component load could be performed in background
    ApplicationManager.getApplication().invokeLater(() -> {
      WriteAction.run(() -> CachedFileType.clearCache());
      FileContentUtilCore.reparseFiles(files);
    });
  }

  @Nonnull
  Collection<VirtualFile> getFiles() {
    return myMap.keySet();
  }

  @Override
  public Element getState() {
    Element root = new Element("root");
    List<Map.Entry<VirtualFile, String>> sorted = new ArrayList<>(myMap.entrySet());
    sorted.sort(Comparator.comparing(e -> e.getKey().getPath()));
    for (Map.Entry<VirtualFile, String> e : sorted) {
      Element element = new Element(FILE_ELEMENT);
      element.setAttribute(URL_ATTR, VfsUtilCore.pathToUrl(e.getKey().getPath()));
      String fileTypeName = e.getValue();
      if (fileTypeName != null && !PlainTextFileType.INSTANCE.getName().equals(fileTypeName)) {
        element.setAttribute(VALUE_ATTR, fileTypeName);
      }
      root.addContent(element);
    }
    return root;
  }

  @Override
  public void loadState(@Nonnull Element state) {
    Set<VirtualFile> oldFiles = new HashSet<>(getFiles());
    myMap.clear();
    for (Element fileElement : state.getChildren(FILE_ELEMENT)) {
      Attribute urlAttr = fileElement.getAttribute(URL_ATTR);
      if (urlAttr != null) {
        String url = urlAttr.getValue();
        VirtualFile vf = VirtualFileManager.getInstance().findFileByUrl(url);
        if (vf != null) {
          String value = fileElement.getAttributeValue(VALUE_ATTR);
          myMap.put(vf, Objects.requireNonNullElse(value, PlainTextFileType.INSTANCE.getName()));
        }
      }
    }

    Collection<VirtualFile> toReparse = Sets.symmetricDifference(myMap.keySet(), oldFiles);
    onFileSettingsChanged(toReparse);
  }
}
