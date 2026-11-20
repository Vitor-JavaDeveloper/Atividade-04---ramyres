@echo off
rem ==================================================================
rem parar.bat: desliga o healthcare de forma segura.
rem para o sistema e o banco (os dados ficam guardados no volume do docker)
rem e, se foi o iniciar.bat que ligou o docker desktop, desliga ele tambem.
rem ==================================================================
title HealthCare - parando
cd /d "%~dp0"
set "MARCA=%~dp0.docker_ligado_pelo_healthcare"
echo.
echo  [..] Parando o HealthCare...

rem se o docker ainda nao estiver no path, usa a pasta padrao do docker desktop
where docker >nul 2>&1
if errorlevel 1 if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" set "PATH=%ProgramFiles%\Docker\Docker\resources\bin;%PATH%"
docker info >nul 2>&1
if errorlevel 1 goto dockerDesligado

rem para e remove os containers, mas mantem o volume com os dados do banco
docker compose down
echo  [OK] Sistema e banco parados. Os dados continuam salvos.

if not exist "%MARCA%" goto fim
echo  [..] Desligando o Docker Desktop, que foi ligado pelo INICIAR.bat...
rem versoes novas do docker desktop tem o comando "docker desktop stop"; nas antigas fechamos o programa
docker desktop stop >nul 2>&1
if errorlevel 1 taskkill /im "Docker Desktop.exe" /f >nul 2>&1
del "%MARCA%" >nul 2>&1
echo  [OK] Docker Desktop desligado.
goto fim

:dockerDesligado
echo  [OK] O Docker ja esta desligado, nada para parar.
if exist "%MARCA%" del "%MARCA%" >nul 2>&1

:fim
echo.
rem quando o iniciar.bat chama este arquivo ele passa "sempausa" para nao travar a janela
if /i not "%~1"=="sempausa" pause
exit /b 0
