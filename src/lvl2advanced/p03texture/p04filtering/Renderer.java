package lvl2advanced.p03texture.p04filtering;


import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLBuffers;
import oglutils.OGLTexImageByte;
import oglutils.OGLTextRenderer;
import oglutils.OGLTexture2D;
import oglutils.OGLUtils;
import oglutils.ShaderUtils;
import oglutils.ToFloatArray;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import transforms.Camera;
import transforms.Mat4;
import transforms.Mat4PerspRH;
import transforms.Vec3D;

/**
 * GLSL sample:<br/>
 * Load and apply texture using OGLTexture from oglutils package<br/>
 * Requires JOGL 2.3.0 or newer
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2016-10-06
 */

public class Renderer implements GLEventListener, MouseListener, MouseMotionListener, KeyListener {

	int width, height, ox, oy;

	OGLBuffers buffers;
	OGLTextRenderer textRenderer;

	int shaderProgram, locMat;

	OGLTexture2D texture, textureGrid;

	Camera cam = new Camera();
	Mat4 proj;
	OGLTexture2D.Viewer textureViewer;

	int mode = 1, locMode, modeTex = 2, modeInter = 5, texSource;
	int level, locLevel;

	int size = 1024;
	int maxLevel;

	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		OGLUtils.shaderCheck(gl);
		
		//get and set debug version of GL class
		gl = OGLUtils.getDebugGL(gl);
		glDrawable.setGL(gl);
		
		OGLUtils.printOGLparameters(gl);

		textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());
		
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p03texture/p04filtering/textureInterpolation");
		createBuffers(gl);

		locMat = gl.glGetUniformLocation(shaderProgram, "mat");
		locMode = gl.glGetUniformLocation(shaderProgram, "mode");
		locLevel = gl.glGetUniformLocation(shaderProgram, "level");

		// load texture using JOGL objects
		// texture files are in /res/textures/
		texture = new OGLTexture2D(gl, "/textures/testTexture.png");
		gl.glGenerateMipmap(GL2GL3.GL_TEXTURE_2D);
		// gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_GENERATE_MIPMAP,
		// GL2GL3.GL_TRUE); //removed from GL 3.1 and above.

		OGLTexImageByte imageGrid = new OGLTexImageByte(size, size, new OGLTexImageByte.Format(4));
		for (int i = 0; i < size; i += 20)
			for (int j = 0; j < size; j += 20)
				for (int m = 0; m < 10; m++)
					for (int n = 0; n < 10; n++) {
						imageGrid.setPixel(i + m, j + n, 1, (byte) 0xff);
						imageGrid.setPixel(i + m, j + n, 2, (byte) 0xff);
						imageGrid.setPixel(i + m, j + n, (byte) 0xff);
					}

		textureGrid = new OGLTexture2D(gl, imageGrid);
		gl.glGenerateMipmap(GL2GL3.GL_TEXTURE_2D);

		// coloring first MIP level
		imageGrid = textureGrid.getTexImage(new OGLTexImageByte.Format(4), 1);
		for (int i = 0; i < imageGrid.getWidth(); i++)
			for (int j = 0; j < imageGrid.getHeight(); j++) {
				imageGrid.setPixel(i, j, 1, (byte) 0xff);
			}
		textureGrid.setTexImage(imageGrid, 1);

		// coloring second MIP level
		imageGrid = textureGrid.getTexImage(new OGLTexImageByte.Format(4), 2);
		for (int i = 0; i < imageGrid.getWidth(); i++)
			for (int j = 0; j < imageGrid.getHeight(); j++) {
				imageGrid.setPixel(i, j, 2, (byte) 0xff);
			}
		textureGrid.setTexImage(imageGrid, 2);

		// coloring third MIP level
		imageGrid = textureGrid.getTexImage(new OGLTexImageByte.Format(4), 3);
		for (int i = 0; i < imageGrid.getWidth(); i++)
			for (int j = 0; j < imageGrid.getHeight(); j++) {
				imageGrid.setPixel(i, j, 0, (byte) 0xff);
			}
		textureGrid.setTexImage(imageGrid, 3);

		cam = cam.withPosition(new Vec3D(1.01, 1.01, -0.02)).withAzimuth(Math.PI * 1.25).withZenith(Math.PI * 0.125);

		gl.glEnable(GL2GL3.GL_DEPTH_TEST);
		textureViewer = new OGLTexture2D.Viewer(gl);
	}

	void createBuffers(GL2GL3 gl) {
		float[] face = {
				// face
				1, 0, 0, 0, 0, 0, 0, 1, 
				0, 0, 0, 0, 0, 0, 1, 1,
				1, 1, 0, 0, 0, 0, 0, 0, 
				0, 1, 0, 0, 0, 0, 1, 0

		};

		OGLBuffers.Attrib[] attributes = { 
				new OGLBuffers.Attrib("inPosition", 3), 
				new OGLBuffers.Attrib("inNormal", 3),
				new OGLBuffers.Attrib("inTextureCoordinates", 2) };

		buffers = new OGLBuffers(gl, face, attributes, null);
	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD");

		OGLTexture2D testTexture;
		
		if (texSource == 1)
			testTexture = texture;
		else
			testTexture = textureGrid;
		
		testTexture.bind();
		maxLevel = 1 + (int) Math.floor((Math.log(Math.max(testTexture.getHeight(), testTexture.getWidth())) / Math.log(2)));

		switch (modeTex) {
		case 0:
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_CLAMP_TO_EDGE);
			text += ", [t]exMode: GL_CLAMP_TO_EDGE";
			break;
		case 1:
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_CLAMP_TO_BORDER);
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_CLAMP_TO_BORDER);
			text += ", [t]exMode: GL_CLAMP_TO_BORDER";
			break;
		case 2:
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_REPEAT);
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_REPEAT);
			text += ", [t]exMode: GL_REPEAT";
			break;
		case 3:
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_MIRRORED_REPEAT);
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_MIRRORED_REPEAT);
			text += ", [t]exMode: GL_MIRRORED_REPEAT";
		}

		switch (modeInter) {
		case 0:
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_NEAREST);
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_NEAREST);
			text += ", [i]nterpolation: GL_NEAREST";
			break;
		case 1:
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR);
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_LINEAR);
			text += ", [i]nterpolation: GL_LINEAR";
			break;
		case 2:
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_NEAREST_MIPMAP_NEAREST);
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_NEAREST);
			text += ", [i]nterpolation: GL_NEAREST_MIPMAP_NEAREST";
			break;
		case 3:
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_NEAREST_MIPMAP_LINEAR);
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_NEAREST);
			text += ", [i]nterpolation: GL_NEAREST_MIPMAP_LINEAR";
			break;
		case 4:
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR_MIPMAP_NEAREST);
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_LINEAR);
			text += ", [i]nterpolation: GL_LINEAR_MIPMAP_NEAREST";
			break;
		case 5:
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR_MIPMAP_LINEAR);
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_LINEAR);
			text += ", [i]nterpolation: GL_LINEAR_MIPMAP_LINEAR";
		}

		switch (mode) {
		case 1:
			text += ", [m]ode: smooth";
			break;
		case 2:
			text += ", [m]ode: flat";
			break;
		case 3:
			text += ", [m]ode: noperspective";
			break;
		default:
			text += ", [m]ode: default";
		}

		text += ", [n] texture source";

		text += ", [l]evel of MIP " + level + "/" + maxLevel;

		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

		gl.glUseProgram(shaderProgram);
		gl.glUniformMatrix4fv(locMat, 1, false, ToFloatArray.convert(cam.getViewMatrix().mul(proj)), 0);

		gl.glUniform1i(locMode, mode);
		gl.glUniform1i(locLevel, level);

		buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram);

		//show loaded texture and its mip layers
		textureViewer.view(texture, 0.25, -1, 0.5);
		for(int i= 0; i<8; i++)
			textureViewer.view(texture, 0.75, i*0.25-1, 0.25, 1.0, i);
		
		//show made texture and its mip layers
		textureViewer.view(textureGrid, -0.75, -1, 0.5);
		for(int i= 0; i<8; i++)
			textureViewer.view(textureGrid, -1, i*0.25-1, 0.25, 1.0, i);
		
		textRenderer.drawStr2D(3, height - 20, text);
		textRenderer.drawStr2D(width - 90, 3, " (c) PGRF UHK");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		this.width = width;
		this.height = height;
		proj = new Mat4PerspRH(Math.PI / 0.6, height / (double) width, 0.01, 20.0);
		textRenderer.updateSize(width, height);
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
		ox = e.getX();
		oy = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		cam = cam.addAzimuth((double) Math.PI * (ox - e.getX()) / width)
				.addZenith((double) Math.PI * (e.getY() - oy) / width);
		ox = e.getX();
		oy = e.getY();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		double step = 0.1;
		switch (e.getKeyCode()) {
		case KeyEvent.VK_X:// moving forward without changing z coordinate
			double z = cam.getPosition().getZ();
			Vec3D pos = cam.forward(step).getPosition();
			cam = cam.withPosition(new Vec3D(pos.getX(), pos.getY(), z));
			break;
		case KeyEvent.VK_Z: // moving backward without changing z coordinate
			z = cam.getPosition().getZ();
			pos = cam.backward(step).getPosition();
			cam = cam.withPosition(new Vec3D(pos.getX(), pos.getY(), z));
			break;
		case KeyEvent.VK_W:
			cam = cam.forward(step);
			break;
		case KeyEvent.VK_D:
			cam = cam.right(step);
			break;
		case KeyEvent.VK_S:
			cam = cam.backward(step);
			break;
		case KeyEvent.VK_A:
			cam = cam.left(step);
			break;
		case KeyEvent.VK_CONTROL:
			cam = cam.down(step);
			break;
		case KeyEvent.VK_SHIFT:
			cam = cam.up(step);
			break;
		case KeyEvent.VK_SPACE:
			cam = cam.withFirstPerson(!cam.getFirstPerson());
			break;
		case KeyEvent.VK_R:
			cam = cam.mulRadius(0.9f);
			break;
		case KeyEvent.VK_F:
			cam = cam.mulRadius(1.1f);
			break;
		case KeyEvent.VK_N:
			texSource = (texSource + 1) % 2;
			break;
		case KeyEvent.VK_M:
			mode = (mode + 1) % 4;
			break;
		case KeyEvent.VK_L:
			level = (level + 1) % maxLevel;
			break;
		case KeyEvent.VK_T:
			modeTex = (modeTex + 1) % 4;
			break;
		case KeyEvent.VK_I:
			modeInter = (modeInter + 1) % 6;
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
		glDrawable.getGL().getGL2GL3().glDeleteProgram(shaderProgram);
	}
}