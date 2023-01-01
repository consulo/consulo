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
package consulo.language.impl.internal.pattern.compiler;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.language.pattern.compiler.PatternClassProvider;
import consulo.language.pattern.compiler.PatternCompiler;
import consulo.language.pattern.compiler.PatternCompilerFactory;
import consulo.util.collection.ArrayUtil;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Gregory.Shrago
 *
 * TODO make it extension add/remove safe, use {@link consulo.component.extension.ExtensionPointCacheKey}
 */
@Singleton
@ServiceImpl
public class PatternCompilerFactoryImpl extends PatternCompilerFactory {
  private final Map<String, Class[]> myClasses = ConcurrentFactoryMap.createMap(key -> {
    final ArrayList<Class> result = new ArrayList<>();
    final List<String> typeList = key == null ? null : Arrays.asList(key.split(",|\\s"));
    for (PatternClassProvider bean : Application.get().getExtensionPoint(PatternClassProvider.class)) {
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
  @SuppressWarnings("unchecked")
  public <T> PatternCompiler<T> getPatternCompiler(@Nonnull Class[] patternClasses) {
    return myCompilers.get(List.of(patternClasses));
  }
}
