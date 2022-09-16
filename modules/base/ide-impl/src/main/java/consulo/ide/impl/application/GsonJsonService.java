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
package consulo.ide.impl.application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import consulo.annotation.component.ServiceImpl;
import consulo.application.json.JsonService;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14-Sep-22
 */
@Singleton
@ServiceImpl
public class GsonJsonService implements JsonService {
  private final Gson myGson = new GsonBuilder().setPrettyPrinting().create();

  @Nonnull
  @Override
  public String toJson(@Nonnull Object value) {
    return myGson.toJson(value);
  }

  @Nonnull
  @Override
  public <T> T fromJson(@Nonnull String json, @Nonnull Class<T> clazz) {
    return myGson.fromJson(json, clazz);
  }
}
