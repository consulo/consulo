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
import com.sun.jna.platform.win32.WinDef.BOOLByReference;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.ptr.IntByReference;

public class FileOperation extends Unknown implements IFileOperation {

    public FileOperation() {
    }

    public FileOperation(Pointer pvInstance) {
        super(pvInstance);
    }

    // VTBL Id indexing starts at 3 after Unknown's 2

    public HRESULT Advise(Pointer pfops, IntByReference pdwCookie) {
        return (HRESULT) this._invokeNativeObject(3, new Object[] { this.getPointer(), pfops, pdwCookie },
                HRESULT.class);
    }

    public HRESULT Unadvise(int dwCookie) {
        return (HRESULT) this._invokeNativeObject(4, new Object[] { this.getPointer(), dwCookie }, HRESULT.class);
    }

    public HRESULT SetOperationFlags(int dwOperationFlags) {
        return (HRESULT) this._invokeNativeObject(5, new Object[] { this.getPointer(), dwOperationFlags },
                HRESULT.class);
    }

    public HRESULT SetProgressMessage(WString pszMessage) {
        return (HRESULT) this._invokeNativeObject(6, new Object[] { this.getPointer(), pszMessage }, HRESULT.class);
    }

    public HRESULT SetProgressDialog(Pointer popd) {
        return (HRESULT) this._invokeNativeObject(7, new Object[] { this.getPointer(), popd }, HRESULT.class);
    }

    public HRESULT SetProperties(Pointer pproparray) {
        return (HRESULT) this._invokeNativeObject(8, new Object[] { this.getPointer(), pproparray }, HRESULT.class);
    }

    public HRESULT SetOwnerWindow(HWND hwndOwner) {
        return (HRESULT) this._invokeNativeObject(9, new Object[] { this.getPointer(), hwndOwner }, HRESULT.class);
    }

    public HRESULT ApplyPropertiesToItem(Pointer psiItem) {
        return (HRESULT) this._invokeNativeObject(10, new Object[] { this.getPointer(), psiItem }, HRESULT.class);
    }

    public HRESULT ApplyPropertiesToItems(Pointer punkItems) {
        return (HRESULT) this._invokeNativeObject(11, new Object[] { this.getPointer(), punkItems }, HRESULT.class);
    }

    public HRESULT RenameItem(Pointer psiItem, WString pszNewName, Pointer pfopsItem) {
        return (HRESULT) this._invokeNativeObject(12,
                new Object[] { this.getPointer(), psiItem, pszNewName, pfopsItem }, HRESULT.class);
    }

    public HRESULT RenameItems(Pointer pUnkItems, WString pszNewName) {
        return (HRESULT) this._invokeNativeObject(13, new Object[] { this.getPointer(), pUnkItems, pszNewName },
                HRESULT.class);
    }

    public HRESULT MoveItem(Pointer psiItem, Pointer psiDestinationFolder, WString pszNewName, Pointer pfopsItem) {
        return (HRESULT) this._invokeNativeObject(14,
                new Object[] { this.getPointer(), psiItem, psiDestinationFolder, pszNewName, pfopsItem },
                HRESULT.class);
    }

    public HRESULT MoveItems(Pointer punkItems, Pointer psiDestinationFolder) {
        return (HRESULT) this._invokeNativeObject(15,
                new Object[] { this.getPointer(), punkItems, psiDestinationFolder }, HRESULT.class);
    }

    public HRESULT CopyItem(Pointer psiItem, Pointer psiDestinationFolder, WString pszCopyName, Pointer pfopsItem) {
        return (HRESULT) this._invokeNativeObject(16,
                new Object[] { this.getPointer(), psiItem, psiDestinationFolder, pszCopyName, pfopsItem },
                HRESULT.class);
    }

    public HRESULT CopyItems(Pointer punkItems, Pointer psiDestinationFolder) {
        return (HRESULT) this._invokeNativeObject(17,
                new Object[] { this.getPointer(), punkItems, psiDestinationFolder }, HRESULT.class);
    }

    public HRESULT DeleteItem(Pointer psiItem, Pointer pfopsItem) {
        return (HRESULT) this._invokeNativeObject(18, new Object[] { this.getPointer(), psiItem, pfopsItem },
                HRESULT.class);
    }

    public HRESULT DeleteItems(Pointer punkItems) {
        return (HRESULT) this._invokeNativeObject(19, new Object[] { this.getPointer(), punkItems }, HRESULT.class);
    }

    public HRESULT NewItem(Pointer psiDestinationFolder, int dwFileAttributes, WString pszName, WString pszTemplateName,
            Pointer pfopsItem) {
        return (HRESULT) this._invokeNativeObject(20, new Object[] { this.getPointer(), psiDestinationFolder,
                dwFileAttributes, pszName, pszTemplateName, pfopsItem }, HRESULT.class);
    }

    public HRESULT PerformOperations() {
        return (HRESULT) this._invokeNativeObject(21, new Object[] { this.getPointer() }, HRESULT.class);
    }

    public HRESULT GetAnyOperationsAborted(BOOLByReference pfAnyOperationsAborted) {
        return (HRESULT) this._invokeNativeObject(22, new Object[] { this.getPointer(), pfAnyOperationsAborted },
                HRESULT.class);
    }
}
