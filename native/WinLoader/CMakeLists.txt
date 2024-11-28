cmake_minimum_required(VERSION 3.4)
project(WinLoaderLibrary)
include(CheckSymbolExists)

if(NOT WIN32)
  message(FATAL_ERROR "Windows only.")
endif()

check_symbol_exists("_M_AMD64" "" TARGET_X64)
check_symbol_exists("_M_ARM64" "" TARGET_ARM64)

if(TARGET_X64)
  set(FILE_NAME "consulo64")
elseif(TARGET_ARM64)
  set(FILE_NAME "consulo-aarch64")
else()
  set(FILE_NAME "consulo")
endif()

add_definitions (-DUNICODE -D_UNICODE)

set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /MT")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} /MTd")

add_executable(${FILE_NAME} WIN32 resource.h stdafx.cpp stdafx.h targetver.h WinLauncher.cpp WinLauncher.h WinLauncher.rc)

set_property(TARGET ${FILE_NAME} PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

ADD_CUSTOM_COMMAND(
    TARGET ${TARGET_TARGETNAME}
    POST_BUILD
    COMMAND "mt.exe" -manifest \"${CMAKE_CURRENT_SOURCE_DIR}\\ConsuloManifest.xml\" -inputresource:\"$<TARGET_FILE:${TARGET_TARGETNAME}>\"\;\#1 -outputresource:\"$<TARGET_FILE:${TARGET_TARGETNAME}>\"\;\#1
)