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

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.impl.stores.StateStorageManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import com.intellij.util.io.UnsyncByteArrayOutputStream;
import consulo.externalStorage.ExternalStorageUtil;
import org.iq80.snappy.SnappyInputStream;
import org.iq80.snappy.SnappyOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 12-Feb-17
 */
public class ExternalStorage {
  private static final Logger LOGGER = Logger.getInstance(ExternalStorage.class);

  private File myProxyDirectory;

  private final ExternalStorageQueue myQueue = new ExternalStorageQueue();

  public ExternalStorage() {
    myProxyDirectory = new File(PathManager.getSystemPath(), "externalStorage");
  }

  @Nullable
  public InputStream loadContent(String fileSpec, RoamingType roamingType, StateStorageManager stateStorageManager) throws IOException {
    Ref<byte[]> ref = myQueue.getContent(fileSpec, roamingType);
    if (ref != null) {
      byte[] bytes = ref.get();
      return bytes == null ? null : new SnappyInputStream(new UnsyncByteArrayInputStream(bytes));
    }

    InputStream stream = null;
    int mod = -1;
    File file = new File(myProxyDirectory, buildFileSpec(roamingType, fileSpec));
    if (file.exists()) {
      stream = new SnappyInputStream(new FileInputStream(file));

      File modFile = ExternalStorageUtil.getModCountFile(file);
      if (modFile.exists()) {
        try {
          mod = Integer.parseInt(FileUtil.loadFile(modFile));
        }
        catch (IOException ignored) {
        }
      }
    }

    myQueue.wantLoad(fileSpec, roamingType, mod, stateStorageManager);
    return stream;
  }

  public void saveContent(@NotNull String fileSpec, @NotNull RoamingType roamingType, byte[] content, int size) throws IOException {
    UnsyncByteArrayOutputStream out = new UnsyncByteArrayOutputStream(size);
    try (SnappyOutputStream snappyOutputStream = new SnappyOutputStream(out)) {
      snappyOutputStream.write(content, 0, size);
    }

    byte[] compressedContent = out.toByteArray();

    myQueue.wantSave(myProxyDirectory, fileSpec, roamingType, compressedContent);
  }

  @NotNull
  public Collection<String> listSubFiles(@NotNull String fileSpec, @NotNull RoamingType roamingType) {
    fileSpec = buildFileSpec(roamingType, fileSpec);

    File proxy = new File(myProxyDirectory, fileSpec);
    if (proxy.isDirectory() && proxy.isDirectory()) {
      return Arrays.asList(proxy.list());
    }
    return Collections.emptyList();
  }

  public void delete(@NotNull String fileSpec, @NotNull RoamingType roamingType) {
    fileSpec = buildFileSpec(roamingType, fileSpec);

    File file = new File(myProxyDirectory, fileSpec);
    file.delete();
  }

  @NotNull
  public static String buildFileSpec(@NotNull RoamingType roamingType, @NotNull String fileSpec) {
    switch (roamingType) {
      case PER_PLATFORM:
        return "$OS$/" + getOsPrefix() + "/" + fileSpec;
      case PER_USER:
        return "$GLOBAL$/" + fileSpec;
      default:
        throw new UnsupportedOperationException(roamingType.name());
    }
  }

  @NotNull
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
