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
package consulo.fileTemplate.impl.internal;

import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.fileTemplate.FileTemplateContributor;
import consulo.fileTemplate.FileTemplateRegistrator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
public class FileTemplateRegistratorImpl implements FileTemplateRegistrator {
  private static final ExtensionPointCacheKey<FileTemplateContributor, FileTemplateRegistratorImpl> REG_KEY = ExtensionPointCacheKey.create("FileTemplateRegistratorImpl", walker -> {
    FileTemplateRegistratorImpl impl = new FileTemplateRegistratorImpl();
    walker.walk(contributor -> contributor.register(impl));
    return impl;
  });

  @Nonnull
  public static FileTemplateRegistratorImpl last() {
    return Application.get().getExtensionPoint(FileTemplateContributor.class).getOrBuildCache(REG_KEY);
  }

  private final Map<String, String> myInternalTemplates = new LinkedHashMap<>();

  @Override
  public void registerInternalTemplate(@Nonnull String name, @Nullable String subject) {
    myInternalTemplates.put(name, subject);
  }

  @Nonnull
  public Map<String, String> getInternalTemplates() {
    return myInternalTemplates;
  }
}
