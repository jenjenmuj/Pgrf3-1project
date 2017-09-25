REM Test of start OpenGL demo application using shaders, textures and listenners with AWT 
REM Please copy folowing files to ./bin directory
REM gluegen-rt.jar
REM gluegen-rt-natives-windows-amd64.jar or gluegen-rt-natives-windows-i586.jar
REM jogl-all.jar
REM jogl-all-natives-windows-amd64.jar or jogl-all-natives-windows-i586.jar

cd ./bin
java -classpath "./;gluegen-rt.jar;jogl-all.jar" lvl2advanced.p01run.JOGLApp



