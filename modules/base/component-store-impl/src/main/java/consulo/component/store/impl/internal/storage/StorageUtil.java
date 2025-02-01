/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.component.store.impl.internal.storage;

import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ComponentManager;
import consulo.component.persist.RoamingType;
import consulo.component.persist.StoragePathMacros;
import consulo.component.store.internal.StorageNotificationService;
import consulo.component.store.internal.ReadOnlyModificationException;
import consulo.component.store.internal.StateStorage;
import consulo.component.store.internal.StreamProvider;
import consulo.component.store.internal.TrackingPathMacroSubstitutor;
import consulo.logging.Logger;
import consulo.platform.LineSeparator;
import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.util.io.UnsyncByteArrayOutputStream;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Parent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * @author mike
 */
public class StorageUtil {
  private static final Logger LOG = Logger.getInstance(StorageUtil.class);

    public static final byte[] XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(StandardCharsets.UTF_8);

  private StorageUtil() {
  }

  public static boolean isChangedByStorageOrSaveSession(@Nonnull VirtualFileEvent event) {
    return event.getRequestor() instanceof StateStorage.SaveSession || event.getRequestor() instanceof StateStorage;
  }

  public static void notifyUnknownMacros(@Nonnull TrackingPathMacroSubstitutor substitutor, @Nonnull final ComponentManager project, @Nullable final String componentName) {
    StorageNotificationService.getInstance().notifyUnknownMacros(substitutor, project, componentName);
  }

  public static boolean isEmpty(@Nullable Parent element) {
    if (element == null) {
      return true;
    }
    else if (element instanceof Element) {
      return JDOMUtil.isEmpty((Element)element);
    }
    else {
      Document document = (Document)element;
      return !document.hasRootElement() || JDOMUtil.isEmpty(document.getRootElement());
    }
  }

  @Nonnull
  @RequiredUIAccess
  public static VirtualFile writeFile(@Nullable File file,
                                      @Nonnull Object requestor,
                                      @Nullable VirtualFile lastResolvedFile,
                                      @Nonnull byte[] content,
                                      @Nullable LineSeparator lineSeparatorIfPrependXmlProlog) throws IOException {
    SimpleReference<VirtualFile> vFileRef = SimpleReference.create();
    try {
      return Application.get().runWriteAction((ThrowableComputable<VirtualFile, IOException>)() -> {
        VirtualFile virtualFile = lastResolvedFile;
        vFileRef.set(virtualFile);
        if (file != null && (virtualFile == null || !virtualFile.isValid())) {
          virtualFile = getOrCreateVirtualFile(requestor, file);
        }
        vFileRef.set(virtualFile);
        assert virtualFile != null;
        try (OutputStream out = virtualFile.getOutputStream(requestor)) {
          if (lineSeparatorIfPrependXmlProlog != null) {
            out.write(XML_PROLOG);
            out.write(lineSeparatorIfPrependXmlProlog.getSeparatorBytes());
          }
          out.write(content);
        }
        return virtualFile;
      });

    }
    catch (FileNotFoundException e) {
      VirtualFile virtualFile = vFileRef.get();
      if (virtualFile == null) {
        throw e;
      }
      else {
        throw new ReadOnlyModificationException(file);
      }
    }
  }

  public static void writeFile(@Nullable File file, @Nonnull byte[] content, @Nullable LineSeparator lineSeparatorIfPrependXmlProlog) throws IOException {
    try {

      try (OutputStream out = new FileOutputStream(file)) {
        if (lineSeparatorIfPrependXmlProlog != null) {
          out.write(XML_PROLOG);
          out.write(lineSeparatorIfPrependXmlProlog.getSeparatorBytes());
        }
        out.write(content);
      }
    }
    catch (FileNotFoundException e) {
      throw new ReadOnlyModificationException(file);
    }
  }

  @Nonnull
  public static CompletableFuture<VirtualFile> writeFileAsync(@Nullable File file,
                                                              @Nonnull Object requestor,
                                                              @Nullable final VirtualFile fileRef,
                                                              @Nonnull byte[] content,
                                                              @Nullable LineSeparator lineSeparatorIfPrependXmlProlog) {
    return AccessRule.writeAsync(() -> {
      VirtualFile virtualFile = fileRef;

      if (file != null && (virtualFile == null || !virtualFile.isValid())) {
        virtualFile = getOrCreateVirtualFile(requestor, file);
      }
      assert virtualFile != null;
      try (OutputStream out = virtualFile.getOutputStream(requestor)) {
        if (lineSeparatorIfPrependXmlProlog != null) {
          out.write(XML_PROLOG);
          out.write(lineSeparatorIfPrependXmlProlog.getSeparatorBytes());
        }
        out.write(content);
      }
      return virtualFile;
    });
  }

  public static void deleteFile(@Nonnull File file, @Nonnull Object requestor, @Nullable VirtualFile virtualFile) throws IOException {
    if (virtualFile == null) {
      LOG.warn("Cannot find virtual file " + file.getAbsolutePath());
    }

    if (virtualFile == null) {
      if (file.exists()) {
        FileUtil.delete(file);
      }
    }
    else if (virtualFile.exists()) {
      deleteFile(requestor, virtualFile);
    }
  }

  public static void deleteFile(@Nonnull File file) throws IOException {
    if (file.exists()) {
      FileUtil.delete(file);
    }
  }

  @RequiredUIAccess
  public static void deleteFile(@Nonnull Object requestor, @Nonnull VirtualFile virtualFile) throws IOException {
    try {
      Application.get().runWriteAction((ThrowableComputable<Object, IOException>)() -> {
        virtualFile.delete(requestor);
        return null;
      });
    }
    catch (FileNotFoundException e) {
      throw new ReadOnlyModificationException(VirtualFileUtil.virtualToIoFile(virtualFile));
    }
  }

  @Nonnull
  @Deprecated
  public static byte[] writeToBytes(@Nonnull Parent element, @Nonnull String lineSeparator) throws IOException {
    return writeToBytes(element);
  }

  @Nonnull
  public static byte[] writeToBytes(@Nonnull Parent element) throws IOException {
    UnsyncByteArrayOutputStream out = new UnsyncByteArrayOutputStream(256);
    JDOMUtil.writeParent(element, out, "\n");
    return out.toByteArray();
  }

  @Nonnull
  private static VirtualFile getOrCreateVirtualFile(@Nullable Object requestor, @Nonnull File ioFile) throws IOException {
    VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile);
    if (virtualFile == null) {
      File parentFile = ioFile.getParentFile();
      // need refresh if the directory has just been created
      VirtualFile parentVirtualFile = parentFile == null ? null : LocalFileSystem.getInstance().refreshAndFindFileByIoFile(parentFile);
      if (parentVirtualFile == null) {
        throw new IOException((parentFile == null ? ioFile.getPath() : parentFile.getPath()) + " not found");
      }
      virtualFile = parentVirtualFile.createChildData(requestor, ioFile.getName());
    }
    return virtualFile;
  }

  @Nonnull
  public static LineSeparator detectLineSeparators(@Nonnull CharSequence chars, @Nullable LineSeparator defaultSeparator) {
    for (int i = 0, n = chars.length(); i < n; i++) {
      char c = chars.charAt(i);
      if (c == '\r') {
        return LineSeparator.CRLF;
      }
      else if (c == '\n') {
        // if we are here, there was no \r before
        return LineSeparator.LF;
      }
    }
    return defaultSeparator == null ? Platform.current().os().lineSeparator() : defaultSeparator;
  }

  @Nonnull
  public static byte[] elementToBytes(@Nonnull Parent element, boolean useSystemLineSeparator) throws IOException {
    return writeToBytes(element);
  }

  public static void sendContent(@Nonnull StreamProvider provider, @Nonnull String fileSpec, @Nonnull Parent element, @Nonnull RoamingType type) {
    if (!provider.isApplicable(fileSpec, type)) {
      return;
    }

    try {
      doSendContent(provider, fileSpec, element, type);
    }
    catch (IOException e) {
      LOG.warn(e);
    }
  }

  public static void delete(@Nonnull StreamProvider provider, @Nonnull String fileSpec, @Nonnull RoamingType type) {
    if (provider.isApplicable(fileSpec, type)) {
      provider.delete(fileSpec, type);
    }
  }

  /**
   * You must call {@link StreamProvider#isApplicable(String, RoamingType)} before
   */
  public static void doSendContent(@Nonnull StreamProvider provider, @Nonnull String fileSpec, @Nonnull Parent element, @Nonnull RoamingType type) throws IOException {
    // we should use standard line-separator (\n) - stream provider can share file content on any OS
    byte[] content = elementToBytes(element, false);
    provider.saveContent(fileSpec, content, type);
  }

  public static boolean isProjectOrModuleFile(@Nonnull String fileSpec) {
    return fileSpec.startsWith(StoragePathMacros.PROJECT_CONFIG_DIR);
  }
}
