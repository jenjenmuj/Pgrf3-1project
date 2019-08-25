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
    private OGLTexture2D texture, texture0, texture1, texture2, texture3;
    private int shaderProgramViewer, locTime, lightLocTime, locView, locProjection, locMode, locLightVP, locEyePosition, locLightPosition, locLightPositionPL;
    private int shaderProgramLight, locLightView, locLightProj, locModeLight;
    private int shaderProgramTheSun, locSunProj, locSunView, locSunPositionPL;
    private boolean viewerPerspective;
    private boolean lightPerspective;
    private Vec3D light1 =  new Vec3D(5, 5, 5);
    private Mat4ViewRH viewLight;
    private int structure = 2;
    private String structureMessage = "GL Fill";
    private Mat4 projViewer, projLight;
    private int rotationOfLight = 0;
    private String rotationMessage = "Rotation of Light around Z axis";
    private int numberOfObjects = 8;
    private float time = 0;
    private  boolean stopTime = false;
    private Camera camera, lightCamera;
    private int mx, my;
    private double cameraSpeed = 0.5;
    private int textureCounter, numberOfTextures = 4;
    private boolean diffTextures = false;
    private ArrayList<Integer> listOfTextures = new ArrayList<Integer>();
    private boolean comingFromRenderViewer = false;

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
        shaderProgramTheSun = ShaderUtils.loadProgram(gl, "/theSun");

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

        texture = new OGLTexture2D(gl, "/textures/bricks.jpg");
        texture0 = new OGLTexture2D(gl, "/textures/texture0.jpg");
        texture1 = new OGLTexture2D(gl, "/textures/texture1.jpg");
        texture2 = new OGLTexture2D(gl, "/textures/texture2.jpg");
        texture3 = new OGLTexture2D(gl, "/textures/texture3.jpg");

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

        renderFromLight(gl);
        renderFromViewer(gl);
        renderFromTheSun(gl);

        // get back polygon mode to display windows correctly
        gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
        textureViewer.view(texture, -1, -1, 0.25);
        textureViewer.view(renderTarget.getColorTexture(), -1, -.75, 0.25);
        textureViewer.view(renderTarget.getDepthTexture(), -1, -0.5, 0.25);

                String[] helpText = {
                "Movement WSAD + QE (up and down)",
                "Press I to change structure",
                "Press T to change texture",
                "Press G to use random texture per object",
                "Press P to change Viewer perspective",
                "Press L to change Light perspective",
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

        double ratio = height / (double) width;

        // switching perspective for viewer and for light
        if (viewerPerspective) projViewer = new Mat4OrthoRH(5 / ratio, 5, 0.1, 20);
        else projViewer = new Mat4PerspRH(Math.PI / 3, ratio, 1, 20.0);
        if (!lightPerspective) projLight = new Mat4OrthoRH(5 , 5, 0.1, 20);
        else projLight = new Mat4PerspRH(Math.PI / 3, 1, 1, 20.0);
    }

    // rendrovani z pohledu svetla
    private void renderFromLight(GL2GL3 gl) {
        gl.glUseProgram(shaderProgramLight);

        // bind rendertargetu
        renderTarget.bind();

        //rotation of light around all 3 axis
         switch (rotationOfLight) {
                    default:
                    case 0: {
                        viewLight = new Mat4ViewRH(light1,light1.mul(-1),new Vec3D(0, 0, 1));
                        light1 = new Point3D(new Vec3D(5, 5, 5)).mul(new Mat4RotZ(time)).ignoreW();
                        rotationMessage = "Rotation of Light around Z axis";
                    } break;
                    case 1: {
                        viewLight = new Mat4ViewRH(light1,light1.mul(-1),new Vec3D(0, 1, 0));
                        light1 = new Point3D(new Vec3D(5, 5, 5)).mul(new Mat4RotY(time)).ignoreW();
                        rotationMessage = "Rotation of Light around Y axis";
                    } break;
                    case 2: {
                        viewLight = new Mat4ViewRH(light1,light1.mul(-1),new Vec3D(1, 0, 0));
                        light1 = new Point3D(new Vec3D(5, 5, 5)).mul(new Mat4RotX(time)).ignoreW();
                        rotationMessage = "Rotation of Light around X axis";
                    } break;
                }


        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

        gl.glUniform1f(lightLocTime, time);
        gl.glUniformMatrix4fv(locLightView, 1, false, viewLight.floatArray(), 0);
        gl.glUniformMatrix4fv(locLightProj, 1, false, projLight.floatArray(), 0);
        gl.glUniform3fv(locLightPositionPL, 1, ToFloatArray.convert(light1) , 0);

        // Render all object from light persp.
        for(int i = 0; i < numberOfObjects; i++) {
            renderThemAll(gl, i);
        }
    }

    //render z pohledu pozorovatele
    private void renderFromViewer(GL2GL3 gl) {
        gl.glUseProgram(shaderProgramViewer);

        gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, width, height);

        gl.glClearColor(0.0f, 0.2f, 0.5f, 1.0f);
        gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

        // zastaveni casu
        if (!stopTime) {
            time += 0.01;
        }

        gl.glUniform1f(locTime, time);
        // Matice, ktera urcuje kam koukam
        gl.glUniformMatrix4fv(locView, 1, false, camera.getViewMatrix().floatArray(), 0);
        // projection matice - ohranicuje prostor kam koukam
        gl.glUniformMatrix4fv(locProjection, 1, false, projViewer.floatArray(), 0);
        //viewLight je matice co otaci svetlem a nasobi se s projLight coz je matice, ktera ohranicuje prostor, kam sviti svetlo, jejich skladanim se otaci svetlo
        gl.glUniformMatrix4fv(locLightVP, 1, false, viewLight.mul(projLight).floatArray(), 0);
        //pozice pozorovatele
        gl.glUniform3fv(locEyePosition, 1, ToFloatArray.convert(camera.getPosition()), 0);
        //pozice svetla
        gl.glUniform3fv(locLightPosition, 1, ToFloatArray.convert(light1), 0); //pozice svetla

        // prepinani struktury - points, fill a line
        switch(structure % 3) {
            default:
            case 0: {
                gl.glPolygonMode(GL2GL3.GL_FRONT, GL2GL3.GL_POINT);
                gl.glPolygonMode(GL2GL3.GL_BACK, GL2GL3.GL_POINT);
                structureMessage = "GL Point";
            } break;
            case 1: {
                gl.glPolygonMode(GL2GL3.GL_FRONT, GL2.GL_LINE);
                gl.glPolygonMode(GL2GL3.GL_BACK, GL2.GL_LINE);
                structureMessage = "GL Line";
            } break;
            case 2: {
                gl.glPolygonMode(GL2GL3.GL_FRONT, GL2.GL_FILL);
                gl.glPolygonMode(GL2GL3.GL_BACK, GL2.GL_FILL);
                structureMessage = "GL Fill";
            } break;
        }

        // vyrendrovani vsech teles, pocet teles je potreba zmenit v prommene numberOfObjects
        for(int i = 0; i < numberOfObjects; i++) {
            comingFromRenderViewer = true;
            renderThemAll(gl, i);
            comingFromRenderViewer = false;
        }
    }

    private void renderThemAll(GL2GL3 gl, int mode) {

        //render from viewer
        if (comingFromRenderViewer) {
            getTexture(mode);
            gl.glUniform1i(locMode, mode);
            buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramViewer);
        }

        // render from Light
        gl.glUniform1i(locModeLight, mode);
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramLight);

    }

    private void renderFromTheSun(GL2GL3 gl) {
        gl.glUseProgram(shaderProgramTheSun);

        gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, width, height);

        gl.glUniform3fv(locSunPositionPL, 1, ToFloatArray.convert(light1), 0);
        gl.glUniformMatrix4fv(locSunView, 1, false, camera.getViewMatrix().floatArray(), 0);
        gl.glUniformMatrix4fv(locSunProj, 1, false, projViewer.floatArray(), 0);

        // Render SUN
        buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgramTheSun);
    }

    public void  getTexture(int mode) {
        // prirazeni textur telesum a prepinani, mame jen 4 textury - ulozeno v promenne numberOfTextures
        if (diffTextures) {
            if (listOfTextures.size() <= numberOfObjects) {
                Random r = new Random();
                listOfTextures.add(r.nextInt(numberOfObjects - 1));

            }
            // prirazeni textur na zaklade pozice v listu, pozice se urcuje pomoci mode
            switch (listOfTextures.get(mode)) {
                case 0:
                case 4:
                    texture = texture0;
                    break;
                case 1:
                case 5:
                    texture = texture1;
                    break;
                case 2:
                case 6:
                    texture = texture2;
                    break;
                case 3:
                case 7:
                    texture = texture3;
                    break;
            }
        }
        // prirazeni rextury podle prave vybrane textury
        else {

            switch (textureCounter) {
                case 0:
                    texture = texture0;
                    break;
                case 1:
                    texture = texture1;
                    break;
                case 2:
                    texture = texture2;
                    break;
                case 3:
                    texture = texture3;
                    break;
            }
        }

        texture.bind(shaderProgramViewer, "textureID", 0);
        renderTarget.getDepthTexture().bind(shaderProgramViewer, "depthTexture", 1);

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
        // L down
        if (e.getKeyCode() == 76) {
            lightPerspective = !lightPerspective;
        }
        // O down
        if (e.getKeyCode() == 79) {
            stopTime = !stopTime;
        }
        // R down
        if (e.getKeyCode() == 82) {
            rotationOfLight++;
            if (rotationOfLight == 3) rotationOfLight = 0;
        }
        // T down
        if (e.getKeyCode() == 84) {
            if (diffTextures) diffTextures = false;
            textureCounter++;
            if (textureCounter == numberOfTextures) textureCounter = 0;
        }
        // G down
        if (e.getKeyCode() == 71) {
            listOfTextures.clear();
            diffTextures = !diffTextures;
            textureCounter = 0;
        }

    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

}