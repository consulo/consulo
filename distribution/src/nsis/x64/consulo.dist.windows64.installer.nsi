!define /ifndef NSIS_MAKENSIS64
!include "MUI2.nsh"

!define MUI_ICON "consulo.ico"

Name "Consulo (x64)"

Icon "consulo.ico"

Unicode True

ManifestDPIAware True

OutFile "consulo.dist.windows64.installer.exe"

InstallDir $APPDATA\Consulo

InstallDirRegKey HKLM "Software\Consulo" "Install_Dir"

RequestExecutionLevel user

;--------------------------------

!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN "$INSTDIR\consulo64.exe"

!insertmacro MUI_PAGE_LICENSE "LICENSE.txt"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_LANGUAGE "English"

;--------------------------------

Section "Consulo (required)"

  SectionIn RO
  
  SetOutPath $INSTDIR
  
  File /r Consulo\**
  
  WriteRegStr HKLM SOFTWARE\Consulo "Install_Dir" "$INSTDIR"
  
  ; Write the uninstall keys for Windows
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Consulo" "DisplayName" "Consulo"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Consulo" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Consulo" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Consulo" "NoRepair" 1
  WriteUninstaller "$INSTDIR\uninstall.exe"
  
SectionEnd

Section "Bundled JRE"
  SectionIn RO

SectionEnd

Section "Start Menu Shortcuts"
  SectionIn 2
	
  CreateDirectory "$SMPROGRAMS\Consulo"
  CreateShortcut "$SMPROGRAMS\Consulo\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
  CreateShortcut "$SMPROGRAMS\Consulo\Consulo (x64).lnk" "$INSTDIR\consulo64.exe" "" "$INSTDIR\consulo64.exe" 0
  
SectionEnd


Section "Desktop Shortcuts"
  SectionIn 3
  
  CreateShortcut "$DESKTOP\Consulo (x64).lnk" "$INSTDIR\consulo64.exe" "" "$INSTDIR\consulo64.exe" 0
  
SectionEnd


;--------------------------------

; Uninstaller

Section "Uninstall"
  
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Consulo"
  DeleteRegKey HKLM SOFTWARE\Consulo

  RMDir /r $INSTDIR\platform
  Delete $INSTDIR\consulo**
  Delete $INSTDIR\uninstall.exe


  Delete "$SMPROGRAMS\Consulo\*.*"
  Delete "$DESKTOP\Consulo (x64).lnk"


  RMDir /r "$APPDATA\Consulo Platform"
  
  RMDir "$SMPROGRAMS\Consulo"
  RMDir "$INSTDIR"

SectionEnd