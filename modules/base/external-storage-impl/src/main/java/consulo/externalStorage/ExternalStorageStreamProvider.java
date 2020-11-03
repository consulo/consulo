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
package consulo.externalStorage;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.RoamingType;
import consulo.components.impl.stores.IApplicationStore;
import consulo.components.impl.stores.storage.StateStorageManager;
import consulo.components.impl.stores.StreamProvider;
import com.intellij.openapi.util.text.StringUtil;
import consulo.externalStorage.storage.ExternalStorage;
import consulo.ide.webService.WebServiceApi;
import consulo.ide.webService.WebServicesConfiguration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 11-Feb-17
 */
public class ExternalStorageStreamProvider extends StreamProvider {
  private final ExternalStorage myStorage;
  private final StateStorageManager myStateStorageManager;

  public ExternalStorageStreamProvider(Application application, IApplicationStore applicationStore) {
    myStorage = new ExternalStorage(applicationStore);
    myStateStorageManager = applicationStore.getStateStorageManager();
  }

  @Override
  public boolean isEnabled() {
    return !StringUtil.isEmpty(WebServicesConfiguration.getInstance().getOAuthKey(WebServiceApi.SYNCHRONIZE_API));
  }

  @Nonnull
  @Override
  public Collection<String> listSubFiles(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    return myStorage.listSubFiles(fileSpec, roamingType);
  }

  @Override
  public void saveContent(@Nonnull String fileSpec, @Nonnull byte[] content, @Nonnull RoamingType roamingType) throws IOException {
    myStorage.saveContent(fileSpec, roamingType, content);
  }

  @Nullable
  @Override
  public InputStream loadContent(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) throws IOException {
    return myStorage.loadContent(fileSpec, roamingType, myStateStorageManager);
  }

  @Override
  public void delete(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    myStorage.delete(fileSpec, roamingType);
  }
}
