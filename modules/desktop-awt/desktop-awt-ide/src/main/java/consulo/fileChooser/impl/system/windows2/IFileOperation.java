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
import com.sun.jna.platform.win32.Guid.CLSID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.WinDef.BOOLByReference;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.IUnknown;
import com.sun.jna.ptr.IntByReference;

public interface IFileOperation extends IUnknown {

    IID IID_IFILEOPERATION = new IID("{947aab5f-0a5c-4c13-b4d6-4bf7836fc9f8}");
    CLSID CLSID_FILEOPERATION = new CLSID("{3ad05575-8857-4850-9277-11b85bdb8e09}");

    HRESULT Advise(Pointer pfops, IntByReference pdwCookie); // IFileOperationProgressSink

    HRESULT Unadvise(int dwCookie);

    HRESULT SetOperationFlags(int dwOperationFlags);

    HRESULT SetProgressMessage(WString pszMessage);

    HRESULT SetProgressDialog(Pointer popd); // IOperationsProgressDialog

    HRESULT SetProperties(Pointer pproparray); // IPropertyChangeArray

    HRESULT SetOwnerWindow(HWND hwndOwner);

    HRESULT ApplyPropertiesToItem(Pointer psiItem); // IShellItem

    HRESULT ApplyPropertiesToItems(Pointer punkItems); // IUnknown

    HRESULT RenameItem(Pointer psiItem, WString pszNewName, Pointer pfopsItem); // IShellItem,
                                                                                // IFileOperationProgressSink

    HRESULT RenameItems(Pointer pUnkItems, WString pszNewName); // IUnknown

    HRESULT MoveItem(Pointer psiItem, Pointer psiDestinationFolder, WString pszNewName, Pointer pfopsItem); // IShellItem,
                                                                                                            // IShellItem,
                                                                                                            // IFileOperationProgressSink

    HRESULT MoveItems(Pointer punkItems, Pointer psiDestinationFolder); // IUnknown, IShellItem

    HRESULT CopyItem(Pointer psiItem, Pointer psiDestinationFolder, WString pszCopyName, Pointer pfopsItem); // IShellItem,
                                                                                                             // IShellItem,
                                                                                                             // IFileOperationProgressSink

    HRESULT CopyItems(Pointer punkItems, Pointer psiDestinationFolder); // IUnknown, IShellItem

    HRESULT DeleteItem(Pointer psiItem, Pointer pfopsItem); // IShellItem, IFileOperationProgressSink

    HRESULT DeleteItems(Pointer punkItems); // IUnknown

    HRESULT NewItem(Pointer psiDestinationFolder, int dwFileAttributes, WString pszName, WString pszTemplateName,
            Pointer pfopsItem); // IShellItem, IFileOperationProgressSink

    HRESULT PerformOperations();

    HRESULT GetAnyOperationsAborted(BOOLByReference pfAnyOperationsAborted);
}
