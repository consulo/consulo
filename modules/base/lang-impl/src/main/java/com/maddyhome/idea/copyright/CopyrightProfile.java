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

package com.maddyhome.idea.copyright;

import com.intellij.util.xmlb.SmartSerializer;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.maddyhome.idea.copyright.pattern.EntityUtil;
import org.jdom.Element;

import javax.annotation.Nonnull;

public class CopyrightProfile implements Comparable<CopyrightProfile> {
  public static final String DEFAULT_COPYRIGHT_NOTICE = EntityUtil.encode("Copyright (c) $today.year. Lorem ipsum dolor sit amet, consectetur adipiscing elit. \n" +
                                                                          "Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan. \n" +
                                                                          "Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna. \n" +
                                                                          "Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus. \n" +
                                                                          "Vestibulum commodo. Ut rhoncus gravida arcu. ");

  private static final SmartSerializer ourSerializer = SmartSerializer.skipEmptySerializer();

  private String notice = DEFAULT_COPYRIGHT_NOTICE;
  private String keyword = EntityUtil.encode("Copyright");
  private String allowReplaceKeyword = "";
  @Nonnull
  protected String myName;

  //read external
  public CopyrightProfile() {
    this("");
  }

  public void copyFrom(@Nonnull CopyrightProfile profile) {
    Element config = new Element("config");
    profile.writeExternal(config);
    readExternal(config);
  }

  public void readExternal(Element element) {
    ourSerializer.readExternal(this, element);
  }

  public void writeExternal(Element element) {
    ourSerializer.writeExternal(this, element);
  }

  public CopyrightProfile(@Nonnull String profileName) {
    myName = profileName;
  }

  @Nonnull
  @OptionTag("myName")
  public String getName() {
    return myName;
  }

  public void setName(@Nonnull String name) {
    myName = name;
  }

  public String getNotice() {
    return notice;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setNotice(String text) {
    notice = text;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getAllowReplaceKeyword() {
    return allowReplaceKeyword;
  }

  public void setAllowReplaceKeyword(String allowReplaceKeyword) {
    this.allowReplaceKeyword = allowReplaceKeyword;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o instanceof CopyrightProfile && myName.equals(((CopyrightProfile)o).myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }

  @Override
  public int compareTo(@Nonnull CopyrightProfile o) {
    return getName().compareToIgnoreCase(o.getName());
  }
}
