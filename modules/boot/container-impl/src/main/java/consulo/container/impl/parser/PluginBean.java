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
package consulo.container.impl.parser;

import consulo.util.nodep.xml.node.SimpleXmlElement;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PluginBean {
  //@Tag("name")
  public String name;

  //@Tag("id")
  public String id;

  //@Tag("description")
  public String description;

  //@Tag("version")
  public String pluginVersion;

  //@Tag("platformVersion")
  public String platformVersion;

  //@Property(surroundWithTag = false)
  public PluginVendor vendor;

  //@Tag("actions")
  public List<SimpleXmlElement> actions = Collections.emptyList();

  //@Property(surroundWithTag = false)
  //@AbstractCollection(surroundWithTag = false)
  public List<PluginDependency> dependencies = Collections.emptyList();

  public List<String> incompatibleWith = Collections.emptyList();

  //@Property(surroundWithTag = false)
  //@AbstractCollection(surroundWithTag = false)
  public List<PluginHelpSet> helpSets = Collections.emptyList();

  public List<String> imports = Collections.emptyList();

  //@Tag("category")
  public String category;

  //@Tag("resource-bundle")
  public String resourceBundle;

  public String localize;

  //@Tag("change-notes")
  public String changeNotes;

  //@Attribute("url")
  public String url;

  public boolean experimental;

  public Map<String, Set<String>> permissions = Collections.emptyMap();

  public Set<String> tags = Collections.emptySet();
}
