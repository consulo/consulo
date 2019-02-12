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
package com.intellij.xdebugger.frame;

import com.intellij.util.SmartList;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents chunk of values which can be added to a {@link XCompositeNode composite node}
 *
 * @author nik
 * @see XCompositeNode#addChildren(XValueChildrenList, boolean)
 */
public class XValueChildrenList {
  public static XValueChildrenList singleton(String name, @Nonnull XValue value) {
    return new XValueChildrenList(Collections.singletonList(name), Collections.singletonList(value));
  }

  public static XValueChildrenList singleton(@Nonnull XNamedValue value) {
    return new XValueChildrenList(Collections.singletonList(value.getName()), Collections.<XValue>singletonList(value));
  }

  public static XValueChildrenList bottomGroup(@Nonnull XValueGroup group) {
    XValueChildrenList list = new XValueChildrenList();
    list.addBottomGroup(group);
    return list;
  }

  public static XValueChildrenList topGroups(@Nonnull List<XValueGroup> topGroups) {
    return new XValueChildrenList(Collections.emptyList(), Collections.emptyList(), topGroups);
  }

  public static final XValueChildrenList EMPTY =
          new XValueChildrenList(Collections.<String>emptyList(), Collections.<XValue>emptyList(), Collections.<XValueGroup>emptyList());
  private final List<String> myNames;
  private final List<XValue> myValues;
  private final List<XValueGroup> myTopGroups;
  private final List<XValueGroup> myBottomGroups = new SmartList<>();

  public XValueChildrenList(int initialCapacity) {
    this(new ArrayList<>(initialCapacity), new ArrayList<>(initialCapacity), new SmartList<>());
  }

  public XValueChildrenList() {
    this(new SmartList<>(), new SmartList<>(), new SmartList<>());
  }

  private XValueChildrenList(List<String> names, List<XValue> values) {
    this(names, values, new SmartList<>());
  }

  private XValueChildrenList(@Nonnull List<String> names, @Nonnull List<XValue> values, @Nonnull List<XValueGroup> topGroups) {
    myNames = names;
    myValues = values;
    myTopGroups = topGroups;
  }

  public void add(@NonNls String name, @Nonnull XValue value) {
    myNames.add(name);
    myValues.add(value);
  }

  public void add(@Nonnull XNamedValue value) {
    myNames.add(value.getName());
    myValues.add(value);
  }

  /**
   * Adds a node representing group of values to the top of a node children list
   */
  public void addTopGroup(@Nonnull XValueGroup group) {
    myTopGroups.add(group);
  }

  /**
   * Adds a node representing group of values to the bottom of a node children list
   */
  public void addBottomGroup(@Nonnull XValueGroup group) {
    myBottomGroups.add(group);
  }

  public int size() {
    return myNames.size();
  }

  public String getName(int i) {
    return myNames.get(i);
  }

  public XValue getValue(int i) {
    return myValues.get(i);
  }

  public List<XValueGroup> getTopGroups() {
    return myTopGroups;
  }

  public List<XValueGroup> getBottomGroups() {
    return myBottomGroups;
  }
}