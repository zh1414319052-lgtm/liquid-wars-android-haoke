@echo off
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b %errorlevel%
set "LABEL=%~1"
if "%LABEL%"=="" set "LABEL=core"
if exist build\follow-1.82\%LABEL%-build.log exit /b 9
set "CORE=app\src\main\cpp\application"
set "SOURCE=%~2"
if "%SOURCE%"=="" set "SOURCE=tools\follow182.cpp"
cl.exe /nologo /std:c++17 /O2 /EHsc /Itools\difficulty-check\stubs /I%CORE% /Iapp\src\main\cpp\aclib /DLODEPNG_COMPILE_DISK %SOURCE% %CORE%\operations.cpp %CORE%\state.cpp %CORE%\items.cpp %CORE%\move.cpp %CORE%\map.cpp %CORE%\ai.cpp %CORE%\ai_tactics.cpp %CORE%\local_tactics.cpp %CORE%\dot.cpp %CORE%\player.cpp %CORE%\random.cpp %CORE%\spiral.cpp %CORE%\info.cpp %CORE%\visual_texture.cpp app\src\main\cpp\aclib\lodepng.cpp /Febuild\follow-1.82\%LABEL%.exe /Fobuild\follow-1.82\ > build\follow-1.82\%LABEL%-build.log 2>&1
exit /b %errorlevel%
