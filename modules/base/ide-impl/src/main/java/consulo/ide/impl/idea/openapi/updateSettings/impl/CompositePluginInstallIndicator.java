/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.openapi.updateSettings.impl;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 30-Jun-24
 */
public class CompositePluginInstallIndicator extends EmptyProgressIndicator {
  @Nonnull
  private final ProgressIndicator myProgressIndicator;
  private final int myCurrentIndex;
  private float myProgressModifier;

  public CompositePluginInstallIndicator(ProgressIndicator progressIndicator, int currentIndex, int pluginsCount) {
    super(progressIndicator.getModalityState());
    myProgressIndicator = progressIndicator;
    myCurrentIndex = currentIndex;
    myProgressModifier = 1f / pluginsCount;
  }

  @Override
  public void setFraction(double fraction) {
    double f = myProgressModifier * myCurrentIndex + myProgressModifier * fraction;

    myProgressIndicator.setFraction(f);
  }

  @Override
  public void setText2Value(LocalizeValue text) {
    myProgressIndicator.setTextValue(text);
  }

  @Override
  public void setTextValue(LocalizeValue text) {
    myProgressIndicator.setTextValue(text);
  }

  @Override
  public void setIndeterminate(boolean indeterminate) {
    myProgressIndicator.setIndeterminate(indeterminate);
  }
}
