/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ide.fileTemplates;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import consulo.container.boot.ContainerPathManager;
import consulo.util.pointers.Named;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * @author Dmitry Avdeev
 */
public abstract class FileTemplatesScheme implements Named {

  public final static FileTemplatesScheme DEFAULT = new FileTemplatesScheme("Default") {
    @Nonnull
    @Override
    public String getTemplatesDir() {
      return new File(ContainerPathManager.get().getConfigPath(), TEMPLATES_DIR).getPath();
    }

    @Nonnull
    @Override
    public Project getProject() {
      return ProjectManager.getInstance().getDefaultProject();
    }
  };

  public static final String TEMPLATES_DIR = "fileTemplates";

  private final String myName;

  public FileTemplatesScheme(@Nonnull String name) {
    myName = name;
  }

  @Override
  @Nonnull
  public String getName() {
    return myName;
  }

  @Nonnull
  public abstract String getTemplatesDir();

  @Nonnull
  public abstract Project getProject();
}
