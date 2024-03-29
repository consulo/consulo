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
package consulo.language.psi.search;

import consulo.logging.Logger;
import consulo.util.lang.Comparing;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import java.util.regex.Pattern;

/**
 * @author Vladimir Kondratyev
 */
public class TodoPattern implements Cloneable {
  private static final Logger LOG = Logger.getInstance(TodoPattern.class);

  private IndexPattern myIndexPattern;

  /**
   * Specify Icon and text attributes.
   */
  private TodoAttributes myAttributes;

  private static final String CASE_SENS_ATT = "case-sensitive";
  private static final String PATTERN_ATT = "pattern";

  public TodoPattern(@Nonnull TodoAttributes attributes) {
    this("", attributes, false);
  }

  public TodoPattern(@Nonnull String patternString, @Nonnull TodoAttributes attributes, boolean caseSensitive) {
    myIndexPattern = new IndexPattern(patternString, caseSensitive);
    myAttributes = attributes;
  }

  @Nonnull
  public String getPatternString() {
    return myIndexPattern.getPatternString();
  }

  public void setPatternString(@Nonnull String patternString) {
    myIndexPattern.setPatternString(patternString);
  }

  @Nonnull
  public TodoAttributes getAttributes() {
    return myAttributes;
  }

  public void setAttributes(@Nonnull TodoAttributes attributes) {
    myAttributes = attributes;
  }

  public boolean isCaseSensitive() {
    return myIndexPattern.isCaseSensitive();
  }

  public void setCaseSensitive(boolean caseSensitive) {
    myIndexPattern.setCaseSensitive(caseSensitive);
  }

  public Pattern getPattern() {
    return myIndexPattern.getPattern();
  }

  public void readExternal(Element element) throws InvalidDataException {
    myAttributes = new TodoAttributes(element);
    myIndexPattern.setCaseSensitive(Boolean.valueOf(element.getAttributeValue(CASE_SENS_ATT)));
    String attributeValue = element.getAttributeValue(PATTERN_ATT);
    if (attributeValue != null) {
      myIndexPattern.setPatternString(attributeValue.trim());
    }
  }

  public void writeExternal(Element element) throws WriteExternalException {
    myAttributes.writeExternal(element);
    element.setAttribute(CASE_SENS_ATT, Boolean.toString(myIndexPattern.isCaseSensitive()));
    element.setAttribute(PATTERN_ATT, myIndexPattern.getPatternString());
  }

  @Override
  public TodoPattern clone() {
    try {
      TodoAttributes attributes = myAttributes.clone();
      TodoPattern pattern = (TodoPattern)super.clone();
      pattern.myIndexPattern = new IndexPattern(myIndexPattern.getPatternString(), myIndexPattern.isCaseSensitive());
      pattern.myAttributes = attributes;

      return pattern;
    }
    catch (CloneNotSupportedException e) {
      LOG.error(e);
      return null;
    }
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof TodoPattern)) {
      return false;
    }
    TodoPattern pattern = (TodoPattern)obj;
    if (!myIndexPattern.equals(pattern.myIndexPattern)) {
      return false;
    }
    if (!Comparing.equal(myAttributes, pattern.myAttributes)) {
      return false;
    }
    return true;
  }

  public int hashCode() {
    return myIndexPattern.hashCode();
  }

  public IndexPattern getIndexPattern() {
    return myIndexPattern;
  }
}
