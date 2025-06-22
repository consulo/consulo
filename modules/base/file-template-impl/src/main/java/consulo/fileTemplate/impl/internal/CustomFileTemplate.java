/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 * @since 2011-04-06
 */
public final class CustomFileTemplate extends FileTemplateBase {
  private String myName;
  private String myExtension;

  public CustomFileTemplate(@Nonnull String name, @Nonnull String extension) {
    myName = name;
    myExtension = extension;
  }

  @Override
  @Nonnull
  public String getName() {
    return myName;
  }

  @Override
  public void setName(@Nonnull String name) {
    myName = name;
  }

  @Override
  @Nonnull
  public String getExtension() {
    return myExtension;
  }

  @Override
  public void setExtension(@Nonnull String extension) {
    myExtension = extension;
  }

  @Override
  @Nonnull
  public String getDescription() {
    return "";  // todo: some default description?
  }

  @Override
  public CustomFileTemplate clone() {
    return (CustomFileTemplate)super.clone();
  }

  @Override
  public boolean isDefault() {
    return false;
  }
}
