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

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid.GUID;

public interface ShTypes {

    @FieldOrder({ "pszName", "pszSpec" })
    class COMDLG_FILTERSPEC extends Structure {
        public WString pszName;
        public WString pszSpec;
    }

    @FieldOrder({ "fmdid", "pid" })
    class PROPERTYKEY extends Structure {
        public GUID fmtid;
        public int pid;
    }

    interface FILEOPENDIALOGOPTIONS {
        int FOS_OVERWRITEPROMPT = 0x2;
        int FOS_STRICTFILETYPES = 0x4;
        int FOS_NOCHANGEDIR = 0x8;
        int FOS_PICKFOLDERS = 0x20;
        int FOS_FORCEFILESYSTEM = 0x40;
        int FOS_ALLNONSTORAGEITEMS = 0x80;
        int FOS_NOVALIDATE = 0x100;
        int FOS_ALLOWMULTISELECT = 0x200;
        int FOS_PATHMUSTEXIST = 0x800;
        int FOS_FILEMUSTEXIST = 0x1000;
        int FOS_CREATEPROMPT = 0x2000;
        int FOS_SHAREAWARE = 0x4000;
        int FOS_NOREADONLYRETURN = 0x8000;
        int FOS_NOTESTFILECREATE = 0x10000;
        int FOS_HIDEMRUPLACES = 0x20000;
        int FOS_HIDEPINNEDPLACES = 0x40000;
        int FOS_NODEREFERENCELINKS = 0x100000;
        int FOS_OKBUTTONNEEDSINTERACTION = 0x200000;
        int FOS_DONTADDTORECENT = 0x2000000;
        int FOS_FORCESHOWHIDDEN = 0x10000000;
        int FOS_DEFAULTNOMINIMODE = 0x20000000;
        int FOS_FORCEPREVIEWPANEON = 0x40000000;
        int FOS_SUPPORTSTREAMABLEITEMS = 0x80000000;
    }

    interface SIATTRIBFLAGS {
        int SIATTRIBFLAGS_AND = 0x1;
        int SIATTRIBFLAGS_OR = 0x2;
        int SIATTRIBFLAGS_APPCOMPAT = 0x3;
        int SIATTRIBFLAGS_MASK = 0x3;
        int SIATTRIBFLAGS_ALLITEMS = 0x4000;
    }

    interface SFGAOF {
        int SFGAO_CANCOPY = 0x1; // Objects can be copied (DROPEFFECT_COPY)
        int SFGAO_CANMOVE = 0x2; // Objects can be moved (DROPEFFECT_MOVE)
        int SFGAO_CANLINK = 0x4; // Objects can be linked (DROPEFFECT_LINK)
        int SFGAO_STORAGE = 0x00000008; // supports BindToObject(IID_IStorage)
        int SFGAO_CANRENAME = 0x00000010; // Objects can be renamed
        int SFGAO_CANDELETE = 0x00000020; // Objects can be deleted
        int SFGAO_HASPROPSHEET = 0x00000040; // Objects have property sheets
        int SFGAO_DROPTARGET = 0x00000100; // Objects are drop target
        int SFGAO_CAPABILITYMASK = 0x00000177;
        int SFGAO_ENCRYPTED = 0x00002000; // object is encrypted (use alt color)
        int SFGAO_ISSLOW = 0x00004000; // 'slow' object
        int SFGAO_GHOSTED = 0x00008000; // ghosted icon
        int SFGAO_LINK = 0x00010000; // Shortcut (link)
        int SFGAO_SHARE = 0x00020000; // shared
        int SFGAO_READONLY = 0x00040000; // read-only
        int SFGAO_HIDDEN = 0x00080000; // hidden object
        int SFGAO_DISPLAYATTRMASK = 0x000FC000;
        int SFGAO_FILESYSANCESTOR = 0x10000000; // may contain children with int SFGAO_FILESYSTEM
        int SFGAO_FOLDER = 0x20000000; // support BindToObject(IID_IShellFolder)
        int SFGAO_FILESYSTEM = 0x40000000; // is a win32 file system object (file/folder/root)
        int SFGAO_HASSUBFOLDER = 0x80000000; // may contain children with int SFGAO_FOLDER
        int SFGAO_CONTENTSMASK = 0x80000000;
        int SFGAO_VALIDATE = 0x01000000; // invalidate cached information
        int SFGAO_REMOVABLE = 0x02000000; // is this removeable media?
        int SFGAO_COMPRESSED = 0x04000000; // Object is compressed (use alt color)
        int SFGAO_BROWSABLE = 0x08000000; // supports IShellFolder, but only implements CreateViewObject() (non-folder
                                          // view)
        int SFGAO_NONENUMERATED = 0x00100000; // is a non-enumerated object
        int SFGAO_NEWCONTENT = 0x00200000; // should show bold in explorer tree
        int SFGAO_CANMONIKER = 0x00400000; // defunct
        int SFGAO_HASSTORAGE = 0x00400000; // defunct
        int SFGAO_STREAM = 0x00400000; // supports BindToObject(IID_IStream)
        int SFGAO_STORAGEANCESTOR = 0x00800000; // may contain children with int SFGAO_STORAGE or int SFGAO_STREAM
        int SFGAO_STORAGECAPMASK = 0x70C50008; // for determining storage capabilities, ie for open/save semantics
    }

    interface GETPROPERTYSTOREFLAGS {
        // If no flags are specified (int GPS_DEFAULT), a read-only property store is
        // returned that includes properties for the file or item.
        // In the case that the shell item is a file, the property store contains:
        // 1. properties about the file from the file system
        // 2. properties from the file itself provided by the file's property handler,
        // unless that file is offline, see int GPS_OPENSLOWITEM
        // 3. if requested by the file's property handler and supported by the file
        // system, properties stored in the alternate property store.
        //
        // Non-file shell items should return a similar read-only store
        //
        // Specifying other int GPS_ flags modifies the store that is returned
        int GPS_DEFAULT = 0x00000000;
        int GPS_HANDLERPROPERTIESONLY = 0x00000001; // only include properties directly from the file's property handler
        int GPS_READWRITE = 0x00000002; // Writable stores will only include handler properties
        int GPS_TEMPORARY = 0x00000004; // A read/write store that only holds properties for the lifetime of the
                                        // IShellItem object
        int GPS_FASTPROPERTIESONLY = 0x00000008; // do not include any properties from the file's property handler
                                                 // (because the file's property handler will hit the disk)
        int GPS_OPENSLOWITEM = 0x00000010; // include properties from a file's property handler, even if it means
                                           // retrieving the file from offline storage.
        int GPS_DELAYCREATION = 0x00000020; // delay the creation of the file's property handler until those properties
                                            // are read, written, or enumerated
        int GPS_BESTEFFORT = 0x00000040; // For readonly stores, succeed and return all available properties, even if
                                         // one or more sources of properties fails. Not valid with int GPS_READWRITE.
        int GPS_NO_OPLOCK = 0x00000080; // some data sources protect the read property store with an oplock, this
                                        // disables that
        int GPS_MASK_VALID = 0x000000FF;
    }

    interface SIGDN {
        int SIGDN_NORMALDISPLAY = 0;
        int SIGDN_PARENTRELATIVEPARSING = 0x80018001;
        int SIGDN_DESKTOPABSOLUTEPARSING = 0x80028000;
        int SIGDN_PARENTRELATIVEEDITING = 0x80031001;
        int SIGDN_DESKTOPABSOLUTEEDITING = 0x8004c000;
        int SIGDN_FILESYSPATH = 0x80058000;
        int SIGDN_URL = 0x80068000;
        int SIGDN_PARENTRELATIVEFORADDRESSBAR = 0x8007c001;
        int SIGDN_PARENTRELATIVE = 0x80080001;
        int SIGDN_PARENTRELATIVEFORUI = 0x80094001;
    }

    interface SICHINTF {
        int SICHINT_DISPLAY = 0;
        int SICHINT_ALLFIELDS = 0x80000000;
        int SICHINT_CANONICAL = 0x10000000;
        int SICHINT_TEST_FILESYSPATH_IF_NOT_EQUAL = 0x20000000;
    };
}
