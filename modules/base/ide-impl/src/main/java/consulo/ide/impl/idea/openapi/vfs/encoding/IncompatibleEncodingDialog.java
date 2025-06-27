// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

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
        @Nonnull final Charset charset,
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
    @NonNls
    protected JComponent createCenterPanel() {
        JLabel label = new JLabel(XmlStringUtil.wrapInHtml(
            "The encoding you've chosen ('" + charset.displayName() + "')" +
                " may change the contents of '" + virtualFile.getName() + "'.<br>" +
                "Do you want to<br>" +
                "1. <b>Reload</b> the file from disk in the new encoding '" + charset.displayName() + "'" +
                " and overwrite editor contents or<br>" +
                "2. <b>Convert</b> the text and overwrite file in the new encoding?"
        ));
        label.setIcon(TargetAWT.to(Messages.getQuestionIcon()));
        label.setIconTextGap(10);
        return label;
    }

    @Nonnull
    @Override
    @NonNls
    protected Action[] createActions() {
        DialogWrapperAction reloadAction = new DialogWrapperAction("Reload") {
            @Override
            protected void doAction(ActionEvent e) {
                if (safeToReload == EncodingUtil.Magic8.NO_WAY) {
                    Ref<Charset> current = Ref.create();
                    EncodingUtil.FailReason failReason = EncodingUtil.checkCanReload(virtualFile, current);
                    int res;
                    byte[] bom = virtualFile.getBOM();
                    String explanation = "<br><br>" + (failReason == null ? "" : "Why: " + failReason + "<br>") +
                        (current.isNull() ? "" : "Current encoding: '" + current.get().displayName() + "'");
                    if (bom != null) {
                        Messages.showErrorDialog(
                            XmlStringUtil.wrapInHtml(
                                "File '" + virtualFile.getName() + "' can't be reloaded" +
                                    " in the '" + charset.displayName() + "' encoding." + explanation
                            ),
                            "Incompatible Encoding: " + charset.displayName()
                        );
                        res = -1;
                    }
                    else {
                        res = Messages.showDialog(
                            XmlStringUtil.wrapInHtml(
                                "File '" + virtualFile.getName() + "' most likely isn't stored" +
                                    " in the '" + charset.displayName() + "' encoding." + explanation
                            ),
                            "Incompatible Encoding: " + charset.displayName(),
                            new String[]{"Reload anyway", "Cancel"},
                            1,
                            AllIcons.General.WarningDialog
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
            reloadAction.putValue(Action.SMALL_ICON, AllIcons.General.Warning);
        }
        reloadAction.putValue(Action.MNEMONIC_KEY, (int) 'R');
        DialogWrapperAction convertAction = new DialogWrapperAction("Convert") {
            @Override
            protected void doAction(ActionEvent e) {
                if (safeToConvert == EncodingUtil.Magic8.NO_WAY) {
                    EncodingUtil.FailReason error = EncodingUtil.checkCanConvert(virtualFile);
                    int res = Messages.showDialog(
                        XmlStringUtil.wrapInHtml(
                            "Please do not convert to '" + charset.displayName() + "'.<br><br>" +
                                (
                                    error == null
                                        ? "Encoding '" + charset.displayName() + "' does not support some characters from the text."
                                        : EncodingUtil.reasonToString(error, virtualFile)
                                )
                        ),
                        "Incompatible Encoding: " + charset.displayName(),
                        new String[]{"Convert anyway", "Cancel"},
                        1,
                        AllIcons.General.WarningDialog
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
            convertAction.putValue(Action.SMALL_ICON, AllIcons.General.Warning);
        }
        convertAction.putValue(Action.MNEMONIC_KEY, (int) 'C');
        Action cancelAction = getCancelAction();
        cancelAction.putValue(DEFAULT_ACTION, Boolean.TRUE);
        return new Action[]{reloadAction, convertAction, cancelAction};
    }

    static final int RELOAD_EXIT_CODE = 10;
    static final int CONVERT_EXIT_CODE = 20;
}
