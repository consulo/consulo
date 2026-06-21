/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataRule;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.PsiElement;
import consulo.module.Module;
import consulo.navigation.Navigatable;
import consulo.usage.UsageView;
import consulo.virtualFileSystem.VirtualFile;

@ExtensionImpl
public class BasicLangUiDataRule implements UiDataRule {
    @Override
    public void uiDataSnapshot(DataSink sink, DataSnapshot snapshot) {
        sink.lazyValue(PsiElement.KEY, PsiElementFromSelectionRule::getData);
        sink.lazyValue(PsiElement.KEY_OF_ARRAY, PsiElementFromSelectionsRule::getData);
        sink.lazyValue(LangDataKeys.PASTE_TARGET_PSI_ELEMENT, PasteTargetRule::getData);
        sink.lazyValue(VirtualFile.KEY, VirtualFileRule::getData);
        sink.lazyValue(VirtualFile.KEY_OF_ARRAY, VirtualFileArrayRule::getData);
        sink.lazyValue(Navigatable.KEY, NavigatableRule::getData);
        sink.lazyValue(Navigatable.KEY_OF_ARRAY, NavigatableArrayRule::getData);
        sink.lazyValue(UsageView.USAGE_TARGETS_KEY, UsageTargetsRule::getData);
        sink.lazyValue(UsageView.USAGE_INFO_LIST_KEY, UsageInfo2ListRule::getData);
        sink.lazyValue(Module.KEY, ModuleRule::getData);
    }
}
