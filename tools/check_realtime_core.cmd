@echo off
rem Focused headless core check, no device and no renderer.
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b %errorlevel%
set "CORE=app\src\main\cpp\application"
cl.exe /nologo /std:c++17 /O2 /EHsc /Itools\difficulty-check\stubs /I%CORE% /Iapp\src\main\cpp\aclib /DLODEPNG_COMPILE_DISK tools\realtime-check\core.cpp %CORE%\state.cpp %CORE%\move.cpp %CORE%\map.cpp %CORE%\ai.cpp %CORE%\ai_tactics.cpp %CORE%\dot.cpp %CORE%\player.cpp %CORE%\random.cpp %CORE%\spiral.cpp %CORE%\info.cpp %CORE%\visual_texture.cpp app\src\main\cpp\aclib\lodepng.cpp /Febuild\realtime-core-check.exe /Fobuild\ > docs\game\model-control-1.58\core-build.log 2>&1
if errorlevel 1 exit /b %errorlevel%
build\realtime-core-check.exe app\src\main\assets > docs\game\model-control-1.58\core-check.log 2>&1
exit /b %errorlevel%
