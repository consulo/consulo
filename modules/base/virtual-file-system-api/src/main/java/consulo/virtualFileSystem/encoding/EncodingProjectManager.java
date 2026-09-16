// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.encoding;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.ComponentManager;
import consulo.component.util.ModificationTracker;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;

import java.nio.charset.Charset;
import java.util.Map;

@ServiceAPI(ComponentScope.PROJECT)
public interface EncodingProjectManager extends EncodingManager {
  public static EncodingProjectManager getInstance(ComponentManager project) {
    return project.getInstance(EncodingProjectManager.class);
  }

  /**
   * @return Project encoding name (configured in Settings|File Encodings|Project Encoding) or empty string if it's configured to "System Default"
   */
  @Override
  String getDefaultCharsetName();

  /**
   * @return Project encoding (configured in Settings|File Encodings|Project Encoding)
   */
  @Override
  Charset getDefaultCharset();

  /**
   * Sets Project encoding (configured in Settings|File Encodings|Project Encoding). Use empty string to specify "System Default"
   */
  @Override
  void setDefaultCharsetName(String name);

  
  ModificationTracker getModificationTracker();

  /**
   * @return readonly map of current mappings. to modify mappings use {@link #setMapping(Map)}
   */
  Map<? extends VirtualFile, ? extends Charset> getAllMappings();

  /**
   * Replaces every mapping at once. Callers with more than one directory to configure must use this or
   * {@link #setPointerMapping(Map)} rather than a {@link #setEncoding} per directory: that one starts a modal reload
   * each time, and a modal progress pumps the event queue, so a second queued call re-enters this manager on the UI
   * thread and nests another progress.
   */
  void setMapping(Map<? extends VirtualFile, ? extends Charset> mapping);

  /**
   * @return readonly map of current mappings, keyed by pointer so a caller can merge into it rather than replace it
   */
  Map<? extends VirtualFilePointer, ? extends Charset> getAllPointersMappings();

  /**
   * Replaces every mapping at once, keyed by pointer. Prefer this over {@link #setMapping(Map)} when merging into
   * {@link #getAllPointersMappings()}, so directories which no longer resolve to a file keep their encoding.
   */
  @RequiredUIAccess
  void setPointerMapping(Map<? extends VirtualFilePointer, ? extends Charset> mapping);
}
