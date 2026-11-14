@echo off
rem Production sprite generator only, no alternate game renderer.
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b %errorlevel%
cl.exe /nologo /std:c++17 /O2 /EHsc /Iapp\src\main\cpp\application tools\check_porcelain_data.cpp app\src\main\cpp\application\visual_texture.cpp /Febuild\porcelain-check.exe /Fobuild\ > docs\game\particles-1.63\sprite-build.log 2>&1
if errorlevel 1 exit /b %errorlevel%
build\porcelain-check.exe build\particles-1.63\atlas.rgba > docs\game\particles-1.63\sprite-check.log 2>&1
exit /b %errorlevel%
