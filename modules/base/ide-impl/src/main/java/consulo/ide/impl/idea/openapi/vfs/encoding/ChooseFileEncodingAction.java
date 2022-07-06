// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.application.AllIcons;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.language.impl.internal.psi.LoadTextUtil;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ide.impl.idea.openapi.util.NotNullLazyValue;
import consulo.util.lang.Pair;
import consulo.ide.impl.idea.openapi.util.VolatileNotNullLazyValue;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.IconDeferrer;
import consulo.ide.impl.idea.util.Function;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class ChooseFileEncodingAction extends ComboBoxAction {
  private final VirtualFile myVirtualFile;

  protected ChooseFileEncodingAction(@Nullable VirtualFile virtualFile) {
    myVirtualFile = virtualFile;
  }

  @Override
  public abstract void update(@Nonnull final AnActionEvent e);

  private void fillCharsetActions(@Nonnull DefaultActionGroup group,
                                  @Nullable final VirtualFile virtualFile,
                                  @Nonnull List<? extends Charset> charsets,
                                  @Nonnull final Function<? super Charset, String> charsetFilter) {
    for (final Charset charset : charsets) {
      AnAction action = new DumbAwareAction(charset.displayName(), null, Image.empty(Image.DEFAULT_ICON_SIZE)) {
        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
          chosen(virtualFile, charset);
        }

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
          super.update(e);
          String description = charsetFilter.fun(charset);
          Image defer;
          if (virtualFile == null || virtualFile.isDirectory()) {
            defer = null;
          }
          else {
            NotNullLazyValue<CharSequence> myText = VolatileNotNullLazyValue.createValue(() -> LoadTextUtil.loadText(virtualFile));
            NotNullLazyValue<byte[]> myBytes = VolatileNotNullLazyValue.createValue(() -> {
              try {
                return virtualFile.contentsToByteArray();
              }
              catch (IOException e1) {
                return ArrayUtil.EMPTY_BYTE_ARRAY;
              }
            });
            defer = IconDeferrer.getInstance().defer(null, Pair.create(virtualFile, charset), pair -> {
              VirtualFile myFile = pair.getFirst();
              Charset charset = pair.getSecond();
              CharSequence text = myText.getValue();
              byte[] bytes = myBytes.getValue();
              EncodingUtil.Magic8 safeToReload = EncodingUtil.isSafeToReloadIn(myFile, text, bytes, charset);
              EncodingUtil.Magic8 safeToConvert = EncodingUtil.Magic8.ABSOLUTELY;
              if (safeToReload != EncodingUtil.Magic8.ABSOLUTELY) {
                safeToConvert = EncodingUtil.isSafeToConvertTo(myFile, text, bytes, charset);
              }
              return safeToReload == EncodingUtil.Magic8.ABSOLUTELY || safeToConvert == EncodingUtil.Magic8.ABSOLUTELY
                     ? null
                     : safeToReload == EncodingUtil.Magic8.WELL_IF_YOU_INSIST || safeToConvert == EncodingUtil.Magic8.WELL_IF_YOU_INSIST ? AllIcons.General.Warning : AllIcons.General.Error;
            });
          }
          e.getPresentation().setIcon(defer);
          e.getPresentation().setDescription(description);
        }
      };
      group.add(action);
    }
  }

  public static final Charset NO_ENCODING = new Charset("NO_ENCODING", null) {
    @Override
    public boolean contains(final Charset cs) {
      return false;
    }

    @Override
    public CharsetDecoder newDecoder() {
      return null;
    }

    @Override
    public CharsetEncoder newEncoder() {
      return null;
    }
  };

  protected abstract void chosen(@Nullable VirtualFile virtualFile, @Nonnull Charset charset);

  @Nonnull
  protected DefaultActionGroup createCharsetsActionGroup(@Nullable String clearItemText, @Nullable Charset alreadySelected, @Nonnull Function<? super Charset, String> charsetFilter) {
    DefaultActionGroup group = new DefaultActionGroup();
    List<Charset> favorites = new ArrayList<>(EncodingManager.getInstance().getFavorites());
    Collections.sort(favorites);
    Charset current = myVirtualFile == null ? null : myVirtualFile.getCharset();
    favorites.remove(current);
    favorites.remove(alreadySelected);

    if (clearItemText != null) {
      String description = "Clear " + (myVirtualFile == null ? "default" : "file '" + myVirtualFile.getName() + "'") + " encoding.";
      group.add(new DumbAwareAction(clearItemText, description, null) {
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
          chosen(myVirtualFile, NO_ENCODING);
        }
      });
    }
    if (favorites.isEmpty() && clearItemText == null) {
      fillCharsetActions(group, myVirtualFile, Arrays.asList(CharsetToolkit.getAvailableCharsets()), charsetFilter);
    }
    else {
      fillCharsetActions(group, myVirtualFile, favorites, charsetFilter);

      DefaultActionGroup more = new DefaultActionGroup("more", true);
      group.add(more);
      fillCharsetActions(more, myVirtualFile, Arrays.asList(CharsetToolkit.getAvailableCharsets()), charsetFilter);
    }
    return group;
  }
}
