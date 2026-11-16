@echo off
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b %errorlevel%
if not exist build\guest193-core mkdir build\guest193-core
set "CORE=app\src\main\cpp\application"
cl.exe /nologo /std:c++17 /O2 /EHsc /Iapp\src\main\cpp\application /Iapp\src\main\cpp\aclib /DLODEPNG_COMPILE_DISK /DLODEPNG_COMPILE_ENCODER tools\export_classic193.cpp app\src\main\cpp\application\visual_texture.cpp app\src\main\cpp\aclib\lodepng.cpp /Febuild\guest193-core\export.exe /Fobuild\guest193-core\
