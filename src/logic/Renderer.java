package logic;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import oglutils.*;
import transforms.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

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

public class Renderer implements GLEventListener, MouseListener, MouseMotionListener, KeyListener {


    private int width, height;
    private OGLBuffers buffers;
    private OGLTextRenderer textRenderer;
    private OGLRenderTarget renderTarget;
    private OGLTexture2D.Viewer textureViewer;
    private OGLTexture2D texture, textureSmile, textureWow;
    private int shaderProgramViewer, locTime, locView, locProjection, locMode, locMode2, locLightVP, locEyePosition, locTransl;
    private int shaderProgramTextureBlend, locTBTime, locTBView, locTBProjection, locTBTransl;
    private int randomObjectMode;
    private int randomObjectMode2;
    private float timeFromUser;
    private boolean viewerPerspective;
    private Mat4Transl translation = new Mat4Transl(0,0,0);
    private Mat4RotX rotX90 = new Mat4RotX (1.5708);
    private int structure = 2;
    private String structureMessage = "GL Fill";
    private Mat4 projViewer, projLight;
    private String rotationMessage = "Rotation of Light around Z axis";
    private int numberOfObjects = 6;
    private float time = 0, time2 = 0.1f, time3 = 0;
    private boolean stopTime = false, userHandelingTime = false;
    private Camera camera;
    private int mx, my;
    private double cameraSpeed = 0.5;
    private boolean blendTexturesAction = false;


    @Override
    public void init(GLAutoDrawable glDrawable) {
        // check whether shaders are supported
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        OGLUtils.shaderCheck(gl);

        OGLUtils.printOGLparameters(gl);

        textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());

        gl.glEnable(GL2GL3.GL_DEPTH_TEST);
        gl.glPointSize(1.5f);

        // nacteni shader programu
        shaderProgramViewer = ShaderUtils.loadProgram(gl, "/start");
        shaderProgramTextureBlend = ShaderUtils.loadProgram(gl, "/textureBlend");

        createBuffers(gl);
        buffers = GridFactory.generateGrid(gl, 20, 20);

        camera = new Camera()
                .withPosition(new Vec3D(0, 0, 0))
                .addAzimuth(5 / 4. * Math.PI)//-3/4.
                .addZenith(-1 / 5. * Math.PI)
                .withFirstPerson(false)
                .withRadius(5);


        // prirazeni textur

        texture = new OGLTexture2D(gl, "/textures/texture1.jpg");
        textureSmile = new OGLTexture2D(gl, "/textures/smile.jpg");
        textureWow = new OGLTexture2D(gl, "/textures/wow.jpg");

        textureViewer = new OGLTexture2D.Viewer(gl);

        // Uniform promenne pro start.vert/frag
        locTime = gl.glGetUniformLocation(shaderProgramViewer, "time");
        locMode = gl.glGetUniformLocation(shaderProgramViewer, "mode");
        locMode2 = gl.glGetUniformLocation(shaderProgramViewer, "mode2");
        locView = gl.glGetUniformLocation(shaderProgramViewer, "view");
        locTransl = gl.glGetUniformLocation(shaderProgramViewer, "translace");
        locProjection = gl.glGetUniformLocation(shaderProgramViewer, "projection");

        // uniform promenne pro textureBlend.vert/frag
        locTBTime = gl.glGetUniformLocation(shaderProgramTextureBlend, "time");
        locTBView = gl.glGetUniformLocation(shaderProgramTextureBlend, "view");
        locTBTransl = gl.glGetUniformLocation(shaderProgramTextureBlend, "translace");
        locTBProjection = gl.glGetUniformLocation(shaderProgramTextureBlend, "projection");

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
        // posilame vektor ktery ma prvni dve slozky position a dalsi 3 barvu (rgb)
        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 2), // 2 floats
                new OGLBuffers.Attrib("inColor", 3) // 3 floats
        };
        buffers = new OGLBuffers(gl, vertexBufferData, attributes, indexBufferData);
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();

        //set random objects
        while (randomObjectMode == randomObjectMode2) {
            Random rnd = new Random();
            randomObjectMode = rnd.nextInt(numberOfObjects);
            randomObjectMode2 = rnd.nextInt(numberOfObjects);
        }

        // set selected objects

        //change shader program
        if (blendTexturesAction) {
            renderFromTextureBlend(gl);
        } else {
            renderFromViewer(gl);
        }

        // get back polygon mode to display windows correctly
        gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
        textureViewer.view(texture, -1, -1, 0.25);

        String[] helpText = {
                "PRESS G TO CHANGE SHADER PROGRAM! ",
                "",
                "Press T to select random objects for blending",
                "Press Arrows <- and -> to blend object yourself",
                "Press Arrows Up to select first object",
                "Press Arrows Down to select second object",
                "Press O to stop and start automatic movement",
                "Movement WSAD + QE (up and down)",
                "Press I to change structure",
                "Press P to change Viewer perspective"
                };


        Color c = new Color(0xFFAD76);
        textRenderer.setColor(c);
        int textOrganizer = 10;
        for(String s : helpText) {
            textRenderer.drawStr2D(3, height - textOrganizer, s);
            textOrganizer += 14;
        }

        textRenderer.drawStr2D(width - 90, 3, " (c) PGRF UHK");


        Color cx = new Color(0xFF8FF3);
        textRenderer.setColor(cx);
        textRenderer.drawStr2D(width / 2 - 30, 37, "First object is: " + getMessageforObejct(randomObjectMode));
        textRenderer.drawStr2D(width / 2 - 30, 20, "Second object is: " + getMessageforObejct(randomObjectMode2));
        textRenderer.drawStr2D(width / 2 - 30, 3, "Using Structure: " + structureMessage);

        double ratio = height / (double) width;

        // switching perspective for viewer and for light
        if (viewerPerspective) projViewer = new Mat4OrthoRH(5 / ratio, 5, 0.1, 20);
        else projViewer = new Mat4PerspRH(Math.PI / 3, ratio, 1, 20.0);

    }

    //render z pohledu pozorovatele
    private void renderFromViewer(GL2GL3 gl) {
        gl.glUseProgram(shaderProgramViewer);

        gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, width, height);

        gl.glClearColor(0.0f, 0.2f, 0.5f, 1.0f);
        gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

        time();

        gl.glUniform1f(locTime, time);
        gl.glUniformMatrix4fv(locView, 1, false, camera.getViewMatrix().floatArray(), 0);
        gl.glUniformMatrix4fv(locProjection, 1, false, projViewer.floatArray(), 0);
        gl.glUniformMatrix4fv(locTransl, 1, false, ToFloatArray.convert(translation), 0);
        gl.glUniform1i(locMode, randomObjectMode);
        gl.glUniform1i(locMode2, randomObjectMode2);

        // prepinani struktury - points, fill a line
        structureMode(gl);

        texture.bind(shaderProgramViewer, "textureID", 0);

        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramViewer);
    }


    private void renderFromTextureBlend(GL2GL3 gl) {
        gl.glUseProgram(shaderProgramTextureBlend);

        gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, width, height);

        gl.glClearColor(0.0f, 0.2f, 0.5f, 1.0f);
        gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

        // zastaveni casu
        time();

        gl.glUniform1f(locTBTime, time);
        gl.glUniformMatrix4fv(locTBView, 1, false, camera.getViewMatrix().floatArray(), 0);
        gl.glUniformMatrix4fv(locTBProjection, 1, false, projViewer.floatArray(), 0);

        structureMode(gl);

        // bind textures
        textureSmile.bind(shaderProgramTextureBlend, "textureID1", 0);
        textureWow.bind(shaderProgramTextureBlend, "textureID2", 2);

        // translate wall 90
        gl.glUniformMatrix4fv(locTBTransl, 1, false, ToFloatArray.convert(rotX90), 0);

        // Render wall
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramTextureBlend);

    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        this.width = width;
        this.height = height;
        textRenderer.updateSize(width, height);

        double ratio = height / (double) width;

        projViewer = new Mat4OrthoRH(5 / ratio , 5, 0.1, 20);
        projLight = new Mat4PerspRH(Math.PI / 3, 1, 1, 20.0);

    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        gl.glDeleteProgram(shaderProgramViewer);
        gl.glDeleteProgram(shaderProgramTextureBlend);
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
        if (e.getKeyCode() == 87) camera = camera.forward(cameraSpeed);
        // S
        if (e.getKeyCode() == 83) camera = camera.backward(cameraSpeed);
        // A
        if (e.getKeyCode() == 65) camera = camera.left(cameraSpeed);
        // D
        if (e.getKeyCode() == 68) camera = camera.right(cameraSpeed);
        // E Up
        if (e.getKeyCode() == 69) camera = camera.up(cameraSpeed);
        // Q Down
        if (e.getKeyCode() == 81) camera = camera.down(cameraSpeed);
        // I down
        if (e.getKeyCode() == 73) {
            structure++;
            if (structure == 3) structure = 0;
        }
        // P down
        if (e.getKeyCode() == 80) {
            viewerPerspective = !viewerPerspective;
        }
        // O down
        if (e.getKeyCode() == 79) {
            if (userHandelingTime) userHandelingTime = false;
            stopTime = !stopTime;
        }
        // G down
        if (e.getKeyCode() == 71) {
            blendTexturesAction = !blendTexturesAction;
        }
        // T down
        if (e.getKeyCode() == 84) {
            randomObjectMode = randomObjectMode2;
        }
        // LeftArrow down
        if (e.getKeyCode() == 37) {
            userHandelingTime = true;
            timeFromUser += 0.1;
        }
        // RightArrow down
        if (e.getKeyCode() == 39) {
            userHandelingTime = true;
            timeFromUser -= 0.1;
        }
        // UpArrow down
        if (e.getKeyCode() == 38) {
            randomObjectMode++;
            if (randomObjectMode == randomObjectMode2) randomObjectMode++;
            if (randomObjectMode == numberOfObjects) randomObjectMode = 0;
        }
        // DownArrow down
        if (e.getKeyCode() == 40) {
            randomObjectMode2++;
            if (randomObjectMode == randomObjectMode2) randomObjectMode2++;
            if (randomObjectMode2 == numberOfObjects) randomObjectMode2 = 0;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public String getMessageforObejct(int object) {
        String message;
        switch (object) {
            case 0: message ="Ball"; break;
            case 1: message ="Elipsoid"; break;
            case 2: message ="Wall"; break;
            case 3: message ="Mobius band"; break;
            case 4: message ="Elephant head"; break;
            case 5: message ="Snake"; break;
            default: message = "Object";
        }
        return message;
    }

    public void time() {

        if (userHandelingTime) {
            stopTime = true;
            time3 = time2 + timeFromUser;
            time = (float) Math.cos(time3);
        }
        if (!stopTime) {
            time3 = time2 + timeFromUser;
            time = (float) Math.cos(time3);
            time2+= 0.01;
        }
    }

    public void structureMode(GL2GL3 gl) {
        switch (structure % 3) {
            default:
            case 0: {
                gl.glPolygonMode(GL2GL3.GL_FRONT, GL2GL3.GL_POINT);
                gl.glPolygonMode(GL2GL3.GL_BACK, GL2GL3.GL_POINT);
                structureMessage = "GL Point";
            }
            break;
            case 1: {
                gl.glPolygonMode(GL2GL3.GL_FRONT, GL2.GL_LINE);
                gl.glPolygonMode(GL2GL3.GL_BACK, GL2.GL_LINE);
                structureMessage = "GL Line";
            }
            break;
            case 2: {
                gl.glPolygonMode(GL2GL3.GL_FRONT, GL2.GL_FILL);
                gl.glPolygonMode(GL2GL3.GL_BACK, GL2.GL_FILL);
                structureMessage = "GL Fill";
            }
            break;
        }
    }
}