/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.codeInsight.lookup;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.language.editor.completion.lookup.LookupArranger;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;
import org.jspecify.annotations.Nullable;

/**
 * The lookup manager for the frontends without swing, opening a {@link UnifiedLookupUI}.
 * <p/>
 * Most of what is here is the same bookkeeping the swing manager does. It is not shared yet on purpose - the swing one
 * is only rebased onto {@link consulo.language.editor.impl.internal.completion.lookup.LookupBase} at the end of this
 * work, and the two are hoisted into one base then, once there is a second implementation to design it against.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedLookupManager extends LookupManager {
    private static final Logger LOG = Logger.getInstance(UnifiedLookupManager.class);

    private final Project myProject;
    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    private @Nullable UnifiedLookupUI myActiveLookup;
    private @Nullable Editor myActiveLookupEditor;

    @Inject
    public UnifiedLookupManager(Project project) {
        myProject = project;

        // an index which is not ready cannot answer what the items mean, and the editor going away takes the lookup
        // which was opened on it
        project.getMessageBus().connect().subscribe(DumbModeListener.class, new DumbModeListener() {
            @Override
            public void enteredDumbMode() {
                hideActiveLookup();
            }

            @Override
            public void exitDumbMode() {
                hideActiveLookup();
            }
        });

        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorReleased(EditorFactoryEvent event) {
                if (event.getEditor() == myActiveLookupEditor) {
                    hideActiveLookup();
                }
            }
        }, myProject);
    }

    @Override
    @RequiredUIAccess
    public @Nullable LookupEx showLookup(Editor editor, LookupElement[] items, String prefix, LookupArranger arranger) {
        for (LookupElement item : items) {
            assert item != null;
        }

        UnifiedLookupUI lookup = createLookup(editor, items, prefix, arranger);
        return lookup.showLookup() ? lookup : null;
    }

    @Override
    @RequiredUIAccess
    public UnifiedLookupUI createLookup(Editor editor, LookupElement[] items, String prefix, LookupArranger arranger) {
        hideActiveLookup();

        UIAccess.assertIsUIThread();

        UnifiedLookupUI lookup = new UnifiedLookupUI(myProject, editor, arranger);

        myActiveLookup = lookup;
        myActiveLookupEditor = editor;

        Disposer.register(lookup, () -> {
            myActiveLookup = null;
            myActiveLookupEditor = null;
            myPropertyChangeSupport.firePropertyChange(PROP_ACTIVE_LOOKUP, lookup, null);
        });

        if (items.length > 0) {
            CamelHumpMatcher matcher = new CamelHumpMatcher(prefix);
            for (LookupElement item : items) {
                lookup.addItem(item, matcher);
            }
            lookup.refreshUi(true, true);
        }

        myPropertyChangeSupport.firePropertyChange(PROP_ACTIVE_LOOKUP, null, lookup);
        return lookup;
    }

    @Override
    @RequiredUIAccess
    public void hideActiveLookup() {
        UnifiedLookupUI lookup = myActiveLookup;
        if (lookup != null) {
            lookup.checkValid();
            lookup.hide();
            LOG.assertTrue(lookup.isLookupDisposed(), "Should be disposed");
        }
    }

    @Override
    public @Nullable LookupEx getActiveLookup() {
        UnifiedLookupUI lookup = myActiveLookup;
        if (lookup != null && lookup.isLookupDisposed()) {
            myActiveLookup = null;
            lookup.checkValid();
        }

        return myActiveLookup;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        myPropertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener, Disposable disposable) {
        addPropertyChangeListener(listener);
        Disposer.register(disposable, () -> removePropertyChangeListener(listener));
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        myPropertyChangeSupport.removePropertyChangeListener(listener);
    }
}
