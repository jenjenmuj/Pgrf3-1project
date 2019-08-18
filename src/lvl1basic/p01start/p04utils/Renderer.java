package lvl1basic.p01start.p04utils;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import oglutils.*;
import transforms.*;

import java.awt.event.*;

/**
 * GLSL sample:<br/>
 * Read and compile shader from files "/shader/glsl01/start.*" using ShaderUtils
 * class in oglutils package (older GLSL syntax can be seen in
 * "/shader/glsl01/startForOlderGLSL")<br/>
 * Manage (create, bind, draw) vertex and index buffers using OGLBuffers class
 * in oglutils package<br/>
 * Requires JOGL 2.3.0 or newer
 *
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2015-09-05
 */


// rojekt 2 morfing texturz a tvaru teles

//TODO render from the sun, do not clear the gl, send matrix of the light position, do for the
// for the rotation use ModelView matrix
// bind the Framebuffer not the renderTarget
// use different textures on diferent object
// prednaska 05 stiny - podle vyorecku, kde se orezava W umistit prepocitavani souradnic z vertex bufferu do frame bufferu
//

public class Renderer implements GLEventListener, MouseListener, MouseMotionListener, KeyListener {

    private int width, height;

    private OGLBuffers buffers;
    private OGLTextRenderer textRenderer;
    private OGLRenderTarget renderTarget;
    private OGLTexture2D.Viewer textureViewer;
    private OGLTexture2D texture;

    private int shaderProgramViewer, locTime, lightLocTime, locView, locProjection, locMode, locLightVP, locEyePosition, locLightPosition, locLightPositionPL;
    private int shaderProgramLight, locLightView, locLightProj, locModeLight;
    private int locSunProj, locSunView, locSunPositionPL, locSunLightVP, locSunEyePosition, locSunLightPosition;
    private int shaderProgramTheSun;
    private Mat4 projViewer, projLight;
    private double alpha = 1.570; //90 degree in radians
    //private Mat3Rot2D rotationMatrix = new Mat3Rot2D(alpha);
    private float time = 0;
    private Camera camera, lightCamera, pomCamera;
    private int mx, my;
    private double speed = 0.5;

    @Override
    public void init(GLAutoDrawable glDrawable) {
        // check whether shaders are supported
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        OGLUtils.shaderCheck(gl);

        OGLUtils.printOGLparameters(gl);

        textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());

        //gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);// vyplnění přivrácených i odvrácených stran
        gl.glPolygonMode(GL2GL3.GL_FRONT, GL2GL3.GL_POINT);
        gl.glPolygonMode(GL2GL3.GL_BACK, GL2GL3.GL_POINT);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glEnable(GL2GL3.GL_DEPTH_TEST); // zapnout z-test

        // nacteni shader programu
        shaderProgramLight = ShaderUtils.loadProgram(gl, "/lvl1basic/p01start/p04utils/light");
        shaderProgramViewer = ShaderUtils.loadProgram(gl, "/lvl1basic/p01start/p04utils/start");
        shaderProgramTheSun = ShaderUtils.loadProgram(gl, "/lvl1basic/p01start/p04utils/theSun");

        createBuffers(gl);
        buffers = GridFactory.generateGrid(gl, 100, 100);

        lightCamera = new Camera()
                .withPosition(new Vec3D(5, 5, 5))
                .addAzimuth(5 / 4. * Math.PI)//-3/4.
                .addZenith(-1 / 5. * Math.PI);

        camera = new Camera()
                .withPosition(new Vec3D(0, 0, 0))
                .addAzimuth(5 / 4. * Math.PI)//-3/4.
                .addZenith(-1 / 5. * Math.PI)
                .withFirstPerson(false)
                .withRadius(5);


        // prirazeni textur
        texture = new OGLTexture2D(gl, "/textures/bricks.jpg");
        textureViewer = new OGLTexture2D.Viewer(gl);

        // Uniform promenne pro start.vert
        locTime = gl.glGetUniformLocation(shaderProgramViewer, "time");
        locMode = gl.glGetUniformLocation(shaderProgramViewer, "mode");
        locView = gl.glGetUniformLocation(shaderProgramViewer, "view");
        locProjection = gl.glGetUniformLocation(shaderProgramViewer, "projection");
        locLightVP = gl.glGetUniformLocation(shaderProgramViewer, "lightVP");
        locEyePosition = gl.glGetUniformLocation(shaderProgramViewer, "eyePosition");
        locLightPosition = gl.glGetUniformLocation(shaderProgramViewer, "lightPosition");

        //uniform promenne pro light.vert
        lightLocTime = gl.glGetUniformLocation(shaderProgramLight, "time");
        locLightProj = gl.glGetUniformLocation(shaderProgramLight, "projLight");
        locLightView = gl.glGetUniformLocation(shaderProgramLight, "viewLight");
        locModeLight = gl.glGetUniformLocation(shaderProgramLight, "mode");
        locLightPositionPL = gl.glGetUniformLocation(shaderProgramViewer, "lightPosition");

        locSunProj = gl.glGetUniformLocation(shaderProgramTheSun, "projection");
        locSunView = gl.glGetUniformLocation(shaderProgramTheSun, "view");
        locSunPositionPL = gl.glGetUniformLocation(shaderProgramTheSun, "lightPosition");
        //locSunLightVP = gl.glGetUniformLocation(shaderProgramTheSun, "lightVP");
        //locSunEyePosition = gl.glGetUniformLocation(shaderProgramTheSun, "eyePosition");
        //locSunLightPosition = gl.glGetUniformLocation(shaderProgramTheSun, "lightPosition");

        renderTarget = new OGLRenderTarget(gl, 1024, 1024);
    }

    void createBuffers(GL2GL3 gl) {
        float[] vertexBufferData = {
                -1, -1, 0.7f, 0, 0,
                1, 0, 0, 0.7f, 0,
                0, 1, 0, 0, 0.7f
        };
        int[] indexBufferData = {0, 1, 2};

        // vertex binding description, concise version
        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 2), // 2 floats
                new OGLBuffers.Attrib("inColor", 3) // 3 floats
        };
        buffers = new OGLBuffers(gl, vertexBufferData, attributes, indexBufferData);
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();

        renderFromLight(gl);
        renderFromViewer(gl);
        renderFromTheSun(gl);

        textureViewer.view(texture, -1, -1, 0.5);
        textureViewer.view(renderTarget.getColorTexture(), -1, -0.5, 0.5);
        textureViewer.view(renderTarget.getDepthTexture(), -1, 0, 0.5);

        String text = this.getClass().getName();
        textRenderer.drawStr2D(3, height - 20, text);
        textRenderer.drawStr2D(width - 90, 3, " (c) PGRF UHK");
    }

    private void renderFromLight(GL2GL3 gl) {
        gl.glUseProgram(shaderProgramLight);

        renderTarget.bind();

        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

        gl.glUniform1f(lightLocTime, time);


        gl.glUniformMatrix4fv(locLightView, 1, false, lightCamera.getViewMatrix().floatArray(), 0);
        gl.glUniformMatrix4fv(locLightProj, 1, false, projLight.floatArray(), 0);
        gl.glUniform3fv(locLightPositionPL, 1, ToFloatArray.convert(lightCamera.getPosition()), 0);


        // PARAMETRIC SURFACE
        // Render wall
        gl.glUniform1i(locModeLight, 0);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramLight);
        // Render spining snake
        gl.glUniform1i(locModeLight, 1);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramLight);
        // Render Mobius Band
        gl.glUniform1i(locModeLight, 6);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramLight);

        // PS WITH SPHERICAL COORDS
        // Render elephants head
        gl.glUniform1i(locModeLight, 2);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramLight);
        // Render mouse
        gl.glUniform1i(locModeLight, 3);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramLight);

        // PS WITH CYLINDRICAL COORDS
        // Render Helicoid
        gl.glUniform1i(locModeLight, 4);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramLight);
        // Render Amphore
        gl.glUniform1i(locModeLight, 5);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramLight);

        // Render SUN
        //gl.glUniform1i(locMode, 7);
        //buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramLight);
    }

    private void renderFromViewer(GL2GL3 gl) {
        gl.glUseProgram(shaderProgramViewer);

        gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, width, height);

        gl.glClearColor(0.0f, 0.2f, 0.5f, 1.0f);
        gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

        time += 0.01;
        gl.glUniform1f(locTime, time);


        gl.glUniformMatrix4fv(locView, 1, false, camera.getViewMatrix().floatArray(), 0);
        gl.glUniformMatrix4fv(locProjection, 1, false, projViewer.floatArray(), 0);
        gl.glUniformMatrix4fv(locLightVP, 1, false, lightCamera.getViewMatrix().mul(projLight).floatArray(), 0);
        gl.glUniform3fv(locEyePosition, 1, ToFloatArray.convert(camera.getPosition()), 0);
        gl.glUniform3fv(locLightPosition, 1, ToFloatArray.convert(lightCamera.getPosition()), 0);

        texture.bind(shaderProgramViewer, "textureID", 0);
        //renderTarget.getColorTexture().bind(shaderProgramViewer, "colorTexture", 0);
        renderTarget.getDepthTexture().bind(shaderProgramViewer, "depthTexture", 1);

        // PARAMETRIC SURFACE
        // render wall
        gl.glUniform1i(locMode, 0);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramViewer);
        // render Sppining Snake
        gl.glUniform1i(locMode, 1);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramViewer);
        // Render Mobius Band
        gl.glUniform1i(locMode, 6);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramViewer);

        // PS WITH SPHERICAL COORDS
        // Render Elephants head
        gl.glUniform1i(locMode, 2);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramViewer);
        // render mouse
        gl.glUniform1i(locMode, 3);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramViewer);

        // PS WITH CYLINDRICAL COORDS
        // Render Helicoid
        gl.glUniform1i(locMode, 4);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramViewer);

        // Render Amphore
        gl.glUniform1i(locMode, 5);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramViewer);

        // Render SUN
        //gl.glUniform1i( 1,7);
        //buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramViewer);
    }

    private void renderFromTheSun(GL2GL3 gl) {
        gl.glUseProgram(shaderProgramTheSun);

        gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, width, height);

        //gl.glClearColor(0.0f, 0.2f, 0.5f, 1.0f);
        //gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

        time += 0.01;
        //gl.glUniform1f(locTime, time);


        //rotuju svetlo
        Mat3RotZ rotatationMatrixZAxis = new Mat3RotZ(0.01);

        //pomCamera = new Camera().withPosition(lightCamera.getPosition().mul(rotatationMatrixZAxis));
        //lightCamera = pomCamera;

        Vec3D direction = new Vec3D(
                // TODO Calculate direction for light
        );
        lightCamera.move(direction);

        gl.glUniform3fv(locSunPositionPL, 1, ToFloatArray.convert(lightCamera.getPosition()), 0);

        gl.glUniformMatrix4fv(locSunView, 1, false, camera.getViewMatrix().floatArray(), 0);
        gl.glUniformMatrix4fv(locSunProj, 1, false, projViewer.floatArray(), 0);

        //gl.glUniformMatrix4fv(locSunLightVP, 1, false, lightCamera.getViewMatrix().mul(projLight).floatArray(), 0);

        //gl.glUniform3fv(locSunEyePosition, 1, ToFloatArray.convert(camera.getPosition()), 0);
       // gl.glUniform3fv(locSunLightPosition, 1, ToFloatArray.convert(lightCamera.getPosition()), 0);
        gl.glUniform3fv(locSunPositionPL, 1, ToFloatArray.convert(lightCamera.getPosition()), 0);



        //lightCamera.withPosition(lightCamera.getPosition().mul(rotatationMatrixZAxis));

        texture.bind(shaderProgramViewer, "textureID", 0);
        renderTarget.getColorTexture().bind(shaderProgramViewer, "colorTexture", 0);
        //renderTarget.getDepthTexture().bind(shaderProgramViewer, "depthTexture", 1);



        // Render SUN
        gl.glUniform1i(locMode, 7);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramViewer);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        this.width = width;
        this.height = height;
        textRenderer.updateSize(width, height);

        double ratio = height / (double) width;
        projLight = new Mat4OrthoRH(5 / ratio, 5, 0.1, 20);
//        projViewer = new Mat4OrthoRH(5 / ratio, 5, 0.1, 20);
        projViewer = new Mat4PerspRH(Math.PI / 3, ratio, 1, 20.0);
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        gl.glDeleteProgram(shaderProgramViewer);
        gl.glDeleteProgram(shaderProgramLight);
        gl.glDeleteProgram(shaderProgramTheSun);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mx = e.getX();
        my = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        camera = camera.addAzimuth(Math.PI * (mx - e.getX()) / width);
        camera = camera.addZenith(Math.PI * (e.getY() - my) / width);
        mx = e.getX();
        my = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // W
        if (e.getKeyCode() == 87) camera = camera.forward(speed);
        // S
        if (e.getKeyCode() == 83) camera = camera.backward(speed);
        // A
        if (e.getKeyCode() == 65) camera = camera.left(speed);
        // D
        if (e.getKeyCode() == 68) camera = camera.right(speed);
        // E Up
        if (e.getKeyCode() == 69) camera = camera.up(speed);
        // Q Down
        if (e.getKeyCode() == 81) camera = camera.down(speed);
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

}