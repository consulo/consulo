cmake_minimum_required(VERSION 3.5)
project(WinRunner)
include(CheckSymbolExists)

if(NOT WIN32)
  message(FATAL_ERROR "Windows only.")
endif()

if (DEFINED ENV{BUILD_NUMBER})
  set(BUILD_NUMBER $ENV{BUILD_NUMBER})
else()
  set(BUILD_NUMBER 9999)
endif()

string(TIMESTAMP YEAR "%Y")

check_symbol_exists("_M_AMD64" "" TARGET_X64)
check_symbol_exists("_M_ARM64" "" TARGET_ARM64)

if(TARGET_X64)
  set(FILE_NAME "runnerw64")
elseif(TARGET_ARM64)
  set(FILE_NAME "runnerw-aarch64")
else()
  set(FILE_NAME "runnerw")
endif()

set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} /MT")

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/runnerw.rc ${CMAKE_CURRENT_BINARY_DIR}/runnerw.rc @ONLY)

add_executable(${FILE_NAME} resource.h runnerw.cpp ${CMAKE_CURRENT_BINARY_DIR}/runnerw.rc)
