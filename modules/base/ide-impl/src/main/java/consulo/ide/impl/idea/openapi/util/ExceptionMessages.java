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
package consulo.ide.impl.idea.openapi.util;

import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.localize.CommonLocalize;

import java.io.IOException;
import java.util.HashMap;

public class ExceptionMessages {
  static  final HashMap<Integer, LocalizeValue> ourIOMessages;
  static {
    ourIOMessages = new HashMap<>();
    if(Platform.current().os().isWindows()) {
      ourIOMessages.put(1, CommonLocalize.incorrectFunctionErrorMessage());
      ourIOMessages.put(2, CommonLocalize.theSystemCannotFindTheFileSpecifiedErrorMessage());
      ourIOMessages.put(3, CommonLocalize.theSystemCannotFindThePathSpecifiedErrorMessage());
      ourIOMessages.put(4, CommonLocalize.theSystemCannotOpenTheFileErrorMessage());
      ourIOMessages.put(5, CommonLocalize.accessIsDeniedErrorMessage());
      ourIOMessages.put(6, CommonLocalize.theDataIsInvalidErrorMessage());
      ourIOMessages.put(7, CommonLocalize.theStorageControlBlocksWereDestroyedErrorMessage());
      ourIOMessages.put(8, CommonLocalize.notEnoughStorageIsAvailableToProcessThisCommandErrorMessage());
      ourIOMessages.put(9, CommonLocalize.theStorageControlBlockAddressIsInvalidErrorMessage());
      ourIOMessages.put(10, CommonLocalize.theEnvironmentIsIncorrectErrorMessage());
      ourIOMessages.put(11, CommonLocalize.anAttemptWasMadeToLoadAProgramWithAnIncorrectFormatErrorMessage());
      ourIOMessages.put(12, CommonLocalize.theAccessCodeIsInvalidErrorMessage());
      ourIOMessages.put(13, CommonLocalize.theDataIsInvalidErrorMessage());
      ourIOMessages.put(14, CommonLocalize.notEnoughStorageIsAvailableToCompleteThisOperationErrorMessage());
      ourIOMessages.put(15, CommonLocalize.theSystemCannotFindTheDriveSpecifiedErrorMessage());
      ourIOMessages.put(16, CommonLocalize.theDirectoryCannotBeRemovedErrorMessage());
      ourIOMessages.put(17, CommonLocalize.theSystemCannotMoveTheFileToADifferentDiskDriveErrorMessage());
      ourIOMessages.put(18, CommonLocalize.thereAreNoMoreFilesErrorMessage());
      ourIOMessages.put(19, CommonLocalize.theMediaIsWriteProtectedErrorMessage());
      ourIOMessages.put(20, CommonLocalize.theSystemCannotFindTheDeviceSpecifiedErrorMessage());
      ourIOMessages.put(21, CommonLocalize.theDeviceIsNotReadyErrorMessage());
      ourIOMessages.put(22, CommonLocalize.theDeviceDoesNotRecognizeTheCommandErrorMessage());
      ourIOMessages.put(23, CommonLocalize.dataErrorCyclicRedundancyCheckErrorMessage());
      ourIOMessages.put(24, CommonLocalize.theProgramIssuedACommandButTheCommandLengthIsIncorrectErrorMessage());
      ourIOMessages.put(25, CommonLocalize.theDriveCannotLocateASpecificAreaOrTrackOnTheDiskErrorMessage());
      ourIOMessages.put(26, CommonLocalize.theSpecifiedDiskOrDisketteCannotBeAccessedErrorMessage());
      ourIOMessages.put(27, CommonLocalize.theDriveCannotFindTheSectorRequestedErrorMessage());
    }
  }

  public static String getMessage(IOException exception) {
    String exceptionMessage = exception.getMessage();
    String detailedMessage = null;
    int idx = exceptionMessage.indexOf('=');
    if(idx != -1) {
      int endIdx = idx + 1;
      for(; endIdx < exceptionMessage.length(); endIdx ++) {
        if(!Character.isDigit(exceptionMessage.charAt(endIdx))) break;
      }
      try {
        int errorNumber = Integer.parseInt(exceptionMessage.substring(idx + 1, endIdx));
        LocalizeValue localizedMessage = ourIOMessages.get(errorNumber);
        detailedMessage = localizedMessage != null ? localizedMessage.get() : null;
      }
      catch (NumberFormatException e) {
      }
    }
    StringBuffer buf = new StringBuffer();
    buf.append(exceptionMessage);
    if(detailedMessage != null) {
      buf.append("\n");
      buf.append(detailedMessage);
    }

    return buf.toString();
  }
}
