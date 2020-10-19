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
package com.intellij.conversion;

import com.intellij.openapi.project.Project;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

import java.io.File;

/**
 * @author Dmitry Avdeev
 *         Date: 9/22/11
 */
@Singleton
public class DummyConversionService extends ConversionService {
  @Nonnull
  @Override
  public ConversionResult convertSilently(@Nonnull String projectPath) {
    return ConversionResult.DUMMY;
  }

  @Nonnull
  @Override
  public ConversionResult convertSilently(@Nonnull String projectPath, @Nonnull ConversionListener conversionListener) {
    return ConversionResult.DUMMY;
  }

  @Nonnull
  @Override
  public ConversionResult convert(@Nonnull String projectPath) {
    return ConversionResult.DUMMY;
  }

  @Nonnull
  @Override
  public ConversionResult convertModule(@Nonnull Project project, @Nonnull File moduleFile) {
    return ConversionResult.DUMMY;
  }
}
