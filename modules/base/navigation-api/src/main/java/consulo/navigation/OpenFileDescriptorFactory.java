/*
 * Copyright 2013-2022 consulo.io
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
package consulo.navigation;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.ComponentManager;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author VISTALL
 * @since 19-Feb-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface OpenFileDescriptorFactory {
  
  static OpenFileDescriptorFactory getInstance(ComponentManager project) {
    return project.getInstance(OpenFileDescriptorFactory.class);
  }

  interface Builder {
    
    Builder offset(int offset);

    
    Builder line(int line);

    
    Builder column(int column);

    
    Builder persist();

    
    Builder useCurrentWindow(boolean useCurrentWindow);

    
    OpenFileDescriptor build();
  }

  
  @Deprecated
  @DeprecationInfo("Use #newBuilder")
  default Builder builder(VirtualFile file) {
    return newBuilder(file);
  }

  
  Builder newBuilder(VirtualFile file);
}
