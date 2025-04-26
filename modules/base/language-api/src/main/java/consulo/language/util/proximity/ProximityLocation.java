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
package consulo.language.util.proximity;

import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class ProximityLocation implements UserDataHolder {
  private final PsiElement myPosition;
  private final Module myPositionModule;
  private final ProcessingContext myContext;

  public ProximityLocation(@Nullable final PsiElement position, final Module positionModule) {
    this(position, positionModule, new ProcessingContext());
  }

  public ProximityLocation(PsiElement position, Module positionModule, ProcessingContext context) {
    myPosition = position;
    myPositionModule = positionModule;
    myContext = context;
  }

  @Nullable
  public Module getPositionModule() {
    return myPositionModule;
  }

  @Nullable
  public PsiElement getPosition() {
    return myPosition;
  }

  @Nullable
  public Project getProject() {
    return myPosition != null ? myPosition.getProject() : null;
  }

  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return myContext.get(key);
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    myContext.put(key, value);
  }
}
