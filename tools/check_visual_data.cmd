@echo off
rem SPDX-License-Identifier: GPL-3.0-or-later
set "CHECK_OUT=%~1"
if "%CHECK_OUT%"=="" set "CHECK_OUT=docs\game\comfort-1.48"
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b %errorlevel%
cl.exe /nologo /std:c++17 /O2 /EHsc /Iapp\src\main\cpp\application /Iapp\src\main\cpp\aclib /DLODEPNG_COMPILE_DISK tools\check_visual_data.cpp app\src\main\cpp\application\visual_texture.cpp app\src\main\cpp\aclib\lodepng.cpp /Febuild\visual-data-check.exe /Fobuild\ > "%CHECK_OUT%\data-check-build.log" 2>&1
if errorlevel 1 exit /b %errorlevel%
build\visual-data-check.exe app\src\main\assets\maps > "%CHECK_OUT%\data-check.log" 2>&1
exit /b %errorlevel%
