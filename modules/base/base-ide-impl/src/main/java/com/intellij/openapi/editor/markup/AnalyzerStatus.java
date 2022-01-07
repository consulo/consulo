/*
 * Copyright 2013-2020 consulo.io
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
package com.intellij.openapi.editor.markup;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.ui.GridBag;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Container containing all necessary information for rendering TrafficLightRenderer.
 * Instance is created each time <code>ErrorStripeRenderer.getStatus</code> is called.
 * <p>
 * from kotlin
 */
public class AnalyzerStatus {
  public static final NotNullLazyValue<AnalyzerStatus> DEFAULT = NotNullLazyValue.createValue(() -> new AnalyzerStatus(consulo.ui.image.Image.empty(0), null, null, () -> new UIController() {
    @Override
    public boolean enableToolbar() {
      return false;
    }

    @Nonnull
    @Override
    public List<AnAction> getActions() {
      return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<InspectionsLevel> getAvailableLevels() {
      return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<LanguageHighlightLevel> getHighlightLevels() {
      return Collections.emptyList();
    }

    @Override
    public void setHighLightLevel(@Nonnull LanguageHighlightLevel newLevels) {

    }

    @Override
    public void fillHectorPanels(@Nonnull Container container, @Nonnull GridBag bag) {

    }

    @Override
    public boolean canClosePopup() {
      return false;
    }

    @Override
    public void onClosePopup() {

    }

    @Override
    public void openProblemsView() {

    }
  }));

  public static boolean equals(@Nullable AnalyzerStatus a, @Nullable AnalyzerStatus b) {
    if (a == null && b == null) {
      return true;
    }

    if (a == null || b == null) {
      return false;
    }

    return Objects.equals(a.getIcon(), b.getIcon()) &&
           Objects.equals(a.expandedStatus, b.expandedStatus) &&
           Objects.equals(a.getTitle(), b.getTitle()) &&
           Objects.equals(a.getDetails(), b.getDetails()) &&
           a.isShowNavigation() == b.isShowNavigation() &&
           Objects.equals(a.passes, b.passes);
  }

  private final consulo.ui.image.Image myIcon;
  private final String myTitle;
  private final String myDetails;

  private List<StatusItem> expandedStatus = Collections.emptyList();
  private List<PassWrapper> passes = Collections.emptyList();

  private boolean textStatus;

  private AnalyzingType analyzingType = AnalyzingType.COMPLETE;

  private boolean myShowNavigation;

  private NotNullLazyValue<UIController> myControllerValue;

  public AnalyzerStatus(consulo.ui.image.Image icon, String title, String details, Supplier<UIController> controllerCreator) {
    myIcon = icon;
    myTitle = title;
    myDetails = details;

    myControllerValue = NotNullLazyValue.createValue(controllerCreator);
  }

  @Nonnull
  public UIController getController() {
    return myControllerValue.getValue();
  }

  public List<StatusItem> getExpandedStatus() {
    return expandedStatus;
  }

  public List<PassWrapper> getPasses() {
    return passes;
  }

  public boolean isTextStatus() {
    return textStatus;
  }

  public AnalyzingType getAnalyzingType() {
    return analyzingType;
  }

  public boolean isShowNavigation() {
    return myShowNavigation;
  }

  public AnalyzerStatus withNavigation() {
    myShowNavigation = true;
    return this;
  }

  public AnalyzerStatus withExpandedStatus(List<StatusItem> status) {
    expandedStatus = status;
    return this;
  }

  public AnalyzerStatus withPasses(List<PassWrapper> passes) {
    this.passes = passes;
    return this;
  }

  public AnalyzerStatus withAnalyzingType(AnalyzingType type) {
    analyzingType = type;
    return this;
  }

  public AnalyzerStatus withTextStatus(String status) {
    expandedStatus = Collections.singletonList(new StatusItem(status));
    textStatus = true;
    return this;
  }

  public Image getIcon() {
    return myIcon;
  }

  public String getTitle() {
    return myTitle;
  }

  public String getDetails() {
    return myDetails;
  }
}
