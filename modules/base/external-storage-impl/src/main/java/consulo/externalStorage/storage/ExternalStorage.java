/*
 * Copyright 2013-2017 consulo.io
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
package consulo.externalStorage.storage;

import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.container.boot.ContainerPathManager;
import consulo.externalService.NotFoundException;
import consulo.externalService.impl.WebServiceApi;
import consulo.externalService.impl.WebServiceApiSender;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.logging.Logger;
import consulo.util.jdom.JDOMUtil;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 12-Feb-17
 */
public class ExternalStorage {
  private static final Logger LOG = Logger.getInstance(ExternalStorage.class);

  public static final String INITIALIZED_FILE_NAME = "initialized";
  public static final String MODCOUNT_EXTENSION = ".modcount";

  private final Path myProxyDirectory;

  private Boolean myInitializedState;

  private final ScheduledExecutorService myExecutorService;

  public ExternalStorage() {
    myProxyDirectory = Path.of(ContainerPathManager.get().getSystemPath(), "externalStorage");
    myExecutorService = AppExecutorUtil.createBoundedScheduledExecutorService("External Storage Pool", 1);
  }

  public Map<String, Long> getModificationInfo() throws IOException {
    Map<String, Long> files = new TreeMap<>();

    Files.walkFileTree(myProxyDirectory, new FileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.getFileName().toString().endsWith(MODCOUNT_EXTENSION)) {
          return FileVisitResult.CONTINUE;
        }

        Path modFile = file.getParent().resolve(file.toString() + MODCOUNT_EXTENSION);
        if (Files.exists(modFile)) {
          Path fileSpec = myProxyDirectory.relativize(file);

          files.put(FileUtil.toSystemIndependentName(fileSpec.toString()), Long.parseLong(Files.readString(modFile)));
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
      }
    });
    return files;
  }

  public void wipe() throws IOException {
    if (!FileUtil.delete(myProxyDirectory)) {
      throw new IOException("Failed to remove directory: " + myProxyDirectory);
    }

    Files.createDirectory(myProxyDirectory);
  }

  public void setInitialized(boolean state) {
    try {
      Path initializeFile = myProxyDirectory.resolve(INITIALIZED_FILE_NAME);

      if (state) {
        if (Files.exists(initializeFile)) {
          return;
        }

        Files.writeString(initializeFile, "initialized", StandardCharsets.UTF_8);
      }
      else {
        Files.deleteIfExists(initializeFile);
      }

      resetInitialized();
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  @Nonnull
  public File getInitializedFile() {
    return myProxyDirectory.resolve(INITIALIZED_FILE_NAME).toFile();
  }

  public boolean isInitialized() {
    Boolean state = myInitializedState;
    if (state == null) {
      myInitializedState = state = Files.exists(myProxyDirectory.resolve(INITIALIZED_FILE_NAME));
    }
    return state;
  }

  private void resetInitialized() {
    myInitializedState = null;
  }

  public void writeModCount(String fullFileSpec, long modCount) throws IOException {
    Path filePath = myProxyDirectory.resolve(fullFileSpec + MODCOUNT_EXTENSION);

    Files.writeString(filePath, String.valueOf(modCount), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
  }

  @Nonnull
  public OutputStream openFile(String fileName) throws IOException {
    Path filePath = myProxyDirectory.resolve(fileName);
    if (!Files.exists(filePath)) {
      Files.createDirectories(filePath.getParent());
    }
    return Files.newOutputStream(filePath, StandardOpenOption.CREATE);
  }

  @Nullable
  public InputStream loadContent(String fileSpec, RoamingType roamingType) throws IOException {
    return loadContent(buildFileSpec(roamingType, fileSpec));
  }

  @Nullable
  public InputStream loadContent(@Nonnull String fullFileSpec) throws IOException {
    Path file = myProxyDirectory.resolve(fullFileSpec);
    if (Files.exists(file)) {
      return Files.newInputStream(file);
    }

    return null;
  }

  public void saveContent(@Nonnull String fileSpec, @Nonnull RoamingType roamingType, byte[] content) throws IOException {
    // write data without compress
    writeLocalFile(fileSpec, roamingType, content, -1);

    saveContentOnServer(fileSpec, roamingType, content);
  }

  private void saveContentOnServer(@Nonnull String fileSpec, RoamingType roamingType, byte[] content) {
    myExecutorService.execute(() -> {
      try {
        // compress data with -1 mod count - for local, server will update it after pushing data
        byte[] compressedData = DataCompressor.compress(content, -1);

        String buildFileSpec = ExternalStorage.buildFileSpec(roamingType, fileSpec);

        PushFileRequestBean bean = new PushFileRequestBean(UpdateSettings.getInstance().getChannel(), buildFileSpec, compressedData);

        PushFileResponseBean pushFileResponse = WebServiceApiSender.doPost(WebServiceApi.STORAGE_API, "pushFile", bean, PushFileResponseBean.class);

        assert pushFileResponse != null;

        writeModCount(buildFileSpec, pushFileResponse.modCount);

        LOG.info("Updated file at server: " + buildFileSpec + ", new mod count: " + pushFileResponse.modCount);
      }
      catch (Exception e) {
        LOG.warn(e);
      }
    });
  }

  public void writeLocalFile(@Nonnull String fileSpec, RoamingType roamingType, byte[] data, long modCount) throws IOException {
    String fullFileSpec = ExternalStorage.buildFileSpec(roamingType, fileSpec);

    writeLocalFile(fullFileSpec, data, modCount);
  }

  public void writeLocalFile(@Nonnull String fullFileSpec, byte[] data, long modCount) throws IOException {
    Path file = myProxyDirectory.resolve(fullFileSpec);

    if (!Files.exists(file)) {
      Files.createDirectories(file.getParent());
    }

    Files.write(file, data, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

    if (modCount != -1) {
      writeModCount(fullFileSpec, modCount);
    }
  }

  @Nonnull
  public Collection<String> listSubFiles(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    fileSpec = buildFileSpec(roamingType, fileSpec);

    File proxy = new File(myProxyDirectory.toFile(), fileSpec);
    if (proxy.isDirectory()) {
      return Arrays.stream(proxy.list()).filter(it -> !it.endsWith(MODCOUNT_EXTENSION)).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  public boolean delete(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    boolean deleted = deleteWithoutServer(fileSpec, roamingType);

    deleteFromServer(fileSpec, roamingType);

    return deleted;
  }

  private void deleteFromServer(String fileSpec, RoamingType roamingType) {
    myExecutorService.execute(() -> {
      try {
        String fullFileSpec = ExternalStorage.buildFileSpec(roamingType, fileSpec);

        LOG.info("Deleting file: " + fullFileSpec);

        WebServiceApiSender.doGet(WebServiceApi.STORAGE_API, "deleteFile", Map.of("filePath", fullFileSpec), Object.class);
      }
      catch (NotFoundException ignored) {
      }
      catch (Exception e) {
        LOG.warn(e);
      }
    });
  }

  public boolean deleteWithoutServer(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    String fullFileSpec = buildFileSpec(roamingType, fileSpec);

    return deleteWithoutServer(fullFileSpec);
  }

  public boolean deleteWithoutServer(@Nonnull String fullFileSpec) {
    LOG.info("Removing local file: " + fullFileSpec);

    Path file = myProxyDirectory.resolve(fullFileSpec);
    if (Files.exists(file)) {
      try {
        return Files.deleteIfExists(file);
      }
      catch (IOException ignored) {
      }
    }

    Path modCount = myProxyDirectory.resolve(fullFileSpec + MODCOUNT_EXTENSION);
    try {
      Files.deleteIfExists(modCount);
    }
    catch (IOException ignored) {
    }
    return false;
  }

  @Nonnull
  public static String buildFileSpec(@Nonnull RoamingType roamingType, @Nonnull String fileSpec) {
    switch (roamingType) {
      case PER_PLATFORM:
        return "$OS$/" + getOsPrefix() + "/" + fileSpec;
      case PER_USER:
        return "$GLOBAL$/" + fileSpec;
      default:
        throw new UnsupportedOperationException(roamingType.name());
    }
  }

  @Nonnull
  public static Set<String> readComponentNames(@Nonnull InputStream stream) throws IOException, JDOMException {
    Set<String> names = new TreeSet<>();
    Element rootElement = JDOMUtil.load(stream);

    for (Element childElement : rootElement.getChildren("component")) {
      String name = childElement.getAttributeValue("name");

      if (name != null) {
        names.add(name);
      }
    }

    return names;
  }

  @Nonnull
  private static String getOsPrefix() {
    if (SystemInfo.isWindows) {
      return "win";
    }
    else if (SystemInfo.isMac) {
      return "mac";
    }
    else if (SystemInfo.isLinux) {
      return "linux";
    }
    else if (SystemInfo.isFreeBSD) {
      return "bsd";
    }
    else if (SystemInfo.isUnix) {
      return "unix";
    }
    return "other";
  }
}
