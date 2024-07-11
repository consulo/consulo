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
package consulo.language.content;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.component.extension.ExtensionInstance;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.TestLikeContentFolderTypeProvider;
import consulo.project.ProjectBundle;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.image.Image;
import consulo.ui.util.LightDarkColorValue;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 22:44/31.10.13
 */
@ExtensionImpl
public class TestContentFolderTypeProvider extends PackageBasedContentFolderTypeProvider implements TestLikeContentFolderTypeProvider {
  private static final ColorValue TESTS_COLOR = new LightDarkColorValue(new RGBColor(0, 140, 46), new RGBColor(73, 140, 101));
  private static final Supplier<TestContentFolderTypeProvider> INSTANCE = ExtensionInstance.from(ContentFolderTypeProvider.class
  );

  @Nonnull
  public static TestContentFolderTypeProvider getInstance() {
    return INSTANCE.get();
  }

  public TestContentFolderTypeProvider() {
    super("TEST");
  }

  @Override
  public int getWeight() {
    return 150;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Modules.TestRoot;
  }

  @Override
  public Image getChildPackageIcon() {
    return AllIcons.Nodes.TestPackage;
  }

  @Nonnull
  @Override
  public String getName() {
    return ProjectBundle.message("module.toggle.test.sources.action");
  }

  @Nonnull
  @Override
  public ColorValue getGroupColor() {
    return TESTS_COLOR;
  }
}
