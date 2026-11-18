@echo off
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b %errorlevel%
if not exist build\flow209-core mkdir build\flow209-core
set "CORE=app\src\main\cpp\application"
set "ENTRY=tools\flow209_core.cpp"
set "EXE=core"
cl.exe /nologo /std:c++17 /O2 /EHsc /Itools\difficulty-check\stubs /I%CORE% /Iapp\src\main\cpp\aclib /DLODEPNG_COMPILE_DISK /DFLOW209_PROFILE %ENTRY% %CORE%\expedition.cpp %CORE%\garden_scene.cpp %CORE%\operations.cpp %CORE%\state.cpp %CORE%\items.cpp %CORE%\move.cpp %CORE%\flow_motion.cpp %CORE%\map.cpp %CORE%\ai.cpp %CORE%\ai_tactics.cpp %CORE%\local_tactics.cpp %CORE%\dot.cpp %CORE%\player.cpp %CORE%\random.cpp %CORE%\spiral.cpp %CORE%\info.cpp %CORE%\visual_texture.cpp app\src\main\cpp\aclib\lodepng.cpp /Febuild\flow209-core\%EXE%.exe /Fobuild\flow209-core\ > docs\game\flow-2.09\%EXE%-build.log 2>&1
exit /b %errorlevel%




