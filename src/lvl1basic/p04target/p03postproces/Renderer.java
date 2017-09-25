package lvl1basic.p04target.p03postproces;


import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLBuffers;
import oglutils.OGLRenderTarget;
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
import transforms.Mat4Scale;
import transforms.Vec3D;

/**
 * GLSL sample:<br/>
 * Use render to texture to perform rendering post-processing<br/>
 * Requires JOGL 2.3.0 or newer
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2015-11-24
 */

public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height, ox, oy;

	OGLBuffers buffers, bufferStrip;
	OGLTextRenderer textRenderer;

	int shaderProgram, shaderProgramPost, locMat;

	OGLTexture2D texture;
	
	Camera cam = new Camera();
	Mat4 proj;

	OGLRenderTarget renderTarget;
	boolean save = false;
	OGLTexture2D.Viewer textureViewer;
	
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
		
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl1basic/p04target/p03postproces/texture");
		shaderProgramPost = ShaderUtils.loadProgram(gl, "/lvl1basic/p04target/p03postproces/postBlur");
		//shaderProgramPost = ShaderUtils.loadProgram(gl, "/lvl1basic/p04target/p03postproces/postGrey");

		createBuffers(gl);

		locMat = gl.glGetUniformLocation(shaderProgram, "mat");

		texture = new OGLTexture2D(gl, "/textures/mosaic.jpg");
	
		renderTarget = new OGLRenderTarget(gl, 500, 500);

		cam = cam.withPosition(new Vec3D(5, 5, 2.5))
				.withAzimuth(Math.PI * 1.25)
				.withZenith(Math.PI * -0.125);

		gl.glEnable(GL2GL3.GL_DEPTH_TEST);
		textureViewer = new OGLTexture2D.Viewer(gl);
	}

	void createBuffers(GL2GL3 gl) {
		// vertices are not shared among triangles (and thus faces) so each face
		// can have a correct normal in all vertices
		// also because of this, the vertices can be directly drawn as GL_TRIANGLES
		// (three and three vertices form one face) 
		// triangles defined in index buffer
				float[] cube = {
						// bottom (z-) face
						1, 0, 0,	0, 0, -1, 	1, 0,
						0, 0, 0,	0, 0, -1,	0, 0, 
						1, 1, 0,	0, 0, -1,	1, 1, 
						0, 1, 0,	0, 0, -1,	0, 1, 
						// top (z+) face
						1, 0, 1,	0, 0, 1,	1, 0, 
						0, 0, 1,	0, 0, 1,	0, 0, 
						1, 1, 1,	0, 0, 1,	1, 1,
						0, 1, 1,	0, 0, 1,	0, 1,
						// x+ face
						1, 1, 0,	1, 0, 0,	1, 0,
						1, 0, 0,	1, 0, 0,	0, 0, 
						1, 1, 1,	1, 0, 0,	1, 1,
						1, 0, 1,	1, 0, 0,	0, 1,
						// x- face
						0, 1, 0,	-1, 0, 0,	1, 0,
						0, 0, 0,	-1, 0, 0,	0, 0, 
						0, 1, 1,	-1, 0, 0,	1, 1,
						0, 0, 1,	-1, 0, 0,	0, 1,
						// y+ face
						1, 1, 0,	0, 1, 0,	1, 0,
						0, 1, 0,	0, 1, 0,	0, 0, 
						1, 1, 1,	0, 1, 0,	1, 1,
						0, 1, 1,	0, 1, 0,	0, 1,
						// y- face
						1, 0, 0,	0, -1, 0,	1, 0,
						0, 0, 0,	0, -1, 0,	0, 0, 
						1, 0, 1,	0, -1, 0,	1, 1,
						0, 0, 1,	0, -1, 0,	0, 1
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
				new OGLBuffers.Attrib("inNormal", 3),
				new OGLBuffers.Attrib("inTextureCoordinates", 2)
		};

		buffers = new OGLBuffers(gl, cube, attributes, indexBufferData);
	
		// full-screen quad, just NDC positions are needed, texturing
		// coordinates can be calculated from them
		float[] triangleStrip = { 1, -1, 
						1, 1, 
						-1, -1, 
						-1, 1 };
		
		OGLBuffers.Attrib[] attributesStrip = {
				new OGLBuffers.Attrib("inPosition", 2)};

		bufferStrip = new OGLBuffers(gl, triangleStrip, attributesStrip, null);

	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		gl.glUseProgram(shaderProgram);

		// set our render target (texture)
		renderTarget.bind();
		
		gl.glClearColor(0.1f, 0.2f, 0.3f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

		texture.bind(shaderProgram, "textureID", 0);
		
		gl.glUniformMatrix4fv(locMat, 1, false,
				ToFloatArray.convert(cam.getViewMatrix().mul(proj)), 0);
		gl.glUniformMatrix4fv(
				locMat,
				1,
				false,
				ToFloatArray.convert(cam.getViewMatrix().mul(proj)
						.mul(new Mat4Scale((double) width / height, 1, 1))), 0);
		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		gl.glUseProgram(shaderProgramPost);
		
		// set the default render target (screen)
		gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
		// reset viewport but fit the aspect ratio of our render target (1:1)
		// best into the window
		if ((double)width/height > 1.0)
			gl.glViewport(0, 0, height, height);
		else
			gl.glViewport(0, 0, width, width);

		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

		// use the result of the previous draw as a texture for the next
		renderTarget.getColorTexture().bind(shaderProgramPost, "textureID", 0);
		
		// draw the full-screen quad
		bufferStrip.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgramPost);

		textureViewer.view(renderTarget.getColorTexture(), -1, -1, 0.5, 1);
		
		String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD");
		
		textRenderer.drawStr2D(3, height-20, text);
		textRenderer.drawStr2D(width-90, 3, " (c) PGRF UHK");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		this.width = width;
		this.height = height;
		proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 1.0, 100.0);
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
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		gl.glDeleteProgram(shaderProgram);
		gl.glDeleteProgram(shaderProgramPost);
	}

}