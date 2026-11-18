@echo off
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b %errorlevel%
if not exist build\rebuild208-core mkdir build\rebuild208-core
set "CORE=app\src\main\cpp\application"
set "ENTRY=tools\gravity207_core.cpp"
set "EXE=core"
if "%1"=="rules" set "ENTRY=tools\rebuild208_rules.cpp"
if "%1"=="rules" set "EXE=rules"
cl.exe /nologo /std:c++17 /O2 /EHsc /Itools\difficulty-check\stubs /I%CORE% /Iapp\src\main\cpp\aclib /DLODEPNG_COMPILE_DISK %ENTRY% %CORE%\expedition.cpp %CORE%\garden_scene.cpp %CORE%\operations.cpp %CORE%\state.cpp %CORE%\items.cpp %CORE%\move.cpp %CORE%\map.cpp %CORE%\ai.cpp %CORE%\ai_tactics.cpp %CORE%\local_tactics.cpp %CORE%\dot.cpp %CORE%\player.cpp %CORE%\random.cpp %CORE%\spiral.cpp %CORE%\info.cpp %CORE%\visual_texture.cpp app\src\main\cpp\aclib\lodepng.cpp /Febuild\rebuild208-core\%EXE%.exe /Fobuild\rebuild208-core\ > docs\game\rebuild-2.08\%EXE%-build.log 2>&1
exit /b %errorlevel%




