/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.diagnostic;

import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;

import java.awt.event.InputEvent;

@SuppressWarnings({"HardCodedStringLiteral"})
public class DropAnErrorWithAttachmentsAction extends DumbAwareAction {
  public DropAnErrorWithAttachmentsAction() {
    super("Drop an error with attachments", "Hold down SHIFT for multiple attachments", null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final boolean multipleAttachments = (e.getModifiers() & InputEvent.SHIFT_MASK) != 0;
    Attachment[] attachments;
    if (multipleAttachments) {
      attachments = new Attachment[]{AttachmentFactory.get().create("first.txt", "first content"), AttachmentFactory.get().create("second.txt", "second content")};
    }
    else {
      attachments = new Attachment[]{AttachmentFactory.get().create("attachment.txt", "content")};
    }
    Logger.getInstance("test (with attachments)").error(LogMessageEx.createEvent("test", "test details", attachments));
  }
}
