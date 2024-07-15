/*
 * Copyright 2013-2022 consulo.io
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
package consulo.fileEditor.impl.internal;

import consulo.fileEditor.*;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 15-Mar-22
 */
public class FileEditorHistoryUtil {
  private static final Logger LOG = Logger.getInstance(FileEditorHistoryUtil.class);

  @Nonnull
  public static HistoryEntry currentStateAsHistoryEntry(@Nonnull FileEditorWithProviderComposite composite) {
    final FileEditor[] editors = composite.getEditors();
    final FileEditorState[] states = new FileEditorState[editors.length];
    for (int j = 0; j < states.length; j++) {
      states[j] = editors[j].getState(FileEditorStateLevel.FULL);
      LOG.assertTrue(states[j] != null);
    }
    final int selectedProviderIndex = ArrayUtil.find(editors, composite.getSelectedEditor());
    LOG.assertTrue(selectedProviderIndex != -1);
    final FileEditorProvider[] providers = composite.getProviders();
    return HistoryEntry.createLight(composite.getFile(), providers, states, providers[selectedProviderIndex]);
  }
}
