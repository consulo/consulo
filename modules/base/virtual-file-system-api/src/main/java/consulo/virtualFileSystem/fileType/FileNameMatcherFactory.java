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
package consulo.virtualFileSystem.fileType;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.util.lang.StringUtil;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class FileNameMatcherFactory {
  
  public static FileNameMatcherFactory getInstance() {
    return Application.get().getInstance(FileNameMatcherFactory.class);
  }

  
  public final FileNameMatcher createMatcher(String pattern) {
    if (pattern.startsWith("*.") && pattern.indexOf('*', 2) < 0 && pattern.indexOf('.', 2) < 0 && pattern.indexOf('?', 2) < 0) {
      return createExtensionFileNameMatcher(StringUtil.toLowerCase(pattern.substring(2)));
    }

    if (pattern.contains("*") || pattern.contains("?")) {
      return createWildcardFileNameMatcher(pattern);
    }

    return createExactFileNameMatcher(pattern);
  }

  
  public FileNameMatcher createExactFileNameMatcher(String fileName) {
    return createExactFileNameMatcher(fileName, false);
  }

  
  public abstract FileNameMatcher createExtensionFileNameMatcher(String extension);

  
  public abstract FileNameMatcher createExactFileNameMatcher(String fileName, boolean ignoreCase);

  
  public abstract FileNameMatcher createWildcardFileNameMatcher(String pattern);
}
