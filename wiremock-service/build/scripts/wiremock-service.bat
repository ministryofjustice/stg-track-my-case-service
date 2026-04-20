@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  wiremock-service startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and WIREMOCK_SERVICE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\wiremock-service-0.1.0.jar;%APP_HOME%\lib\wiremock-3.13.0.jar;%APP_HOME%\lib\jackson-datatype-jsr310-2.18.4.jar;%APP_HOME%\lib\jackson-annotations-2.18.4.jar;%APP_HOME%\lib\json-schema-validator-1.5.6.jar;%APP_HOME%\lib\jackson-dataformat-yaml-2.18.4.jar;%APP_HOME%\lib\jackson-core-2.18.4.jar;%APP_HOME%\lib\jackson-databind-2.18.4.jar;%APP_HOME%\lib\slf4j-simple-2.0.17.jar;%APP_HOME%\lib\httpclient5-5.4.3.jar;%APP_HOME%\lib\json-path-2.9.0.jar;%APP_HOME%\lib\handlebars-helpers-4.3.1.jar;%APP_HOME%\lib\handlebars-4.3.1.jar;%APP_HOME%\lib\jetty-alpn-java-client-11.0.24.jar;%APP_HOME%\lib\jetty-proxy-11.0.24.jar;%APP_HOME%\lib\jetty-client-11.0.24.jar;%APP_HOME%\lib\jetty-alpn-client-11.0.24.jar;%APP_HOME%\lib\jetty-alpn-java-server-11.0.24.jar;%APP_HOME%\lib\jetty-alpn-server-11.0.24.jar;%APP_HOME%\lib\http2-server-11.0.24.jar;%APP_HOME%\lib\jetty-webapp-11.0.24.jar;%APP_HOME%\lib\jetty-servlet-11.0.24.jar;%APP_HOME%\lib\jetty-security-11.0.24.jar;%APP_HOME%\lib\jetty-server-11.0.24.jar;%APP_HOME%\lib\jetty-servlets-11.0.24.jar;%APP_HOME%\lib\http2-common-11.0.24.jar;%APP_HOME%\lib\http2-hpack-11.0.24.jar;%APP_HOME%\lib\jetty-http-11.0.24.jar;%APP_HOME%\lib\jetty-io-11.0.24.jar;%APP_HOME%\lib\jetty-xml-11.0.24.jar;%APP_HOME%\lib\jetty-util-11.0.24.jar;%APP_HOME%\lib\slf4j-api-2.0.17.jar;%APP_HOME%\lib\guava-33.4.8-jre.jar;%APP_HOME%\lib\xmlunit-legacy-2.10.0.jar;%APP_HOME%\lib\xmlunit-placeholders-2.10.0.jar;%APP_HOME%\lib\xmlunit-core-2.10.0.jar;%APP_HOME%\lib\json-unit-core-2.40.1.jar;%APP_HOME%\lib\jopt-simple-5.0.4.jar;%APP_HOME%\lib\commons-fileupload-1.5.jar;%APP_HOME%\lib\failureaccess-1.0.3.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\jspecify-1.0.0.jar;%APP_HOME%\lib\error_prone_annotations-2.36.0.jar;%APP_HOME%\lib\j2objc-annotations-3.0.0.jar;%APP_HOME%\lib\httpcore5-h2-5.3.4.jar;%APP_HOME%\lib\httpcore5-5.3.4.jar;%APP_HOME%\lib\hamcrest-core-2.2.jar;%APP_HOME%\lib\json-smart-2.5.0.jar;%APP_HOME%\lib\commons-io-2.11.0.jar;%APP_HOME%\lib\itu-1.10.3.jar;%APP_HOME%\lib\jetty-jakarta-servlet-api-5.0.2.jar;%APP_HOME%\lib\hamcrest-2.2.jar;%APP_HOME%\lib\accessors-smart-2.5.0.jar;%APP_HOME%\lib\snakeyaml-2.3.jar


@rem Execute wiremock-service
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %WIREMOCK_SERVICE_OPTS%  -classpath "%CLASSPATH%" uk.gov.moj.cp.wiremock.WiremockServiceApplication %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable WIREMOCK_SERVICE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%WIREMOCK_SERVICE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
