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

package consulo.ui.ex;

import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;

public class SimpleColoredText implements ColoredTextContainer {
  private final ArrayList<String> myTexts;
  private final ArrayList<SimpleTextAttributes> myAttributes;
  private String myCachedToString = null;

  public SimpleColoredText() {
    myTexts = new ArrayList<String>(3);
    myAttributes = new ArrayList<SimpleTextAttributes>(3);
  }

  public SimpleColoredText(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes) {
    this();
    append(fragment, attributes);
  }

  @Override
  public void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes){
    myTexts.add(fragment);
    myCachedToString = null;
    myAttributes.add(attributes);
  }

  public void insert(int index, @Nonnull String fragment, @Nonnull SimpleTextAttributes attributes) {
    myTexts.add(index, fragment);
    myCachedToString = null;
    myAttributes.add(index, attributes);
  }

  @Override
  public void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes, Object tag) {
    append(fragment, attributes);
  }

  @Override
  public void setIcon(@Nullable Image icon) {
  }

  @Override
  public void setToolTipText(@Nullable String text) {
  }

  @Nonnull
  @Override
  public CharSequence getCharSequence(boolean mainOnly) {
    return toString();
  }

  @Nonnull
  @Override
  public ColoredIterator iterator() {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    myTexts.clear();
    myCachedToString = null;
    myAttributes.clear();
  }

  public void appendToComponent(@Nonnull ColoredTextContainer component) {
    int size = myTexts.size();
    for (int i = 0; i < size; i++) {
      String text = myTexts.get(i);
      SimpleTextAttributes attribute = myAttributes.get(i);
      component.append(text, attribute);
    }
  }

  @Override
  public String toString() {
    if (myCachedToString == null) {
      myCachedToString = StringUtil.join(myTexts, "");
    }
    return myCachedToString;
  }

  public ArrayList<String> getTexts() {
    return myTexts;
  }

  public ArrayList<SimpleTextAttributes> getAttributes() {
    return myAttributes;
  }

  public SimpleColoredText derive(SimpleTextAttributes attributes, boolean override) {
    SimpleColoredText result = new SimpleColoredText();
    for (int i = 0; i < myTexts.size(); i++) {
      SimpleTextAttributes overridden = override
                                        ? SimpleTextAttributes.merge(myAttributes.get(i), attributes)
                                        : SimpleTextAttributes.merge(attributes, myAttributes.get(i));
      result.append(myTexts.get(i), overridden);
    }
    return result;
  }
}
