REM Test of start OpenGL demo application using shaders, textures and listenners without AWT 
REM Please copy folowing files to ./bin directory
REM gluegen-rt.jar
REM gluegen-rt-natives-windows-amd64.jar or gluegen-rt-natives-windows-i586.jar
REM jogl-all-noawt.jar
REM jogl-all-noawt-natives-windows-amd64.jar or jogl-all-noawt-natives-windows-i586.jar

cd ./bin
java -classpath "./;gluegen-rt.jar;jogl-all-noawt.jar" lvl2advanced.p01run.JOGLAppNoAwt



