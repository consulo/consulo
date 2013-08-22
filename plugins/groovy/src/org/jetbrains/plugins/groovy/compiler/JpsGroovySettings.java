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
package org.jetbrains.plugins.groovy.compiler;

import com.intellij.util.xmlb.annotations.Tag;
import org.jdom.Element;

/**
 * @author peter
 */
public class JpsGroovySettings {
  public static final String DEFAULT_HEAP_SIZE = "400";
  public static final boolean DEFAULT_INVOKE_DYNAMIC = false;
  public static final boolean DEFAULT_TRANSFORMS_OK = false;

  public String heapSize = DEFAULT_HEAP_SIZE;
  public boolean invokeDynamic = DEFAULT_INVOKE_DYNAMIC;

  @Tag("excludes") public Element excludes = new Element("aaa");

  public boolean transformsOk = DEFAULT_TRANSFORMS_OK;
}
