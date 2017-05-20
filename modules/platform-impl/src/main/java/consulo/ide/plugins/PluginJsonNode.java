/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.plugins;

import com.intellij.util.ArrayUtilRt;

/**
 * @author VISTALL
 * @since 30-Aug-16
 */
public class PluginJsonNode {
  public static class Extension {
    public String key;

    public String[] values = ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  public String id;
  public String name;
  public String description;
  public String category;
  public String vendor;
  public int downloads;
  public Long length;
  public Long date;
  public Integer rating;
  public String version;
  public String platformVersion;
  public String[] dependencies;
  public String[] optionalDependencies;
  public Extension[] extensions;
}