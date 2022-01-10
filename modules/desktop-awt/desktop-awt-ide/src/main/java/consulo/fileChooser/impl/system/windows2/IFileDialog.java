/* Copyright (c) 2020 Daniel Widdis, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package consulo.fileChooser.impl.system.windows2;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import consulo.fileChooser.impl.system.windows2.ShTypes.COMDLG_FILTERSPEC;

public interface IFileDialog extends IModalWindow {

    IID IID_IFILEDIALOG = new IID("{42f85136-db7e-439c-85f1-e4075d135fc8}");

    HRESULT SetFileTypes(int FileTypes, COMDLG_FILTERSPEC[] rgFilterSpec);

    HRESULT SetFileTypeIndex(int iFileType);

    HRESULT GetFileTypeIndex(IntByReference piFileType);

    HRESULT Advise(Pointer pfde, IntByReference pdwCookie); // IFileDialogEvents

    HRESULT Unadvise(int dwCookie);

    HRESULT SetOptions(int fos); // FILEOPENDIALOGOPTIONS

    HRESULT GetOptions(IntByReference pfos); // FILEOPENDIALOGOPTIONS

    HRESULT SetDefaultFolder(Pointer psi); // IShellItem

    HRESULT SetFolder(Pointer psi); // IShellItem

    HRESULT GetFolder(PointerByReference ppsi); // IShellItem

    HRESULT GetCurrentSelection(PointerByReference ppsi); // IShellItem

    HRESULT SetFileName(WString pszName);

    HRESULT GetFileName(PointerByReference pszName); // WString

    HRESULT SetTitle(WString pszTitle);

    HRESULT SetOkButtonLabel(WString pszText);

    HRESULT SetFileNameLabel(WString pszLabel);

    HRESULT GetResult(PointerByReference ppsi);

    HRESULT AddPlace(Pointer psi, int fdap); // IShellItem

    HRESULT SetDefaultExtension(WString pszDefaultExtension);

    HRESULT Close(HRESULT hr);

    HRESULT SetClientGuid(GUID.ByReference guid);

    HRESULT ClearClientData();

    HRESULT SetFilter(Pointer pFilter); // IShellItemFilter
}
