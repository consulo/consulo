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

package com.intellij.diagnostic.logging;

import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import consulo.ui.image.Image;
import org.jdom.Element;

import javax.annotation.Nullable;

/**
 * User: anna
 * Date: 22-Mar-2006
 */
public class LogFilter implements JDOMExternalizable {
  public String myName;
  private Image myIcon;

  public LogFilter(final String name, final Image icon) {
    myName = name;
    myIcon = icon;
  }

  public LogFilter(String name) {
    myName = name;
  }

  //read external
  public LogFilter() {
  }

  @Override
  public String toString() {
    return myName;
  }

  public void setIcon(final Image icon) {
    myIcon = icon;
  }

  public boolean isAcceptable(String line){
    return true;
  }

  public String getName(){
    return myName;
  }

  @Nullable
  public Image getIcon() {
    return myIcon;
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    DefaultJDOMExternalizer.writeExternal(this, element);
  }
}
