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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import consulo.components.impl.stores.storage.StateStorageManager;
import consulo.container.boot.ContainerPathManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
  private File myProxyDirectory;

  private final ExternalStorageQueue myQueue = new ExternalStorageQueue(this);

  public ExternalStorage() {
    myProxyDirectory = new File(ContainerPathManager.get().getSystemPath(), "externalStorage");
  }

  File getProxyDirectory() {
    return myProxyDirectory;
  }

  @Nullable
  public InputStream loadContent(String fileSpec, RoamingType roamingType, StateStorageManager stateStorageManager) throws IOException {
    Ref<byte[]> ref = myQueue.getContent(fileSpec, roamingType);
    if (ref != null) {
      byte[] bytes = ref.get();
      if (bytes == null) {
        return null;
      }
      else {
        Pair<byte[], Integer> pair = DataCompressor.uncompress(new UnsyncByteArrayInputStream(bytes));
        return new UnsyncByteArrayInputStream(pair.getFirst());
      }
    }

    InputStream stream = null;
    int mod = -1;
    File file = new File(myProxyDirectory, buildFileSpec(roamingType, fileSpec));
    if (file.exists()) {
      try (FileInputStream inputStream = new FileInputStream(file)) {
        Pair<byte[], Integer> compressedPair = DataCompressor.uncompress(inputStream);

        stream = new UnsyncByteArrayInputStream(compressedPair.getFirst());
        mod = compressedPair.getSecond();
      }
    }

    myQueue.wantLoad(fileSpec, roamingType, mod, stateStorageManager);
    return stream;
  }

  public void saveContent(@Nonnull String fileSpec, @Nonnull RoamingType roamingType, byte[] content) throws IOException {
    // compress data with -1 mod count - for local, server wull update it after pushing data
    byte[] compress = DataCompressor.compress(content, -1);

    myQueue.wantSaveToServer(myProxyDirectory, fileSpec, roamingType, compress);
  }

  @Nonnull
  public Collection<String> listSubFiles(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    fileSpec = buildFileSpec(roamingType, fileSpec);

    File proxy = new File(myProxyDirectory, fileSpec);
    if (proxy.isDirectory() && proxy.isDirectory()) {
      return Arrays.asList(proxy.list());
    }
    return Collections.emptyList();
  }

  public boolean delete(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    boolean deleted = deleteWithoutServer(fileSpec, roamingType);

    myQueue.deleteFromServer(fileSpec, roamingType);

    return deleted;
  }

  public boolean deleteWithoutServer(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    fileSpec = buildFileSpec(roamingType, fileSpec);

    File file = new File(myProxyDirectory, fileSpec);
    if (file.exists()) {
      return file.delete();
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
