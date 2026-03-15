// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.progress;

import consulo.build.ui.BuildDescriptor;


public interface BuildProgressDescriptor {
  
  public static BuildProgressDescriptor of(BuildDescriptor descriptor) {
    return new BuildProgressDescriptor() {
      
      @Override
      public String getTitle() {
        return descriptor.getTitle();
      }

      
      @Override
      public BuildDescriptor getBuildDescriptor() {
        return descriptor;
      }
    };
  }

  
  String getTitle();

  
  BuildDescriptor getBuildDescriptor();
}
