package lvl2advanced.p03texture.p02cubetexture;


import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLBuffers;
import oglutils.OGLTextRenderer;
import oglutils.OGLTextureCube;
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
 * Load and apply cube texture using OGLTextureCube from oglutils package<br/>
 * Requires JOGL 2.3.0 or newer
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2015-09-06
 */

public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height, ox, oy;

	OGLBuffers buffers;
	OGLTextRenderer textRenderer;

	int shaderProgram, locMat;
	
	OGLTextureCube texture;

	Camera cam = new Camera();
	Mat4 proj;
	Mat4 swapYZflipZ = new Mat4(new double[] {
			1, 0, 0, 0,
			0, 0, -1, 0,
			0, 1, 0, 0,
			0, 0, 1, 1,
	}); 
	OGLTextureCube.Viewer textureViewer;

	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		OGLUtils.shaderCheck(gl);

		// get and set debug version of GL class
		gl = OGLUtils.getDebugGL(gl);
		glDrawable.setGL(gl);
		
		OGLUtils.printOGLparameters(gl);

		textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());
		
		// shader files are in /shaders/ directory
		// shaders directory must be set as a source directory of the project
		// e.g. in Eclipse via main menu Project/Properties/Java Build Path/Source
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p03texture/p02cubetexture/textureCube");
		createBuffers(gl);

		locMat = gl.glGetUniformLocation(shaderProgram, "mat");

		// texture files are in /res/textures/
		// texture = new OGLTextureCube(gl, "/textures/skyBox_.jpg",OGLTextureCube.SUFFICES_RIGHT_LEFT);
		// texture = new OGLTextureCube(gl, "/textures/snow_.jpg",OGLTextureCube.SUFFICES_POSITIVE_NEGATIVE);
		String[] names = {"/textures/snow_positive_x.jpg",
				"/textures/snow_negative_x.jpg",
				"/textures/snow_negative_y.jpg",
				"/textures/snow_positive_y.jpg",
				"/textures/snow_positive_z.jpg",
				"/textures/snow_negative_z.jpg"};
		
		texture = new OGLTextureCube(gl, names);
		
		cam = cam.withPosition(new Vec3D(0.5, 0.5, 0.5));

		gl.glEnable(GL2GL3.GL_DEPTH_TEST);
		textureViewer = new OGLTextureCube.Viewer(gl);
	}

	void createBuffers(GL2GL3 gl) {
		// vertices are not shared among triangles (and thus faces) so each face
				// can have a correct normal in all vertices
				// also because of this, the vertices can be directly drawn as GL_TRIANGLES
				// (three and three vertices form one face) 
				// triangles defined in index buffer
				float[] cube = {
						// bottom (z-) face
						1, 0, 0,	0, 0, -1,
						0, 0, 0,	0, 0, -1,
						1, 1, 0,	0, 0, -1,
						0, 1, 0,	0, 0, -1,
						// top (z+) face
						1, 0, 1,	0, 0, 1,
						0, 0, 1,	0, 0, 1,
						1, 1, 1,	0, 0, 1,
						0, 1, 1,	0, 0, 1,
						// x+ face
						1, 1, 0,	1, 0, 0,
						1, 0, 0,	1, 0, 0,
						1, 1, 1,	1, 0, 0,
						1, 0, 1,	1, 0, 0,
						// x- face
						0, 1, 0,	-1, 0, 0,
						0, 0, 0,	-1, 0, 0,
						0, 1, 1,	-1, 0, 0,
						0, 0, 1,	-1, 0, 0,
						// y+ face
						1, 1, 0,	0, 1, 0,
						0, 1, 0,	0, 1, 0,
						1, 1, 1,	0, 1, 0,
						0, 1, 1,	0, 1, 0,
						// y- face
						1, 0, 0,	0, -1, 0,
						0, 0, 0,	0, -1, 0,
						1, 0, 1,	0, -1, 0,
						0, 0, 1,	0, -1, 0
				};

				int[] indexBufferData = new int[36];
				for (int i = 0; i<6; i++){
					indexBufferData[i*6] = i*4;
					indexBufferData[i*6 + 1] = i*4 + 1;
					indexBufferData[i*6 + 2] = i*4 + 2;
					indexBufferData[i*6 + 3] = i*4 + 1;
					indexBufferData[i*6 + 4] = i*4 + 2;
					indexBufferData[i*6 + 5] = i*4 + 3;
				}
				OGLBuffers.Attrib[] attributes = {
						new OGLBuffers.Attrib("inPosition", 3),
						new OGLBuffers.Attrib("inNormal", 3)
				};

				buffers = new OGLBuffers(gl, cube, attributes, indexBufferData);
	}
	
	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);
		
		gl.glUseProgram(shaderProgram); 
		gl.glUniformMatrix4fv(locMat, 1, false,
				ToFloatArray.convert(swapYZflipZ.mul(cam.getViewMatrix()).mul(proj)), 0);
		
		texture.bind(shaderProgram, "textureID", 0);
		
		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);
		
		textureViewer.view(texture);
		
		String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD");
		
		textRenderer.drawStr2D(3, height-20, text);
		textRenderer.drawStr2D(width-90, 3, " (c) PGRF UHK");
	}

	@Override
	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width,
			int height) {
		this.width = width;
		this.height = height;
		proj = new Mat4PerspRH(Math.PI / 3, height / (double) width, 0.01, 1000.0);
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
		switch (e.getKeyCode()) {
		case KeyEvent.VK_W:
			cam = cam.forward(1);
			break;
		case KeyEvent.VK_D:
			cam = cam.right(1);
			break;
		case KeyEvent.VK_S:
			cam = cam.backward(1);
			break;
		case KeyEvent.VK_A:
			cam = cam.left(1);
			break;
		case KeyEvent.VK_CONTROL:
			cam = cam.down(1);
			break;
		case KeyEvent.VK_SHIFT:
			cam = cam.up(1);
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