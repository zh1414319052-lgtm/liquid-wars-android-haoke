@echo off
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b %errorlevel%
if not exist build\liquidfun210-core mkdir build\liquidfun210-core
set "CORE=app\src\main\cpp\application"
set "ENTRY=tools\liquidfun210_core.cpp"
set "EXE=core"
cl.exe /nologo /std:c++17 /O2 /EHsc /Ithird_party\liquidfun\liquidfun\Box2D /Itools\difficulty-check\stubs /I%CORE% /Iapp\src\main\cpp\aclib /DLODEPNG_COMPILE_DISK /DFLOW209_PROFILE %ENTRY% %CORE%\expedition.cpp %CORE%\garden_scene.cpp %CORE%\operations.cpp %CORE%\state.cpp %CORE%\items.cpp %CORE%\move.cpp %CORE%\flow_motion.cpp %CORE%\liquidfun_motion.cpp %CORE%\map.cpp %CORE%\ai.cpp %CORE%\ai_tactics.cpp %CORE%\local_tactics.cpp %CORE%\dot.cpp %CORE%\player.cpp %CORE%\random.cpp %CORE%\spiral.cpp %CORE%\info.cpp %CORE%\visual_texture.cpp app\src\main\cpp\aclib\lodepng.cpp build\liquidfun210-core\b2*.obj /Febuild\liquidfun210-core\%EXE%.exe /Fobuild\liquidfun210-core\ > docs\game\liquidfun-2.10\%EXE%-build.log 2>&1
exit /b %errorlevel%




