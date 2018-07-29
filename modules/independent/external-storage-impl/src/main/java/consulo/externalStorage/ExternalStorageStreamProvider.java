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

import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.impl.stores.IApplicationStore;
import com.intellij.openapi.components.impl.stores.StreamProvider;
import com.intellij.openapi.util.text.StringUtil;
import consulo.externalStorage.storage.ExternalStorage;
import consulo.ide.webService.WebServiceApi;
import consulo.ide.webService.WebServicesConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 11-Feb-17
 */
@Singleton
public class ExternalStorageStreamProvider extends StreamProvider {
  private final IApplicationStore myStore;
  private final WebServicesConfiguration myWebServicesConfiguration;

  private final ExternalStorage myStorage = new ExternalStorage();

  @Inject
  public ExternalStorageStreamProvider(IApplicationStore store, WebServicesConfiguration webServicesConfiguration) {
    myStore = store;
    myWebServicesConfiguration = webServicesConfiguration;
  }

  @Override
  public boolean isEnabled() {
    return !StringUtil.isEmpty(myWebServicesConfiguration.getOAuthKey(WebServiceApi.SYNCHRONIZE_API));
  }

  @Nonnull
  @Override
  public Collection<String> listSubFiles(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    return myStorage.listSubFiles(fileSpec, roamingType);
  }

  @Override
  public void saveContent(@Nonnull String fileSpec, @Nonnull byte[] content, int size, @Nonnull RoamingType roamingType) throws IOException {
    myStorage.saveContent(fileSpec, roamingType, content, size);
  }

  @Nullable
  @Override
  public InputStream loadContent(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) throws IOException {
    return myStorage.loadContent(fileSpec, roamingType, myStore.getStateStorageManager());
  }

  @Override
  public void delete(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    myStorage.delete(fileSpec, roamingType);
  }
}
