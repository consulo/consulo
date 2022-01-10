/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.codeInsight.hints.settings;

import com.intellij.lang.Language;
import com.intellij.openapi.components.*;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.containers.ContainerUtil;
import java.util.HashMap;
import jakarta.inject.Singleton;
import org.jdom.Attribute;
import org.jdom.Element;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;

/**
 * from kotlin
 */
@Singleton
@State(name = "ParameterNameHintsSettings", storages = @Storage("parameter.hints.xml"))
public class ParameterNameHintsSettings implements PersistentStateComponent<Element> {
  private static final String BLACKLISTS = "blacklists";
  private static final String LANGUAGE_LIST = "blacklist";
  private static final String LANGUAGE = "language";
  private static final String ADDED = "added";
  private static final String REMOVED = "removed";
  private static final String PATTERN = "pattern";
  private static final String DO_NOT_SHOW_IF_PARAM_NAME_CONTAINED_IN_METHOD_NAME = "showIfParamNameContained";
  private static final String SHOW_WHEN_MULTIPLE_PARAMS_WITH_SAME_TYPE = "showWhenMultipleParamsWithSameType";

  @Nonnull
  public static ParameterNameHintsSettings getInstance() {
    return ServiceManager.getService(ParameterNameHintsSettings.class);
  }

  private final Map<String, Set<String>> myRemovedPatterns = new HashMap<>();
  private final Map<String, Set<String>> myAddedPatterns = new HashMap<>();

  private boolean myIsDoNotShowIfMethodNameContainsParameterName = true;
  private boolean myIsShowForParamsWithSameType = false;

  public void addIgnorePattern(Language language, String pattern) {
    Set<String> patternsBefore = getAddedPatterns(language);
    setAddedPatterns(language, ContainerUtil.<String>newLinkedHashSet(patternsBefore, pattern));
  }

  public Diff getBlackListDiff(Language language) {
    Set<String> added = getAddedPatterns(language);
    Set<String> removed = getRemovedPatterns(language);

    return new Diff(added, removed);
  }

  public void setBlackListDiff(Language language, Diff diff) {
    setAddedPatterns(language, diff.getAdded());
    setRemovedPatterns(language, diff.getRemoved());
  }

  public boolean isDoNotShowIfMethodNameContainsParameterName() {
    return myIsDoNotShowIfMethodNameContainsParameterName;
  }

  public boolean isShowForParamsWithSameType() {
    return myIsShowForParamsWithSameType;
  }

  public void setDoNotShowIfMethodNameContainsParameterName(boolean doNotShowIfMethodNameContainsParameterName) {
    myIsDoNotShowIfMethodNameContainsParameterName = doNotShowIfMethodNameContainsParameterName;
  }

  public void setShowForParamsWithSameType(boolean showForParamsWithSameType) {
    myIsShowForParamsWithSameType = showForParamsWithSameType;
  }

  @Nullable
  @Override
  public Element getState() {
    Element root = new Element("settings");

    if (!myRemovedPatterns.isEmpty() || !myAddedPatterns.isEmpty()) {
      Element blacklists = getOrCreateChild(root, BLACKLISTS);

      myRemovedPatterns.forEach((language, patterns) -> addLanguagePatternElements(blacklists, language, patterns, REMOVED));

      myAddedPatterns.forEach((language, patterns) -> addLanguagePatternElements(blacklists, language, patterns, ADDED));
    }

    if (!myIsDoNotShowIfMethodNameContainsParameterName) {
      getOrCreateChild(root, DO_NOT_SHOW_IF_PARAM_NAME_CONTAINED_IN_METHOD_NAME)
              .setAttribute("value", String.valueOf(myIsDoNotShowIfMethodNameContainsParameterName));
    }

    if (myIsShowForParamsWithSameType) {
      getOrCreateChild(root, SHOW_WHEN_MULTIPLE_PARAMS_WITH_SAME_TYPE).setAttribute("value", String.valueOf(myIsShowForParamsWithSameType));
    }

    return root;
  }

  @Override
  public void loadState(Element state) {
    myAddedPatterns.clear();
    myRemovedPatterns.clear();

    myIsDoNotShowIfMethodNameContainsParameterName = true;
    myIsShowForParamsWithSameType = false;

    List<Element> allBlackLists = JDOMUtil.getChildren(state.getChild(BLACKLISTS), LANGUAGE_LIST);

    for (Element blacklist : allBlackLists) {
      String language = attributeValue(blacklist, LANGUAGE);
      if (language == null) {
        continue;
      }
      myAddedPatterns.put(language, extractPatterns(blacklist, ADDED));
      myRemovedPatterns.put(language, extractPatterns(blacklist, REMOVED));
    }

    myIsDoNotShowIfMethodNameContainsParameterName = getBooleanValue(state, DO_NOT_SHOW_IF_PARAM_NAME_CONTAINED_IN_METHOD_NAME, true);

    myIsShowForParamsWithSameType = getBooleanValue(state, SHOW_WHEN_MULTIPLE_PARAMS_WITH_SAME_TYPE, false);
  }

  @Nonnull
  private Set<String> extractPatterns(Element element, String tag) {
    List<Element> children = element.getChildren(tag);
    return new LinkedHashSet<>(ContainerUtil.mapNotNull(children, it -> attributeValue(it, PATTERN)));
  }

  @Nullable
  private String attributeValue(Element element, String attr) {
    Attribute attribute = element.getAttribute(attr);
    if (attribute == null) {
      return null;
    }
    return attribute.getValue();
  }

  private static void addLanguagePatternElements(Element element, String language, Set<String> patterns, String tag) {
    Element list = getOrCreateChild(element, LANGUAGE_LIST);
    list.setAttribute(LANGUAGE, language);
    List<Element> elements = ContainerUtil.map(patterns, it -> toPatternElement(it, tag));
    list.addContent(elements);
  }

  private static Element toPatternElement(String pattern, String status) {
    Element element = new Element(status);
    element.setAttribute(PATTERN, pattern);
    return element;
  }

  private static Element getOrCreateChild(Element element, String name) {
    Element child = element.getChild(name);
    if (child == null) {
      child = new Element(name);
      element.addContent(child);
    }
    return child;
  }

  private static boolean getBooleanValue(Element element, String childName, boolean defaultValue) {
    Element child = element.getChild(childName);
    if (child == null) {
      return defaultValue;
    }
    String value = child.getAttributeValue("value");
    if (value == null) {
      return defaultValue;
    }
    return Boolean.parseBoolean(value);
  }

  @Nonnull
  private Set<String> getAddedPatterns(Language language) {
    String key = language.getID();
    Set<String> set = myAddedPatterns.get(key);
    return set == null ? Collections.<String>emptySet() : set;
  }

  @Nonnull
  private Set<String> getRemovedPatterns(Language language) {
    String key = language.getID();
    Set<String> set = myRemovedPatterns.get(key);
    return set == null ? Collections.<String>emptySet() : set;
  }

  private void setRemovedPatterns(Language language, Set<String> removed) {
    String key = language.getID();
    myRemovedPatterns.put(key, removed);
  }

  private void setAddedPatterns(Language language, Set<String> added) {
    String key = language.getID();
    myAddedPatterns.put(key, added);
  }
}
