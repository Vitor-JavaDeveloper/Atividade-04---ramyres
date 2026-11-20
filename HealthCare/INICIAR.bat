@echo off
rem ==================================================================
rem iniciar.bat: liga o healthcare com dois cliques.
rem 1. confere se o docker desktop esta instalado e ligado, e liga se precisar
rem 2. sobe o banco postgresql e o sistema com o docker compose
rem 3. espera o sistema responder e abre o navegador
rem 4. deixa este painel aberto com comandos uteis
rem ==================================================================
setlocal EnableDelayedExpansion
title HealthCare - painel de controle
rem entra na pasta onde este arquivo esta, de onde quer que ele tenha sido aberto
cd /d "%~dp0"
set "URL=http://localhost:8088"
set "MARCA=%~dp0.docker_ligado_pelo_healthcare"

echo.
echo  ==================================================
echo     HealthCare - Controle de Triagem Hospitalar
echo  ==================================================
echo.

rem passo 1: o docker esta instalado?
rem logo depois de instalar, o windows as vezes ainda nao colocou o docker no path;
rem por isso procuramos tambem na pasta padrao de instalacao do docker desktop
where docker >nul 2>&1
if not errorlevel 1 goto temDocker
if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" set "PATH=%ProgramFiles%\Docker\Docker\resources\bin;%PATH%"
where docker >nul 2>&1
if errorlevel 1 goto semDocker
:temDocker

rem passo 2: o docker esta ligado? se nao estiver, liga o docker desktop
docker info >nul 2>&1
if not errorlevel 1 goto dockerOk
echo  [..] Ligando o Docker Desktop. Isso pode levar uns 2 minutos...
set "DD=%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
if not exist "%DD%" set "DD=%LocalAppData%\Programs\Docker\Docker\Docker Desktop.exe"
if not exist "%DD%" goto semDocker
start "" "%DD%"
rem guarda uma marca para o parar.bat saber que fomos nos que ligamos o docker
echo ligado pelo iniciar.bat> "%MARCA%"
set /a tentativas=0
:esperaDocker
timeout /t 3 /nobreak >nul
docker info >nul 2>&1
if not errorlevel 1 goto dockerOk
set /a tentativas+=1
rem 60 tentativas de 3 segundos = 3 minutos
if !tentativas! lss 60 goto esperaDocker
echo  [ERRO] O Docker Desktop nao respondeu. Abra ele manualmente, espere ficar verde e rode este arquivo de novo.
pause
exit /b 1

:dockerOk
echo  [OK] Docker ligado.

rem passo 3: avisa se a porta 8088 ja estiver ocupada por outro programa
docker ps --filter "name=healthcare_app" --format "{{.Names}}" | findstr healthcare_app >nul 2>&1
if not errorlevel 1 goto subir
netstat -ano | findstr /r /c:":8088 .*LISTENING" >nul 2>&1
if errorlevel 1 goto subir
echo  [AVISO] A porta 8088 ja esta em uso por outro programa. Feche ele e tente de novo.
pause
exit /b 1

:subir
rem passo 4: sobe banco e sistema. na primeira vez baixa as imagens e compila, demora alguns minutos.
echo  [..] Subindo banco PostgreSQL e sistema. Na primeira vez pode levar de 3 a 10 minutos...
docker compose up -d --build
if errorlevel 1 goto falhaSubir
call :esperarSistema
if errorlevel 1 goto falhaSubir
start "" "%URL%"
goto menu

rem ------------------------------------------------------------------
rem painel com comandos uteis. fica aberto ate escolher sair.
rem ------------------------------------------------------------------
:menu
echo.
echo  ==================================================
echo   HealthCare rodando em %URL%
echo   Logins de teste (usuario / senha):
echo     admin / admin123          (administrador: relatorios e tudo)
echo     recepcao / recepcao123    (recepcionista: pacientes e medicos)
echo     enfermeiro / enfermeiro123 (enfermeiro: triagem)
echo     medico / medico123        (medica: fila, atendimento, historico)
echo     medico2 / medico123       (segundo medico)
echo  --------------------------------------------------
echo   [1] Abrir o sistema no navegador
echo   [2] Ver status dos containers
echo   [3] Ver ultimas linhas do log do sistema
echo   [4] Reiniciar o sistema
echo   [5] Zerar o banco e voltar aos dados de demonstracao
echo   [6] Abrir o banco no terminal - psql, digite \q para sair
echo   [7] Parar tudo e fechar
echo   [0] Fechar este painel e deixar o sistema rodando
echo  ==================================================
choice /c 12345670 /n /m "  Escolha uma opcao: "
rem o choice devolve a posicao da tecla; testamos do maior para o menor
if errorlevel 8 goto sair
if errorlevel 7 goto parar
if errorlevel 6 goto psql
if errorlevel 5 goto zerar
if errorlevel 4 goto reiniciar
if errorlevel 3 goto logs
if errorlevel 2 goto status
start "" "%URL%"
goto menu

:status
docker compose ps
goto menu

:logs
docker compose logs --tail 40 app
goto menu

:reiniciar
docker compose restart app
call :esperarSistema
goto menu

:zerar
choice /c SN /m "  Isso apaga todos os dados cadastrados. Confirma"
if errorlevel 2 goto menu
rem down -v apaga o volume do banco; ao subir de novo o sistema recria tabelas e dados de teste
docker compose down -v
docker compose up -d
call :esperarSistema
goto menu

:psql
docker exec -it healthcare_db psql -U healthcare -d healthcare
goto menu

:parar
call "%~dp0PARAR.bat" sempausa
exit /b 0

:sair
exit /b 0

:semDocker
echo  [ERRO] O Docker Desktop nao foi encontrado neste computador.
echo         Ele e o unico programa que precisa instalar. Vou abrir a pagina de download.
echo         Depois de instalar e abrir o Docker Desktop uma vez, rode este arquivo de novo.
start "" "https://www.docker.com/products/docker-desktop/"
pause
exit /b 1

:falhaSubir
echo  [ERRO] O sistema nao subiu. Ultimas linhas do log:
docker compose logs --tail 40
echo  Dica: confira se o Docker Desktop esta ligado e se ha internet na primeira execucao.
pause
goto menu

rem ------------------------------------------------------------------
rem sub-rotina: espera o endereco /api/status responder (ate 10 minutos)
rem usa o curl que vem no windows 10 e 11; se nao tiver, usa o powershell
rem ------------------------------------------------------------------
:esperarSistema
echo  [..] Aguardando o sistema responder...
set /a espera=0
:loopEspera
curl -s -f -o nul "%URL%/api/status" >nul 2>&1
if not errorlevel 1 goto prontoEspera
powershell -NoProfile -Command "try { Invoke-WebRequest -UseBasicParsing '%URL%/api/status' -TimeoutSec 3 | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1
if not errorlevel 1 goto prontoEspera
set /a espera+=1
if !espera! geq 200 exit /b 1
timeout /t 3 /nobreak >nul
goto loopEspera
:prontoEspera
echo  [OK] Sistema pronto em %URL%
exit /b 0
