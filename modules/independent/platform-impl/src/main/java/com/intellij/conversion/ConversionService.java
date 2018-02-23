/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

import java.io.File;

/**
 * @author nik
 */
public abstract class ConversionService {

  @Nonnull
  public static ConversionService getInstance() {
    ConversionService service = ServiceManager.getService(ConversionService.class);
    return service == null ? new DummyConversionService() : service;
  }

  @Nonnull
  public abstract ConversionResult convertSilently(@Nonnull String projectPath);

  @Nonnull
  public abstract ConversionResult convertSilently(@Nonnull String projectPath, @Nonnull ConversionListener conversionListener);

  @Nonnull
  public abstract ConversionResult convert(@Nonnull String projectPath);

  @Nonnull
  public abstract ConversionResult convertModule(@Nonnull Project project, @Nonnull File moduleFile);
}
