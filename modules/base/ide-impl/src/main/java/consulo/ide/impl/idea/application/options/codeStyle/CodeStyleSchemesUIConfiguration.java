/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options.codeStyle;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Rustam Vishnyakov
 */
@Singleton
@State(name = "CodeStyleSchemesUIConfiguration", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")})
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class CodeStyleSchemesUIConfiguration implements PersistentStateComponent<CodeStyleSchemesUIConfiguration> {

  public String RECENT_IMPORT_FILE_LOCATION = "";

  @Nullable
  @Override
  public CodeStyleSchemesUIConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(CodeStyleSchemesUIConfiguration state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public static CodeStyleSchemesUIConfiguration getInstance() {
    return ServiceManager.getService(CodeStyleSchemesUIConfiguration.class);
  }

  public static class Util {
    @Nullable
    public static VirtualFile getRecentImportFile() {
      CodeStyleSchemesUIConfiguration configuration = getInstance();
      if (configuration != null) {
        String fileLocation = configuration.RECENT_IMPORT_FILE_LOCATION;
        if (fileLocation == null || fileLocation.trim().isEmpty()) return null;
        try {
          URL url = new URL(fileLocation);
          return VfsUtil.findFileByURL(url);
        }
        catch (MalformedURLException e) {
          // Ignore
        }
      }
      return null;
    }

    public static void setRecentImportFile(@Nonnull VirtualFile recentFile) {
      CodeStyleSchemesUIConfiguration configuration = getInstance();
      if (configuration != null) {
        URL url = VfsUtil.convertToURL(recentFile.getUrl());
        if (url != null) {
          configuration.RECENT_IMPORT_FILE_LOCATION = url.toString();
        }
      }
    }
  }
}
