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
package consulo.process.impl;

import consulo.language.file.EncodingManager;
import consulo.process.DefaultCharsetProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;
import java.nio.charset.Charset;

/**
 * @author VISTALL
 * @since 05-Feb-22
 */
@Singleton
public class DefaultCharsetProviderImpl implements DefaultCharsetProvider {
  private EncodingManager myEncodingManager;

  @Inject
  public DefaultCharsetProviderImpl(EncodingManager encodingManager) {
    myEncodingManager = encodingManager;
  }

  @Nullable
  @Override
  public Charset getDefaultCharset() {
    return myEncodingManager.getDefaultCharset();
  }
}
