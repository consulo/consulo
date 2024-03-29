/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.application.ApplicationPropertiesComponent;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Major difference between the parent class and <code>JBSplitter</code> is an ability to save proportion
 *
 * @author Konstantin Bulenkov
 * @see Splitter
 */
public class JBSplitter extends Splitter {
  /**
   * Used as a key to save and load proportion
   */
  @Nullable
  private String mySplitterProportionKey = null;
  private final float myDefaultProportion;

  public JBSplitter() {
    super();

    myDefaultProportion = 0.5f;
  }

  public JBSplitter(@Nonnull String proportionKey, float defaultProportion) {
    this(false, proportionKey, defaultProportion);
  }

  public JBSplitter(boolean vertical, @Nonnull String proportionKey, float defaultProportion) {
    super(vertical, defaultProportion);

    mySplitterProportionKey = proportionKey;
    myDefaultProportion = defaultProportion;
  }

  public JBSplitter(boolean vertical) {
    super(vertical);

    myDefaultProportion = 0.5f;
  }

  public JBSplitter(boolean vertical, float proportion) {
    super(vertical, proportion);

    myDefaultProportion = proportion;
  }

  public JBSplitter(float proportion) {
    super(false, proportion);

    myDefaultProportion = proportion;
  }

  public JBSplitter(boolean vertical, float proportion, float minProp, float maxProp) {
    super(vertical, proportion, minProp, maxProp);

    myDefaultProportion = proportion;
  }

  /**
   * Splitter proportion unique key
   *
   * @return non empty unique String or <code>null</code> if splitter does not require proportion saving
   */
  @Nullable
  public final String getSplitterProportionKey() {
    return mySplitterProportionKey;
  }

  /**
   * Sets proportion key
   *
   * @param key non empty unique String or <code>null</code> if splitter does not require proportion saving
   */
  public final void setSplitterProportionKey(@Nullable String key) {
    mySplitterProportionKey = key;
  }

  /**
   * Sets proportion key and load from settings.
   * @param key
   */
  public final void setAndLoadSplitterProportionKey(@Nonnull String key) {
    setSplitterProportionKey(key);
    loadProportion();
  }

  @Override
  public void addNotify() {
    super.addNotify();
    loadProportion();
  }

  @Override
  public void setProportion(float proportion) {
    super.setProportion(proportion);
    saveProportion();
  }

  protected void loadProportion() {
    if (!StringUtil.isEmpty(mySplitterProportionKey)) {
      setProportion(ApplicationPropertiesComponent.getInstance().getFloat(mySplitterProportionKey, myProportion));
    }
  }

  protected void saveProportion() {
    if (!StringUtil.isEmpty(mySplitterProportionKey)) {
      ApplicationPropertiesComponent.getInstance().setValue(mySplitterProportionKey, myProportion, myDefaultProportion);
    }
  }
}
