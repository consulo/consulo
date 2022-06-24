// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.dataContext.DataContext;
import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.project.Project;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.lang.Pair;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFilePropertyEvent;
import consulo.ide.impl.idea.openapi.vfs.encoding.ChangeFileEncodingAction;
import consulo.ide.impl.idea.openapi.vfs.encoding.EncodingManagerImpl;
import consulo.ide.impl.idea.openapi.vfs.encoding.EncodingManagerListener;
import consulo.ide.impl.idea.openapi.vfs.encoding.EncodingUtil;
import consulo.virtualFileSystem.event.BulkVirtualFileListenerAdapter;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ide.impl.idea.util.ObjectUtils;
import consulo.component.messagebus.MessageBusConnection;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.nio.charset.Charset;

public class EncodingPanel extends EditorBasedStatusBarPopup {
  public EncodingPanel(@Nonnull Project project) {
    super(project, true);
  }

  @Nonnull
  @Override
  protected WidgetState getWidgetState(@Nullable VirtualFile file) {
    if (file == null) {
      return WidgetState.HIDDEN;
    }

    Pair<Charset, String> check = EncodingUtil.getCharsetAndTheReasonTooltip(file);
    String failReason = Pair.getSecond(check);
    Charset charset = ObjectUtils.notNull(Pair.getFirst(check), file.getCharset());
    String charsetName = ObjectUtils.notNull(charset.displayName(), "n/a");
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
      public void propertyChanged(@Nullable Document document, @Nonnull String propertyName, Object oldValue, Object newValue) {
        if (propertyName.equals(EncodingManagerImpl.PROP_CACHED_ENCODING_CHANGED)) {
          updateForDocument(document);
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
    return new EncodingPanel(project);
  }

  @Override
  @Nonnull
  public String ID() {
    return StatusBar.StandardWidgets.ENCODING_PANEL;
  }
}
