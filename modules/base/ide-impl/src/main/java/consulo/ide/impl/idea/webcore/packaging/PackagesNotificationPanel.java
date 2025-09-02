// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.webcore.packaging;

import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.ide.impl.idea.util.ui.SwingHelper;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.repository.ui.PackageManagementService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.HyperlinkAdapter;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PackagesNotificationPanel {
    private final JEditorPane myHtmlViewer;
    private final Map<String, Runnable> myLinkHandlers = new HashMap<>();
    private String myErrorTitle;
    private PackageManagementService.ErrorDescription myErrorDescription;

    public PackagesNotificationPanel() {
        myHtmlViewer = SwingHelper.createHtmlViewer(true, null, null, null);
        myHtmlViewer.setVisible(false);
        myHtmlViewer.setOpaque(true);
        myHtmlViewer.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            @RequiredUIAccess
            protected void hyperlinkActivated(HyperlinkEvent e) {
                Runnable handler = myLinkHandlers.get(e.getDescription());
                if (handler != null) {
                    handler.run();
                }
                else if (myErrorTitle != null && myErrorDescription != null) {
                    showError(myErrorTitle, myErrorDescription);
                }
            }
        });
    }

    @RequiredUIAccess
    public static void showError(@Nonnull String title, @Nonnull PackageManagementService.ErrorDescription description) {
        PackagingErrorDialog dialog = new PackagingErrorDialog(title, description);
        dialog.showAsync();
    }

    public void showResult(String packageName, @Nullable PackageManagementService.ErrorDescription errorDescription) {
        if (errorDescription == null) {
            LocalizeValue message = IdeLocalize.packageInstalledSuccessfully();
            if (packageName != null) {
                message = IdeLocalize.package0InstalledSuccessfully(packageName);
            }
            showSuccess(message.get());
        }
        else {
            LocalizeValue title = IdeLocalize.failedToInstallPackagesDialogTitle();
            if (packageName != null) {
                title = IdeLocalize.failedToInstallPackageDialogTitle(packageName);
            }
            LocalizeValue text = IdeLocalize.installPackageFailure(packageName);
            showError(text.get(), title.get(), errorDescription);
        }
    }

    public void addLinkHandler(String key, Runnable handler) {
        myLinkHandlers.put(key, handler);
    }

    public void removeAllLinkHandlers() {
        myLinkHandlers.clear();
    }

    public JComponent getComponent() {
        return myHtmlViewer;
    }

    public void showSuccess(String text) {
        showContent(text, MessageType.INFO.getPopupBackground());
    }

    private void showContent(@Nonnull String text, @Nonnull Color background) {
        String htmlText = text.startsWith("<html>") ? text : UIUtil.toHtml(text);
        myHtmlViewer.setText(htmlText);
        myHtmlViewer.setBackground(background);
        setVisibleEditorPane(true);
        myErrorTitle = null;
        myErrorDescription = null;
    }

    public void showError(String text, @Nullable String detailsTitle, PackageManagementService.ErrorDescription errorDescription) {
        showContent(text, MessageType.ERROR.getPopupBackground());
        myErrorTitle = detailsTitle;
        myErrorDescription = errorDescription;
    }

    public void showWarning(String text) {
        showContent(text, MessageType.WARNING.getPopupBackground());
    }

    public void hide() {
        setVisibleEditorPane(false);
    }

    private void setVisibleEditorPane(boolean visible) {
        boolean oldVisible = myHtmlViewer.isVisible();
        myHtmlViewer.setVisible(visible);
        if (oldVisible != visible) {
            myHtmlViewer.revalidate();
            myHtmlViewer.repaint();
        }
    }

    public boolean hasLinkHandler(String key) {
        return myLinkHandlers.containsKey(key);
    }

    public void removeLinkHandler(String key) {
        myLinkHandlers.remove(key);
    }
}
