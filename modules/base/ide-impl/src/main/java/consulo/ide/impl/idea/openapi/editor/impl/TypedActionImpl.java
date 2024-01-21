// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.editor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.codeEditor.action.TypedAction;
import consulo.undoRedo.CommandProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ServiceImpl
public class TypedActionImpl extends TypedAction {
  private final DefaultRawTypedHandler myDefaultRawTypedHandler;

  @Inject
  public TypedActionImpl(Application application, CommandProcessor commandProcessor) {
    super(application);
    myDefaultRawTypedHandler = new DefaultRawTypedHandler(this, commandProcessor);
    setupRawHandler(myDefaultRawTypedHandler);
  }

  public DefaultRawTypedHandler getDefaultRawTypedHandler() {
    return myDefaultRawTypedHandler;
  }
}
