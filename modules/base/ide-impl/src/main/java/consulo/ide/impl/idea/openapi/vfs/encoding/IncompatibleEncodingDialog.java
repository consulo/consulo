// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.charset.Charset;

public class IncompatibleEncodingDialog extends DialogWrapper {
    @Nonnull
    private final VirtualFile virtualFile;
    @Nonnull
    private final Charset charset;
    @Nonnull
    private final EncodingUtil.Magic8 safeToReload;
    @Nonnull
    private final EncodingUtil.Magic8 safeToConvert;

    IncompatibleEncodingDialog(
        @Nonnull VirtualFile virtualFile,
        @Nonnull Charset charset,
        @Nonnull EncodingUtil.Magic8 safeToReload,
        @Nonnull EncodingUtil.Magic8 safeToConvert
    ) {
        super(false);
        this.virtualFile = virtualFile;
        this.charset = charset;
        this.safeToReload = safeToReload;
        this.safeToConvert = safeToConvert;
        setTitle(virtualFile.getName() + ": Reload or Convert to " + charset.displayName());
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JLabel label = new JLabel(XmlStringUtil.wrapInHtml(
            IdeLocalize.dialogMessageIncompatibleEncoding(charset.displayName(), virtualFile.getName()).get()
        ));
        label.setIcon(TargetAWT.to(UIUtil.getQuestionIcon()));
        label.setIconTextGap(10);
        return label;
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        DialogWrapperAction reloadAction = new DialogWrapperAction(IdeLocalize.buttonReload()) {
            @Override
            @RequiredUIAccess
            protected void doAction(ActionEvent e) {
                if (safeToReload == EncodingUtil.Magic8.NO_WAY) {
                    SimpleReference<Charset> current = SimpleReference.create();
                    EncodingUtil.FailReason failReason = EncodingUtil.checkCanReload(virtualFile, current);
                    int res;
                    byte[] bom = virtualFile.getBOM();
                    String explanation = "<br><br>" + (failReason == null ? "" : "Why: " + failReason + "<br>") +
                        (current.isNull() ? "" : "Current encoding: '" + current.get().displayName() + "'");
                    if (bom != null) {
                        Messages.showErrorDialog(
                            XmlStringUtil.wrapInHtml(
                                IdeLocalize.dialogTitleFile0CanTBeReloaded(virtualFile.getName(), charset.displayName(), explanation).get()
                            ),
                            IdeLocalize.incompatibleEncodingDialogTitle(charset.displayName()).get()
                        );
                        res = -1;
                    }
                    else {
                        res = Messages.showDialog(
                            XmlStringUtil.wrapInHtml(IdeLocalize.dialogTitleFile0MostLikelyIsnTStored(
                                virtualFile.getName(),
                                charset.displayName(),
                                explanation
                            ).get()),
                            IdeLocalize.incompatibleEncodingDialogTitle(charset.displayName()).get(),
                            new String[]{IdeLocalize.buttonReloadAnyway().get(), CommonLocalize.buttonCancel().get()},
                            1,
                            PlatformIconGroup.generalWarningdialog()
                        );
                    }
                    if (res != 0) {
                        doCancelAction();
                        return;
                    }
                }
                close(RELOAD_EXIT_CODE);
            }
        };
        if (!Platform.current().os().isMac() && safeToReload == EncodingUtil.Magic8.NO_WAY) {
            reloadAction.putValue(Action.SMALL_ICON, PlatformIconGroup.generalWarning());
        }
        reloadAction.putValue(Action.MNEMONIC_KEY, (int) 'R');
        DialogWrapperAction convertAction = new DialogWrapperAction(IdeLocalize.buttonConvert()) {
            @Override
            @RequiredUIAccess
            protected void doAction(ActionEvent e) {
                if (safeToConvert == EncodingUtil.Magic8.NO_WAY) {
                    EncodingUtil.FailReason error = EncodingUtil.checkCanConvert(virtualFile);
                    int res = Messages.showDialog(
                        XmlStringUtil.wrapInHtml(
                            IdeLocalize.encodingDoNotConvertMessage(charset.displayName()) + "<br><br>" +
                                (error == null
                                    ? IdeLocalize.encodingUnsupportedCharactersMessage(charset.displayName()).get()
                                    : EncodingUtil.reasonToString(error, virtualFile).get())
                        ),
                        IdeLocalize.incompatibleEncodingDialogTitle(charset.displayName()).get(),
                        new String[]{IdeLocalize.buttonConvertAnyway().get(), CommonLocalize.buttonCancel().get()},
                        1,
                        PlatformIconGroup.generalWarningdialog()
                    );
                    if (res != 0) {
                        doCancelAction();
                        return;
                    }
                }
                close(CONVERT_EXIT_CODE);
            }

            @Override
            public boolean isEnabled() {
                return !RawFileLoader.getInstance().isTooLarge(virtualFile.getLength());
            }
        };
        if (!Platform.current().os().isMac() && safeToConvert == EncodingUtil.Magic8.NO_WAY) {
            convertAction.putValue(Action.SMALL_ICON, PlatformIconGroup.generalWarning());
        }
        convertAction.putValue(Action.MNEMONIC_KEY, (int) 'C');
        Action cancelAction = getCancelAction();
        cancelAction.putValue(DEFAULT_ACTION, Boolean.TRUE);
        return new Action[]{reloadAction, convertAction, cancelAction};
    }

    static final int RELOAD_EXIT_CODE = 10;
    static final int CONVERT_EXIT_CODE = 20;
}
