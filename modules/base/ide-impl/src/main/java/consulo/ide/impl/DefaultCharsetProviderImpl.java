/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.process.DefaultCharsetProvider;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;
import java.nio.charset.Charset;

/**
 * @author VISTALL
 * @since 05-Feb-22
 */
@Singleton
@ServiceImpl
public class DefaultCharsetProviderImpl implements DefaultCharsetProvider {
  private ApplicationEncodingManager myEncodingManager;

  @Inject
  public DefaultCharsetProviderImpl(ApplicationEncodingManager encodingManager) {
    myEncodingManager = encodingManager;
  }

  @Nullable
  @Override
  public Charset getDefaultCharset() {
    return myEncodingManager.getDefaultCharset();
  }
}
