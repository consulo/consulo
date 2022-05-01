// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInspection.reference;

import consulo.language.editor.inspection.reference.RefElement;

public interface WritableRefElement extends RefElement, WritableRefEntity {
  void addInReference(RefElement refElement);

  void addOutReference(RefElement refElement);

  void addSuppression(String text);
}
