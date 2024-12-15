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
package consulo.ide.impl.plugins;

import consulo.util.collection.ArrayUtil;

/**
 * @author VISTALL
 * @since 30-Aug-16
 */
public class PluginJsonNode {
    public class ExtensionPreview {
        public String apiPluginId;
        public String apiClassName;
        public String implId;
    }

    public static class Checksum {
        public String sha3_256;
    }

    public static class Permission {
        public String type;

        public String[] options;
    }

    public String id;
    public String name;
    public String description;
    public String category;
    public String vendor;
    public String url;
    public int downloads;
    public Long length;
    public Long date;
    public String version;
    public String iconBytes;
    public String iconDarkBytes;
    public String platformVersion;
    public String[] downloadUrls = ArrayUtil.EMPTY_STRING_ARRAY;
    public String[] dependencies;
    public String[] optionalDependencies;
    public boolean experimental;
    public Permission[] permissions;
    public String[] tags;
    public Checksum checksum = new Checksum();
    public ExtensionPreview[] extensionPreviews = new ExtensionPreview[0];
}