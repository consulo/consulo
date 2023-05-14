// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.progress;

import consulo.build.ui.BuildDescriptor;

import jakarta.annotation.Nonnull;

public interface BuildProgressDescriptor {
  @Nonnull
  public static BuildProgressDescriptor of(@Nonnull BuildDescriptor descriptor) {
    return new BuildProgressDescriptor() {
      @Nonnull
      @Override
      public String getTitle() {
        return descriptor.getTitle();
      }

      @Nonnull
      @Override
      public BuildDescriptor getBuildDescriptor() {
        return descriptor;
      }
    };
  }

  @Nonnull
  String getTitle();

  @Nonnull
  BuildDescriptor getBuildDescriptor();
}
