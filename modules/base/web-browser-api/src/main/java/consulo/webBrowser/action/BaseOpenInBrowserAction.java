/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.webBrowser.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.io.Url;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import consulo.webBrowser.*;
import consulo.webBrowser.localize.WebBrowserLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

public abstract class BaseOpenInBrowserAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(BaseOpenInBrowserAction.class);

    protected BaseOpenInBrowserAction(@Nonnull WebBrowser browser) {
        super(browser.getName(), null, browser.getIcon());
    }

    @SuppressWarnings("UnusedDeclaration")
    protected BaseOpenInBrowserAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
        super(text, description, icon);
    }

    @Nullable
    protected abstract WebBrowser getBrowser(@Nonnull AnActionEvent event);

    @Override
    public final void update(@Nonnull AnActionEvent e) {
        WebBrowser browser = getBrowser(e);
        if (browser == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        Pair<OpenInBrowserRequest, WebBrowserUrlProvider> result = doUpdate(e);
        if (result == null) {
            return;
        }

        String description = getTemplatePresentation().getText();
        if (ActionPlaces.CONTEXT_TOOLBAR.equals(e.getPlace())) {
            StringBuilder builder = new StringBuilder(description);
            builder.append(" (");
            Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts("WebOpenInAction");
            boolean exists = shortcuts.length > 0;
            if (exists) {
                builder.append(KeymapUtil.getShortcutText(shortcuts[0]));
            }

            if (WebFileFilter.isFileAllowed(result.first.getFile())) {
                builder.append(exists ? ", " : "").append("hold Shift to open URL of local file");
            }
            builder.append(')');
            description = builder.toString();
        }
        e.getPresentation().setText(description);
    }

    @Override
    @RequiredUIAccess
    public final void actionPerformed(@Nonnull AnActionEvent e) {
        WebBrowser browser = getBrowser(e);
        if (browser != null) {
            open(e, browser);
        }
    }

    @Nullable
    public static OpenInBrowserRequest createRequest(@Nonnull DataContext context) {
        final Editor editor = context.getData(Editor.KEY);
        if (editor != null) {
            Project project = editor.getProject();
            if (project != null && project.isInitialized()) {
                PsiFile psiFile = ReadAction.compute(() -> context.getData(PsiFile.KEY));
                if (psiFile == null) {
                    psiFile = ReadAction.compute(() -> PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()));
                }
                if (psiFile != null) {
                    return new OpenInBrowserRequest(psiFile) {
                        private PsiElement element;

                        @Nonnull
                        @Override
                        @RequiredReadAction
                        public PsiElement getElement() {
                            if (element == null) {
                                element = getFile().findElementAt(editor.getCaretModel().getOffset());
                            }
                            return ObjectUtil.chooseNotNull(element, getFile());
                        }
                    };
                }
            }
        }
        else {
            PsiFile psiFile = ReadAction.compute(() -> context.getData(PsiFile.KEY));
            VirtualFile virtualFile = context.getData(VirtualFile.KEY);
            Project project = context.getData(Project.KEY);
            if (virtualFile != null && !virtualFile.isDirectory() && virtualFile.isValid() && project != null && project.isInitialized()) {
                psiFile = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(virtualFile));
            }

            if (psiFile != null) {
                return OpenInBrowserRequest.create(psiFile);
            }
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    public static Pair<OpenInBrowserRequest, WebBrowserUrlProvider> doUpdate(@Nonnull AnActionEvent event) {
        OpenInBrowserRequest request = createRequest(event.getDataContext());
        boolean applicable = false;
        WebBrowserUrlProvider provider = null;
        if (request != null) {
            applicable = WebFileFilter.isFileAllowed(request.getFile()) && !(request.getVirtualFile() instanceof LightVirtualFileBase);
            if (!applicable) {
                provider = WebBrowserService.getInstance().getProvider(request);
                applicable = provider != null;
            }
        }

        event.getPresentation().setEnabledAndVisible(applicable);
        return applicable ? Pair.create(request, provider) : null;
    }

    @RequiredUIAccess
    public static void open(@Nonnull AnActionEvent event, @Nullable WebBrowser browser) {
        open(createRequest(event.getDataContext()), (event.getModifiers() & Event.SHIFT_MASK) != 0, browser);
    }

    @RequiredUIAccess
    public static void open(@Nullable final OpenInBrowserRequest request, boolean preferLocalUrl, @Nullable final WebBrowser browser) {
        if (request == null) {
            return;
        }

        try {
            Collection<Url> urls = WebBrowserService.getInstance().getUrlsToOpen(request, preferLocalUrl);
            if (!urls.isEmpty()) {
                chooseUrl(urls).doWhenDone(url -> {
                    //noinspection RequiredXAction
                    Application.get().saveAll();
                    BrowserLauncher.getInstance().browse(url.toExternalForm(), browser, request.getProject());
                });
            }
        }
        catch (WebBrowserUrlProvider.BrowserException e1) {
            Messages.showErrorDialog(e1.getMessage(), WebBrowserLocalize.browserError().get());
        }
        catch (Exception e1) {
            LOG.error(e1);
        }
    }

    @Nonnull
    private static AsyncResult<Url> chooseUrl(@Nonnull Collection<Url> urls) {
        if (urls.size() == 1) {
            return AsyncResult.resolved(ContainerUtil.getFirstItem(urls));
        }
        final AsyncResult<Url> result = AsyncResult.undefined();
        JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<>(urls))
            .setTitle("Choose Url")
            .setRenderer(new ColoredListCellRenderer() {
                @Override
                protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
                    setIcon(PlatformIconGroup.nodesPpweb());
                    append(((Url)value).toDecodedForm());
                }
            })
            .setItemSelectedCallback(value -> {
                if (value != null) {
                    result.setDone(value);
                }
                else {
                    result.setRejected();
                }
            })
            .createPopup()
            .showInFocusCenter();
        return result;
    }
}