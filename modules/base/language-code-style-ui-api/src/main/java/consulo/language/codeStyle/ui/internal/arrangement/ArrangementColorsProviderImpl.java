/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;
import consulo.language.codeStyle.arrangement.std.ArrangementColorsAware;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import org.jspecify.annotations.Nullable;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Zhdanov
 * @since 10/24/12 4:25 PM
 */
public class ArrangementColorsProviderImpl implements ArrangementColorsProvider {

  
  private final Map<ArrangementSettingsToken, TextAttributes> myNormalAttributesCache = new HashMap<>();
  
  private final Map<ArrangementSettingsToken, TextAttributes> mySelectedAttributesCache = new HashMap<>();

  
  private final TextAttributes myDefaultNormalAttributes = new TextAttributes();
  
  private final TextAttributes myDefaultSelectedAttributes = new TextAttributes();
  
  private final Color myDefaultNormalBorderColor;
  
  private final Color myDefaultSelectedBorderColor;

  private final @Nullable ArrangementColorsAware myColorsAware;

  private @Nullable Color myCachedNormalBorderColor;
  private @Nullable Color myCachedSelectedBorderColor;

  public ArrangementColorsProviderImpl(@Nullable ArrangementColorsAware colorsAware) {
    myColorsAware = colorsAware;

    // Default settings.
    myDefaultNormalAttributes.setForegroundColor(TargetAWT.from(UIUtil.getTreeTextForeground()));
    myDefaultNormalAttributes.setBackgroundColor(TargetAWT.from(UIUtil.getPanelBackground()));
    myDefaultSelectedAttributes.setForegroundColor(TargetAWT.from(UIUtil.getTreeSelectionForeground()));
    myDefaultSelectedAttributes.setBackgroundColor(TargetAWT.from(UIUtil.getTreeSelectionBackground()));
    myDefaultNormalBorderColor = UIUtil.getBorderColor();
    Color selectionBorderColor = UIUtil.getTreeSelectionBorderColor();
    if (selectionBorderColor == null) {
      selectionBorderColor = JBColor.black;
    }
    myDefaultSelectedBorderColor = selectionBorderColor;
  }

  
  @Override
  public Color getBorderColor(boolean selected) {
    Color cached;
    if (selected) {
      cached = myCachedSelectedBorderColor;
    }
    else {
      cached = myCachedNormalBorderColor;
    }
    if (cached != null) {
      return cached;
    }

    Color result = null;
    if (myColorsAware != null) {
      result = myColorsAware.getBorderColor(EditorColorsManager.getInstance().getGlobalScheme(), selected);
    }
    if (result == null) {
      result = selected ? myDefaultSelectedBorderColor : myDefaultNormalBorderColor;
    }
    if (selected) {
      myCachedSelectedBorderColor = result;
    }
    else {
      myCachedNormalBorderColor = result;
    }
    return result;
  }

  
  @Override
  public TextAttributes getTextAttributes(ArrangementSettingsToken token, boolean selected) {
    TextAttributes cached;
    if (selected) {
      cached = mySelectedAttributesCache.get(token);
    }
    else {
      cached = myNormalAttributesCache.get(token);
    }
    if (cached != null) {
      return cached;
    }

    TextAttributes result = null;
    if (myColorsAware != null) {
      result = myColorsAware.getTextAttributes(EditorColorsManager.getInstance().getGlobalScheme(), token, selected);
    }
    if (result == null) {
      result = selected ? myDefaultSelectedAttributes : myDefaultNormalAttributes;
    }
    if (selected) {
      mySelectedAttributesCache.put(token, result);
    }
    else {
      myNormalAttributesCache.put(token, result);
    }

    return result;
  }

  /**
   * Asks the implementation to ensure that it uses the most up-to-date colors.
   * <p/>
   * I.e. this method is assumed to be called when color settings has been changed and gives a chance to reflect the changes
   * accordingly.
   */
  public void refresh() {
    if (myColorsAware != null) {
      myNormalAttributesCache.clear();
      mySelectedAttributesCache.clear();
    }
  }
}
