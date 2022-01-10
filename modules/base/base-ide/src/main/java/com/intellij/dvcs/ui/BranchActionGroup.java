/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.dvcs.ui;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.project.DumbAware;
import consulo.ui.image.Image;
import consulo.ui.image.ImageState;
import icons.DvcsImplIcons;

import javax.annotation.Nonnull;

public abstract class BranchActionGroup extends ActionGroup implements DumbAware {

  private boolean myIsFavorite;

  private ImageState<Boolean> myIconState = new ImageState<>(Boolean.FALSE);

  public BranchActionGroup() {
    super("", true);
    getTemplatePresentation().setDisabledMnemonic(true);
    setIcons(DvcsImplIcons.Favorite, Image.empty(Image.DEFAULT_ICON_SIZE), DvcsImplIcons.Favorite, DvcsImplIcons.NotFavoriteOnHover);
  }

  protected void setIcons(@Nonnull Image favorite, @Nonnull Image notFavorite, @Nonnull Image favoriteOnHover, @Nonnull Image notFavoriteOnHover) {
    getTemplatePresentation().setIcon(Image.stated(myIconState, it -> it ? favorite : notFavorite));
    getTemplatePresentation().setSelectedIcon(Image.stated(myIconState, it -> it ? favoriteOnHover : notFavoriteOnHover));

    updateIcons();
  }

  private void updateIcons() {
    myIconState.setState(myIsFavorite);
  }

  public boolean isFavorite() {
    return myIsFavorite;
  }

  public void setFavorite(boolean favorite) {
    myIsFavorite = favorite;
    updateIcons();
  }

  public void toggle() {
    setFavorite(!myIsFavorite);
  }

  public boolean hasIncomingCommits() {
    return false;
  }

  public boolean hasOutgoingCommits() {
    return false;
  }
}
