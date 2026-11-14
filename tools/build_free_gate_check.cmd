@echo off
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul
set "CORE=app\src\main\cpp\application"
cl.exe /nologo /std:c++17 /O2 /EHsc /Itools\difficulty-check\stubs /I%CORE% /Iapp\src\main\cpp\aclib /DLODEPNG_COMPILE_DISK tools\strategy-check\GateCheck.cpp %CORE%\state.cpp %CORE%\move.cpp %CORE%\map.cpp %CORE%\dot.cpp %CORE%\player.cpp %CORE%\random.cpp %CORE%\spiral.cpp %CORE%\info.cpp %CORE%\visual_texture.cpp app\src\main\cpp\aclib\lodepng.cpp /Febuild\free-gate-check.exe /Fobuild\ > docs\game\strategy-1.60\gate-build.log 2>&1
if errorlevel 1 exit /b %errorlevel%
build\free-gate-check.exe app\src\main\assets docs\game\strategy-1.60\gates\physical-gates.csv docs\game\strategy-1.60\gates\passage.csv
