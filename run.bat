@echo off
setlocal
rem UTF-8로 컴파일 + 실행
javac -encoding UTF-8 -d . taprun/core/*.java taprun/modes/*.java taprun/Main.java || exit /b 1
java taprun.Main
