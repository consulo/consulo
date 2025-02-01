/*
 * Copyright 2013-2021 consulo.io
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
package consulo.container.internal.plugin.classloader;

import java.util.List;

/**
 * @author VISTALL
 * @since 27/04/2021
 */
public interface Java9ModuleProcessor {
  Java9ModuleProcessor EMPTY = new Java9ModuleProcessor() {
    @Override
    public void process(List<Opens> toOpenMap) {
    }

    @Override
    public boolean isEnabledModules() {
      return false;
    }
  };

  class Opens {
    public final String fromModuleName;
    public final String packageName;
    public final String toModuleName;

    public Opens(String fromModuleName, String packageName, String toModuleName) {
      this.fromModuleName = fromModuleName;
      this.packageName = packageName;
      this.toModuleName = toModuleName;
    }
  }

  void process(List<Opens> toOpenMap);

  boolean isEnabledModules();
}
