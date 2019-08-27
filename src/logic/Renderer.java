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
    private OGLTexture2D texture, textureSmile, textureWow;
    private int shaderProgramViewer, locTime, lightLocTime, locView, locProjection, locMode, locMode2, locLightVP, locEyePosition, locTransl;
    private int shaderProgramLight, locLightView, locLightProj, locModeLight, locModeLight2, locLightPosition, locLightPositionPL, locTranslLight;
    private int shaderProgramTextureBlend, locTBTime, locTBView, locTBProjection, locTBMode, locTBLightVP, locTBEyePosition, locTBTransl, locTBLightPosition;
    private int randomObjectMode;
    private int randomObjectMode2;
    private boolean viewerPerspective;
    private Vec3D light1 =  new Vec3D(5, 5, 5);
    private Mat4ViewRH viewLight;
    private Mat4Transl translationY1 = new Mat4Transl(0,1,0);
    private Mat4RotX rotX90 = new Mat4RotX (1.5708);
    private int structure = 2;
    private String structureMessage = "GL Fill";
    private Mat4 projViewer, projLight;
    private int rotationOfLight = 3;
    private String rotationMessage = "Rotation of Light around Z axis";
    private int numberOfObjects = 6;
    private float time = 0, time2 = 0.1f;
    private  boolean stopTime = false;
    private Camera camera;
    private int mx, my;
    private double cameraSpeed = 0.5;
    private boolean blendTexturesAction = false;
    private ArrayList<Integer> listOfTextures = new ArrayList<Integer>();
    private String objectMessage1, objectMessage2;

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
        shaderProgramLight = ShaderUtils.loadProgram(gl, "/light");
        shaderProgramViewer = ShaderUtils.loadProgram(gl, "/start");
        shaderProgramTextureBlend = ShaderUtils.loadProgram(gl, "/textureBlend");

        createBuffers(gl);
        buffers = GridFactory.generateGrid(gl, 20, 20);
//
//        lightCamera = new Camera()
//                .withPosition(new Vec3D(5, 5, 5))
//                .addAzimuth(5 / 4. * Math.PI)//-3/4.
//                .addZenith(-1 / 5. * Math.PI);

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

        // Uniform promenne pro start.vert
        locTime = gl.glGetUniformLocation(shaderProgramViewer, "time");
        locMode = gl.glGetUniformLocation(shaderProgramViewer, "mode");
        locMode2 = gl.glGetUniformLocation(shaderProgramViewer, "mode2");
        locView = gl.glGetUniformLocation(shaderProgramViewer, "view");
        locTransl = gl.glGetUniformLocation(shaderProgramViewer, "translace");
        locProjection = gl.glGetUniformLocation(shaderProgramViewer, "projection");
        locLightVP = gl.glGetUniformLocation(shaderProgramViewer, "lightVP");
        locEyePosition = gl.glGetUniformLocation(shaderProgramViewer, "eyePosition");
        locLightPosition = gl.glGetUniformLocation(shaderProgramViewer, "lightPosition");


        locTBTime = gl.glGetUniformLocation(shaderProgramTextureBlend, "time");
        locTBView = gl.glGetUniformLocation(shaderProgramTextureBlend, "view");
        locTBTransl = gl.glGetUniformLocation(shaderProgramTextureBlend, "translace");
        locTBProjection = gl.glGetUniformLocation(shaderProgramTextureBlend, "projection");


        //uniform promenne pro light.vert
        lightLocTime = gl.glGetUniformLocation(shaderProgramLight, "time");
        locLightProj = gl.glGetUniformLocation(shaderProgramLight, "projLight");
        locLightView = gl.glGetUniformLocation(shaderProgramLight, "viewLight");
        locTranslLight = gl.glGetUniformLocation(shaderProgramLight, "translaceLight");
        locModeLight = gl.glGetUniformLocation(shaderProgramLight, "mode");
        locModeLight2 = gl.glGetUniformLocation(shaderProgramLight, "mode2");
        locLightPositionPL = gl.glGetUniformLocation(shaderProgramViewer, "lightPosition");

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
            objectMessage1 = setMessageforObejct(randomObjectMode);
            randomObjectMode2 = rnd.nextInt(numberOfObjects);
            objectMessage2 = setMessageforObejct(randomObjectMode2);
        }

        //change shader program
        if (blendTexturesAction) {
            renderFromTextureBlend(gl);
            //System.out.println("RenderFromTextureBLend");
        } else {
            //renderFromLight(gl);
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
                "Movement WSAD + QE (up and down)",
                "Press I to change structure",
                "Press P to change Viewer perspective",
                "Press R to change axis of Light rotation",
                "Press O to stop all movement"};

        Color c = new Color(0xFFAD76);
        textRenderer.setColor(c);
        int textOrganizer = 10;
        for(String s : helpText) {
            textRenderer.drawStr2D(3, height - textOrganizer, s);
            textOrganizer += 14;
        }

        textRenderer.drawStr2D(width - 90, 3, " (c) PGRF UHK");
        textRenderer.drawStr2D(width - 80, height - 20, structureMessage );
        textRenderer.drawStr2D(width - 200, height - 35, rotationMessage );
        textRenderer.drawStr2D(width / 2 - 20, 3, "First object is: " + objectMessage1);
        textRenderer.drawStr2D(width / 2 - 20, 20, "Second object is: " + objectMessage2);

        double ratio = height / (double) width;

        // switching perspective for viewer and for light
        if (viewerPerspective) projViewer = new Mat4OrthoRH(5 / ratio, 5, 0.1, 20);
        else projViewer = new Mat4PerspRH(Math.PI / 3, ratio, 1, 20.0);

        projLight = new Mat4PerspRH(Math.PI / 3, 1, 1, 20.0);
    }

    //render z pohledu pozorovatele
    private void renderFromViewer(GL2GL3 gl) {
        gl.glUseProgram(shaderProgramViewer);

        gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, width, height);

        gl.glClearColor(0.0f, 0.2f, 0.5f, 1.0f);
        gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

        stopTime();

        gl.glUniform1f(locTime, time);
        gl.glUniformMatrix4fv(locView, 1, false, camera.getViewMatrix().floatArray(), 0);
        gl.glUniformMatrix4fv(locProjection, 1, false, projViewer.floatArray(), 0);
        gl.glUniformMatrix4fv(locTransl, 1, false, ToFloatArray.convert(translationY1), 0);
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
        stopTime();

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
        gl.glDeleteProgram(shaderProgramLight);
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
            stopTime = !stopTime;
        }
        // R down
        if (e.getKeyCode() == 82) {
            rotationOfLight++;
            if (rotationOfLight == 4) rotationOfLight = 0;
        }
        // G down
        if (e.getKeyCode() == 71) {
            blendTexturesAction = !blendTexturesAction;
        }

    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public String setMessageforObejct(int object) {
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

    public void stopTime() {
        if (!stopTime) {
            time = (float) Math.cos(time2);
            time2 += 0.01;
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