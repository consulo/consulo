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
package consulo.components.impl.stores;

import com.intellij.notification.NotificationType;
import com.intellij.notification.NotificationsManager;
import consulo.application.Application;
import consulo.component.persist.RoamingType;
import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import consulo.project.Project;
import consulo.project.ProjectBundle;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.util.JDOMUtil;
import consulo.application.util.function.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import com.intellij.util.LineSeparator;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.UnsyncByteArrayOutputStream;
import consulo.application.ui.awt.UIUtil;
import consulo.application.AccessRule;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.ref.SimpleReference;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Parent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author mike
 */
public class StorageUtil {
  private static final Logger LOG = Logger.getInstance(StorageUtil.class);

  private static final byte[] XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(StandardCharsets.UTF_8);

  private StorageUtil() {
  }

  public static boolean isChangedByStorageOrSaveSession(@Nonnull VirtualFileEvent event) {
    return event.getRequestor() instanceof StateStorage.SaveSession || event.getRequestor() instanceof StateStorage;
  }

  public static void notifyUnknownMacros(@Nonnull TrackingPathMacroSubstitutor substitutor, @Nonnull final Project project, @Nullable final String componentName) {
    final LinkedHashSet<String> macros = new LinkedHashSet<String>(substitutor.getUnknownMacros(componentName));
    if (macros.isEmpty()) {
      return;
    }

    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        macros.removeAll(getMacrosFromExistingNotifications(project));

        if (!macros.isEmpty()) {
          LOG.debug("Reporting unknown path macros " + macros + " in component " + componentName);
          String format = "<p><i>%s</i> %s undefined. <a href=\"define\">Fix it</a></p>";
          String productName = Application.get().getName().get();
          String content = String.format(format, StringUtil.join(macros, ", "), macros.size() == 1 ? "is" : "are") +
                           "<br>Path variables are used to substitute absolute paths " +
                           "in " +
                           productName +
                           " project files " +
                           "and allow project file sharing in version control systems.<br>" +
                           "Some of the files describing the current project settings contain unknown path variables " +
                           "and " +
                           productName +
                           " cannot restore those paths.";
          new UnknownMacroNotification("Load Error", "Load error: undefined path variables", content, NotificationType.ERROR,
                                       (notification, event) -> ProjectStorageUtil.checkUnknownMacros((ProjectEx)project, true), macros).notify(project);
        }
      }
    });
  }

  private static List<String> getMacrosFromExistingNotifications(Project project) {
    List<String> notified = ContainerUtil.newArrayList();
    NotificationsManager manager = NotificationsManager.getNotificationsManager();
    for (final UnknownMacroNotification notification : manager.getNotificationsOfType(UnknownMacroNotification.class, project)) {
      notified.addAll(notification.getMacros());
    }
    return notified;
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
  public static AsyncResult<VirtualFile> writeFileAsync(@Nullable File file,
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
      throw new ReadOnlyModificationException(VfsUtil.virtualToIoFile(virtualFile));
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
        throw new IOException(ProjectBundle.message("project.configuration.save.file.not.found", parentFile == null ? ioFile.getPath() : parentFile.getPath()));
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
    return defaultSeparator == null ? LineSeparator.getSystemLineSeparator() : defaultSeparator;
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

  @Nonnull
  public static String getStoreDir(@Nonnull Project project) {
    return project.getBasePath() + "/" + Project.DIRECTORY_STORE_FOLDER;
  }
}
