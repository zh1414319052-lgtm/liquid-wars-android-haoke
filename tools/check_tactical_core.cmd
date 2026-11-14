@echo off
call tools\build_free_spawn_check.cmd
if errorlevel 1 exit /b %errorlevel%
"C:\Users\Administrator\AppData\Local\Programs\Python\Python312\python.exe" tools\run_free_core_checks.py --mode placement --phase free-core-final --assets app/src/main/assets --seeds 128
exit /b %errorlevel%
