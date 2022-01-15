/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.patterns.compiler;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ConcurrentFactoryMap;

import javax.annotation.Nonnull;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Gregory.Shrago
 */
@Singleton
public class PatternCompilerFactoryImpl extends PatternCompilerFactory {
  public static final ExtensionPointName<PatternClassBean> PATTERN_CLASS_EP = ExtensionPointName.create("com.intellij.patterns.patternClass");


  private final Map<String, Class[]> myClasses = ConcurrentFactoryMap.createMap(key -> {
    final ArrayList<Class> result = new ArrayList<Class>(1);
    final List<String> typeList = key == null ? null : Arrays.asList(key.split(",|\\s"));
    for (PatternClassBean bean : PATTERN_CLASS_EP.getExtensions()) {
      if (typeList == null || typeList.contains(bean.getAlias())) result.add(bean.getPatternClass());
    }
    return result.isEmpty() ? ArrayUtil.EMPTY_CLASS_ARRAY : result.toArray(new Class[result.size()]);
  });

  private final Map<List<Class>, PatternCompiler> myCompilers = ConcurrentFactoryMap.createMap(PatternCompilerImpl::new);

  @Nonnull
  @Override
  public Class[] getPatternClasses(String alias) {
    return myClasses.get(alias);
  }

  @Nonnull
  @Override
  public <T> PatternCompiler<T> getPatternCompiler(@Nonnull Class[] patternClasses) {
    return myCompilers.get(Arrays.asList(patternClasses));
  }
}
