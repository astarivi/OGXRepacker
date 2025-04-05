!include "MUI2.nsh"

# Installer info
Name "OGXRepacker"
OutFile "OGXRepacker_x64_Setup.exe"
InstallDir "$PROGRAMFILES\OGXRepacker"
InstallDirRegKey HKCU "Software\OGXRepacker" "Install_Dir"
RequestExecutionLevel admin

# Interface Pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "NOTICE"
!insertmacro MUI_PAGE_DIRECTORY
Page custom ShortcutsPage ShortcutsPageLeave
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Language
!insertmacro MUI_LANGUAGE "English"

# Variables to track checkbox states
Var SHORTCUT_DESKTOP
Var SHORTCUT_MENU

Function ShortcutsPage
  nsDialogs::Create 1018
  Pop $0

  ${NSD_CreateLabel} 0 0 100% 12u "Choose which shortcuts to create:"
  Pop $1

  ${NSD_CreateCheckbox} 0 20u 100% 12u "&Create Desktop Shortcut"
  Pop $SHORTCUT_DESKTOP
  ${NSD_Check} $SHORTCUT_DESKTOP

  ${NSD_CreateCheckbox} 0 35u 100% 12u "&Create Start Menu Shortcut"
  Pop $SHORTCUT_MENU
  ${NSD_Check} $SHORTCUT_MENU

  nsDialogs::Show
FunctionEnd

Function ShortcutsPageLeave
  ${NSD_GetState} $SHORTCUT_DESKTOP $SHORTCUT_DESKTOP
  ${NSD_GetState} $SHORTCUT_MENU $SHORTCUT_MENU
FunctionEnd

# Install section
Section "Install"
  SetOutPath "$INSTDIR"
  WriteRegStr HKCU "Software\OGXRepacker" "Install_Dir" "$INSTDIR"
  File /r "build\launch4j\*"

  # Start Menu Shortcut
  ${If} $SHORTCUT_MENU == ${BST_CHECKED}
    CreateDirectory "$SMPROGRAMS\OGXRepacker"
    CreateShortCut "$SMPROGRAMS\OGXRepacker\OGXRepacker.lnk" "$INSTDIR\OGXRepacker.exe"
  ${EndIf}

  # Desktop Shortcut
  ${If} $SHORTCUT_DESKTOP == ${BST_CHECKED}
    CreateShortCut "$DESKTOP\OGXRepacker.lnk" "$INSTDIR\OGXRepacker.exe"
  ${EndIf}

  # Uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OGXRepacker" "DisplayName" "OGXRepacker"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OGXRepacker" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OGXRepacker" "DisplayIcon" "$INSTDIR\OGXRepacker.exe"
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OGXRepacker" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OGXRepacker" "NoRepair" 1
SectionEnd

# Uninstall section
Section "Uninstall"
  Delete "$INSTDIR\OGXRepacker.exe"
  Delete "$INSTDIR\Uninstall.exe"
  RMDir /r "$INSTDIR"

  Delete "$DESKTOP\OGXRepacker.lnk"
  Delete "$SMPROGRAMS\OGXRepacker\OGXRepacker.lnk"
  RMDir "$SMPROGRAMS\OGXRepacker"

  DeleteRegKey HKCU "Software\OGXRepacker"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OGXRepacker"
SectionEnd
