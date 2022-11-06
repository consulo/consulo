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
package consulo.sandboxPlugin.ide.fileEditor;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.FileEditor;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.ui.Alerts;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 18-Jul-22
 */
@ExtensionImpl
public class SandEditorNotificationProvider implements EditorNotificationProvider {
  @Nonnull
  @Override
  public String getId() {
    return "sand";
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationBuilder buildNotification(@Nonnull VirtualFile file, @Nonnull FileEditor fileEditor, @Nonnull Supplier<EditorNotificationBuilder> builderFactory) {
    if (file.getFileType() != SandFileType.INSTANCE) {
      return null;
    }

    EditorNotificationBuilder builder = builderFactory.get();
    builder.withText(LocalizeValue.localizeTODO("Sand text"));
    builder.withIcon(PlatformIconGroup.nodesStatic());
    builder.withAction(LocalizeValue.localizeTODO("Hello World"), (e) ->{
      IPopupChooserBuilder<Object> chooserBuilder = JBPopupFactory.getInstance().createPopupChooserBuilder(List.of("Value 1", "Value 2"));
      chooserBuilder.createPopup().showBy(e);
    });
    builder.withGearAction((e) -> Alerts.okInfo(LocalizeValue.localizeTODO("Hello World")).showAsync());
    return builder;
  }
}
