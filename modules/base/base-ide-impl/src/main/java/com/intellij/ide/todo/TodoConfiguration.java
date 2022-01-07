/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.ide.todo;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.psi.search.*;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vladimir Kondratyev
 */
@Singleton
@State(name = "TodoConfiguration", storages = @Storage("editor.xml"))
public class TodoConfiguration implements PersistentStateComponent<Element> {
  public static final Topic<PropertyChangeListener> PROPERTY_CHANGE = new Topic<>("TodoConfiguration changes", PropertyChangeListener.class);

  private TodoPattern[] myTodoPatterns;
  private TodoFilter[] myTodoFilters;
  private IndexPattern[] myIndexPatterns;

  @NonNls
  public static final String PROP_MULTILINE = "multiLine";
  @NonNls public static final String PROP_TODO_PATTERNS = "todoPatterns";
  @NonNls public static final String PROP_TODO_FILTERS = "todoFilters";
  @NonNls private static final String ELEMENT_PATTERN = "pattern";
  @NonNls private static final String ELEMENT_FILTER = "filter";
  private final MessageBus myMessageBus;

  private final PropertyChangeListener myTopic;

  @Inject
  public TodoConfiguration(@Nonnull Application application) {
    myMessageBus = application.getMessageBus();
    resetToDefaultTodoPatterns();
    myTopic = myMessageBus.syncPublisher(PROPERTY_CHANGE);
  }

  public void resetToDefaultTodoPatterns() {
    myTodoPatterns = new TodoPattern[]{
      new TodoPattern("\\btodo\\b.*", TodoAttributesUtil.createDefault(), false),
      new TodoPattern("\\bfixme\\b.*", TodoAttributesUtil.createDefault(), false),
    };
    myTodoFilters = new TodoFilter[]{};
    buildIndexPatterns();
  }

  private void buildIndexPatterns() {
    myIndexPatterns = new IndexPattern[myTodoPatterns.length];
    for(int i=0; i<myTodoPatterns.length; i++) {
      myIndexPatterns [i] = myTodoPatterns [i].getIndexPattern();
    }
  }

  @Deprecated
  public static TodoConfiguration getInstance() {
    return ServiceManager.getService(TodoConfiguration.class);
  }

  @Nonnull
  public TodoPattern[] getTodoPatterns() {
    return myTodoPatterns;
  }

  @Nonnull
  public IndexPattern[] getIndexPatterns() {
    return myIndexPatterns;
  }

  public void setTodoPatterns(@Nonnull TodoPattern[] todoPatterns) {
    doSetTodoPatterns(todoPatterns, true);
  }

  private void doSetTodoPatterns(@Nonnull TodoPattern[] todoPatterns, final boolean shouldNotifyIndices) {
    TodoPattern[] oldTodoPatterns = myTodoPatterns;
    IndexPattern[] oldIndexPatterns = myIndexPatterns;

    myTodoPatterns = todoPatterns;
    buildIndexPatterns();

    // only trigger index refresh actual index patterns have changed
    if (shouldNotifyIndices && !Arrays.deepEquals(myIndexPatterns, oldIndexPatterns)) {
      final PropertyChangeEvent event =
        new PropertyChangeEvent(this, IndexPatternProvider.PROP_INDEX_PATTERNS, oldTodoPatterns, todoPatterns);
      myMessageBus.syncPublisher(IndexPatternProvider.INDEX_PATTERNS_CHANGED).propertyChange(event);
    }

    // only trigger gui and code daemon refresh when either the index patterns or presentation attributes have changed
    if (!Arrays.deepEquals(myTodoPatterns, oldTodoPatterns)) {
      myTopic.propertyChange(new PropertyChangeEvent(this, PROP_TODO_PATTERNS, oldTodoPatterns, todoPatterns));
    }
  }

  /**
   * @return <code>TodoFilter</code> with specified <code>name</code>. Method returns
   *         <code>null</code> if there is no filter with <code>name</code>.
   */
  public TodoFilter getTodoFilter(String name) {
    for (TodoFilter filter : myTodoFilters) {
      if (filter.getName().equals(name)) {
        return filter;
      }
    }
    return null;
  }

  /**
   * @return all <code>TodoFilter</code>s.
   */
  @Nonnull
  public TodoFilter[] getTodoFilters() {
    return myTodoFilters;
  }

  public void setTodoFilters(@Nonnull TodoFilter[] filters) {
    TodoFilter[] oldFilters = myTodoFilters;
    myTodoFilters = filters;
    myTopic.propertyChange(new PropertyChangeEvent(this, PROP_TODO_FILTERS, oldFilters, filters));
  }

  @Override
  public void loadState(Element state) {
    List<TodoPattern> patternsList = new ArrayList<>();
    List<TodoFilter> filtersList = new ArrayList<>();
    for (Element child : state.getChildren()) {
      if (ELEMENT_PATTERN.equals(child.getName())) {
        TodoPattern pattern = new TodoPattern(TodoAttributesUtil.createDefault());
        pattern.readExternal(child);
        patternsList.add(pattern);
      }
      else if (ELEMENT_FILTER.equals(child.getName())) {
        TodoPattern[] patterns = patternsList.toArray(new TodoPattern[patternsList.size()]);
        TodoFilter filter = new TodoFilter();
        filter.readExternal(child, patterns);
        filtersList.add(filter);
      }
    }
    doSetTodoPatterns(patternsList.toArray(new TodoPattern[patternsList.size()]), false);
    setTodoFilters(filtersList.toArray(new TodoFilter[filtersList.size()]));
  }

  @Nullable
  @Override
  public Element getState() {
    Element stateElement = new Element("state");
    final TodoPattern[] todoPatterns = myTodoPatterns;
    for (TodoPattern pattern : todoPatterns) {
      Element child = new Element(ELEMENT_PATTERN);
      pattern.writeExternal(child);
      stateElement.addContent(child);
    }
    for (TodoFilter filter : myTodoFilters) {
      Element child = new Element(ELEMENT_FILTER);
      filter.writeExternal(child, todoPatterns);
      stateElement.addContent(child);
    }
    return stateElement;
  }

  public void colorSettingsChanged() {
    for (TodoPattern pattern : myTodoPatterns) {
      TodoAttributes attributes = pattern.getAttributes();
      if (!attributes.shouldUseCustomTodoColor()) {
        attributes.setUseCustomTodoColor(false, TodoAttributesUtil.getDefaultColorSchemeTextAttributes());
      }
    }
  }
}
