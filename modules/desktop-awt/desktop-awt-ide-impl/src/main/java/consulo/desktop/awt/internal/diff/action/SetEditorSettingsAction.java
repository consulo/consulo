/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff.action;

import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.util.SoftWrapUtil;
import consulo.desktop.awt.internal.diff.util.SyncScrollSupport;
import consulo.diff.impl.internal.TextDiffSettingsHolder;
import consulo.diff.impl.internal.util.HighlightingLevel;
import consulo.diff.localize.DiffLocalize;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SetEditorSettingsAction extends ActionGroup implements DumbAware {
    @Nonnull
    private final TextDiffSettingsHolder.TextDiffSettings myTextSettings;
    @Nonnull
    private final List<? extends Editor> myEditors;
    @Nullable
    private SyncScrollSupport.Support mySyncScrollSupport;

    @Nonnull
    private final AnAction[] myActions;

    public SetEditorSettingsAction(
        @Nonnull TextDiffSettingsHolder.TextDiffSettings settings,
        @Nonnull List<? extends Editor> editors
    ) {
        super(DiffLocalize.editorSettings(), LocalizeValue.empty(), AllIcons.General.GearPlain);
        setPopup(true);
        myTextSettings = settings;
        myEditors = editors;

        for (Editor editor : myEditors) {
            ((EditorGutterComponentEx)editor.getGutter()).setGutterPopupGroup(this);
        }

        myActions = new AnAction[]{
            new EditorSettingToggleAction("EditorToggleShowWhitespaces") {
                @Override
                public boolean isSelected() {
                    return myTextSettings.isShowWhitespaces();
                }

                @Override
                public void setSelected(boolean state) {
                    myTextSettings.setShowWhiteSpaces(state);
                }

                @Override
                public void apply(@Nonnull Editor editor, boolean value) {
                    if (editor.getSettings().isWhitespacesShown() != value) {
                        editor.getSettings().setWhitespacesShown(value);
                        editor.getComponent().repaint();
                    }
                }
            },
            new EditorSettingToggleAction("EditorToggleShowLineNumbers") {
                @Override
                public boolean isSelected() {
                    return myTextSettings.isShowLineNumbers();
                }

                @Override
                public void setSelected(boolean state) {
                    myTextSettings.setShowLineNumbers(state);
                }

                @Override
                public void apply(@Nonnull Editor editor, boolean value) {
                    if (editor.getSettings().isLineNumbersShown() != value) {
                        editor.getSettings().setLineNumbersShown(value);
                        editor.getComponent().repaint();
                    }
                }
            },
            new EditorSettingToggleAction("EditorToggleShowIndentLines") {
                @Override
                public boolean isSelected() {
                    return myTextSettings.isShowIndentLines();
                }

                @Override
                public void setSelected(boolean state) {
                    myTextSettings.setShowIndentLines(state);
                }

                @Override
                public void apply(@Nonnull Editor editor, boolean value) {
                    if (editor.getSettings().isIndentGuidesShown() != value) {
                        editor.getSettings().setIndentGuidesShown(value);
                        editor.getComponent().repaint();
                    }
                }
            },
            new EditorSettingToggleAction("EditorToggleUseSoftWraps") {
                private boolean myForcedSoftWrap;

                @Override
                public boolean isSelected() {
                    return myForcedSoftWrap || myTextSettings.isUseSoftWraps();
                }

                @Override
                public void setSelected(boolean state) {
                    myForcedSoftWrap = false;
                    myTextSettings.setUseSoftWraps(state);
                }

                @Override
                public void apply(@Nonnull Editor editor, boolean value) {
                    if (editor.getSettings().isUseSoftWraps() == value) {
                        return;
                    }

                    if (mySyncScrollSupport != null) {
                        mySyncScrollSupport.enterDisableScrollSection();
                    }
                    try {
                        SoftWrapUtil.toggleSoftWraps(editor, null, value);
                    }
                    finally {
                        if (mySyncScrollSupport != null) {
                            mySyncScrollSupport.exitDisableScrollSection();
                        }
                    }
                }

                @Override
                public void applyDefaults(@Nonnull List<? extends Editor> editors) {
                    if (!myTextSettings.isUseSoftWraps()) {
                        for (Editor editor : editors) {
                            myForcedSoftWrap = myForcedSoftWrap || ((RealEditor)editor).shouldSoftWrapsBeForced();
                        }
                    }
                    super.applyDefaults(editors);
                }
            },
            new EditorHighlightingLayerAction(),
        };
    }

    public void setSyncScrollSupport(@Nullable SyncScrollSupport.Support syncScrollSupport) {
        mySyncScrollSupport = syncScrollSupport;
    }

    public void applyDefaults() {
        for (AnAction action : myActions) {
            ((EditorSettingAction)action).applyDefaults(myEditors);
        }
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> result = new ArrayList<>();
        ContainerUtil.addAll(result, myActions);
        result.add(AnSeparator.getInstance());
        result.add(ActionManager.getInstance().getAction(IdeActions.GROUP_DIFF_EDITOR_GUTTER_POPUP));
        return ContainerUtil.toArray(result, new AnAction[result.size()]);
    }

    private abstract class EditorSettingToggleAction extends ToggleAction implements DumbAware, EditorSettingAction {
        private EditorSettingToggleAction(@Nonnull String actionId) {
            ActionUtil.copyFrom(this, actionId);
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return isSelected();
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            setSelected(state);
            for (Editor editor : myEditors) {
                apply(editor, state);
            }
        }

        public abstract boolean isSelected();

        public abstract void setSelected(boolean value);

        public abstract void apply(@Nonnull Editor editor, boolean value);

        @Override
        public void applyDefaults(@Nonnull List<? extends Editor> editors) {
            for (Editor editor : editors) {
                apply(editor, isSelected());
            }
        }
    }

    private class EditorHighlightingLayerAction extends ActionGroup implements EditorSettingAction {
        private final AnAction[] myOptions;

        public EditorHighlightingLayerAction() {
            super(DiffLocalize.highlightingLevel(), true);
            myOptions = ContainerUtil.map(HighlightingLevel.values(), OptionAction::new, AnAction.EMPTY_ARRAY);
        }

        @Nonnull
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return myOptions;
        }

        @Override
        public void applyDefaults(@Nonnull List<? extends Editor> editors) {
            apply(myTextSettings.getHighlightingLevel());
        }

        private void apply(@Nonnull HighlightingLevel layer) {
            for (Editor editor : myEditors) {
                ((RealEditor)editor).setHighlightingFilter(layer.getCondition());
            }
        }

        private class OptionAction extends ToggleAction implements DumbAware {
            @Nonnull
            private final HighlightingLevel myLayer;

            public OptionAction(@Nonnull HighlightingLevel layer) {
                super(layer.getText(), LocalizeValue.empty(), layer.getIcon());
                myLayer = layer;
            }

            @Override
            public boolean isSelected(@Nonnull AnActionEvent e) {
                return myTextSettings.getHighlightingLevel() == myLayer;
            }

            @Override
            public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                myTextSettings.setHighlightingLevel(myLayer);
                apply(myLayer);
            }
        }
    }

    private interface EditorSettingAction {
        void applyDefaults(@Nonnull List<? extends Editor> editors);
    }
}
