// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.index.io.data.DataInputOutputUtil;
import consulo.container.boot.ContainerPathManager;

import java.io.*;

class PersistentIndicesConfiguration {
  private static final int INDICES_CONFIGURATION_VERSION = 1;

  static void saveConfiguration() {
    try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indicesConfigurationFile())))) {
      DataInputOutputUtil.writeINT(out, INDICES_CONFIGURATION_VERSION);
      IndexingStamp.savePersistentIndexStamp(out);
    }
    catch (IOException ignored) {
    }
  }

  static void loadConfiguration() {
    try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(indicesConfigurationFile())))) {
      if (DataInputOutputUtil.readINT(in) == INDICES_CONFIGURATION_VERSION) {
        IndexingStamp.initPersistentIndexStamp(in);
      }
    }
    catch (IOException ignored) {
    }
  }

  private static File indicesConfigurationFile() {
    return new File(ContainerPathManager.get().getIndexRoot(), "indices.config");
  }
}
