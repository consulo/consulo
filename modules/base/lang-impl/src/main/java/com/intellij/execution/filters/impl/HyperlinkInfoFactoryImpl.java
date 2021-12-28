/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.execution.filters.impl;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.HyperlinkInfoFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import jakarta.inject.Singleton;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
@Singleton
public class HyperlinkInfoFactoryImpl extends HyperlinkInfoFactory {
  @Nonnull
  @Override
  public HyperlinkInfo createMultipleFilesHyperlinkInfo(@Nonnull List<? extends VirtualFile> files, int line, @Nonnull Project project) {
    return new MultipleFilesHyperlinkInfo(files, line, project);
  }

  @Nonnull
  @Override
  public HyperlinkInfo createMultipleFilesHyperlinkInfo(@Nonnull List<? extends VirtualFile> files, int line, @Nonnull Project project, HyperlinkInfoFactory.HyperlinkHandler action) {
    return new MultipleFilesHyperlinkInfo(files, line, project, action);
  }

  @Override
  public
  @Nonnull
  HyperlinkInfo createMultiplePsiElementHyperlinkInfo(@Nonnull Collection<? extends PsiElement> elements) {
    return new MultiPsiElementHyperlinkInfo(elements);
  }
}
