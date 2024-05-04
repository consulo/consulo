// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.ide.impl.idea.openapi.vfs.encoding.ChangeFileEncodingAction;
import consulo.ide.impl.idea.openapi.vfs.encoding.EncodingManagerImpl;
import consulo.ide.impl.idea.openapi.vfs.encoding.EncodingUtil;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingManagerListener;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.BulkVirtualFileListenerAdapter;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFilePropertyEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.charset.Charset;

public class EncodingPanel extends EditorBasedStatusBarPopup {
  public EncodingPanel(@Nonnull Project project, @Nonnull StatusBarWidgetFactory factory) {
    super(project, factory, true);
  }

  @Nonnull
  @Override
  protected WidgetState getWidgetState(@Nullable VirtualFile file) {
    if (file == null) {
      return WidgetState.HIDDEN;
    }

    Pair<Charset, String> check = EncodingUtil.getCharsetAndTheReasonTooltip(file);
    String failReason = Pair.getSecond(check);
    Charset charset = ObjectUtil.notNull(Pair.getFirst(check), file.getCharset());
    String charsetName = ObjectUtil.notNull(charset.displayName(), "n/a");
    String toolTipText = failReason == null ? "File Encoding: " + charsetName : StringUtil.capitalize(failReason) + ".";
    return new WidgetState(toolTipText, charsetName, failReason == null);
  }

  @Nullable
  @Override
  protected ListPopup createPopup(DataContext context) {
    ChangeFileEncodingAction action = new ChangeFileEncodingAction();
    action.getTemplatePresentation().setText("File Encoding");
    return action.createPopup(context);
  }

  @Override
  protected void registerCustomListeners() {
    MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(this);

    // should update to reflect encoding-from-content
    connection.subscribe(EncodingManagerListener.class, new EncodingManagerListener() {
      @Override
      public void propertyChanged(@Nullable Object document, @Nonnull String propertyName, Object oldValue, Object newValue) {
        if (propertyName.equals(EncodingManagerImpl.PROP_CACHED_ENCODING_CHANGED)) {
          updateForDocument((Document)document);
        }
      }
    });

    connection.subscribe(BulkFileListener.class, new BulkVirtualFileListenerAdapter(new VirtualFileListener() {
      @Override
      public void propertyChanged(@Nonnull VirtualFilePropertyEvent event) {
        if (VirtualFile.PROP_ENCODING.equals(event.getPropertyName())) {
          updateForFile(event.getFile());
        }
      }
    }));
  }

  @Nonnull
  @Override
  protected StatusBarWidget createInstance(@Nonnull Project project) {
    return new EncodingPanel(project, myFactory);
  }
}
