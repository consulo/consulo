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
package consulo.ui.internal;

import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public class WGwtTreeImpl<N> extends WGwtBaseComponent {
  private N myRootNode;
  private NotNullFunction<N, Collection<N>> myNodeResolver = new NotNullFunction<N, Collection<N>>() {
    @NotNull
    @Override
    public Collection<N> fun(N dom) {
      return Collections.emptyList();
    }
  };

  public WGwtTreeImpl(N rootNode) {
    myRootNode = rootNode;
  }

  public void setNodeResolver(@NotNull NotNullFunction<N, Collection<N>> resolver) {
    myNodeResolver = resolver;
  }
}
