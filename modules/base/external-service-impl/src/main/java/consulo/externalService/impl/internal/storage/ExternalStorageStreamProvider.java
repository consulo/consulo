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
package consulo.externalService.impl.internal.storage;

import consulo.component.persist.RoamingType;
import consulo.component.store.internal.StreamProvider;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 11-Feb-17
 */
public class ExternalStorageStreamProvider extends StreamProvider {
  private final ExternalStorage myStorage;
  private final ExternalServiceConfiguration myExternalServiceConfiguration;

  public ExternalStorageStreamProvider(ExternalStorage externalStorage, ExternalServiceConfiguration externalServiceConfiguration) {
    myExternalServiceConfiguration = externalServiceConfiguration;
    myStorage = externalStorage;
  }

  @Override
  public boolean isEnabled() {
    if(!myStorage.isInitialized()) {
      return false;
    }

    return myExternalServiceConfiguration.getState(ExternalService.STORAGE) == ThreeState.YES;
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
    return myStorage.loadContent(fileSpec, roamingType);
  }

  @Override
  public void delete(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    myStorage.delete(fileSpec, roamingType);
  }
}
