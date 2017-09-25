package lvl2advanced.p03texture.p01quad;

import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLBuffers;
import oglutils.OGLRenderTarget;
import oglutils.OGLTexImageFloat;
import oglutils.OGLTextRenderer;
import oglutils.OGLTexture2D;
import oglutils.OGLUtils;
import oglutils.ShaderUtils;
import oglutils.ToFloatArray;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import transforms.Camera;
import transforms.Mat4;
import transforms.Mat4Identity;
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

public class Renderer implements GLEventListener, MouseListener,
	MouseMotionListener, KeyListener {

	int width, height, ox, oy;

	OGLBuffers buffers;
	OGLTextRenderer textRenderer;

	int shaderProgram, locMat;
	
	OGLTexture2D texture;
	OGLTexture2D texture2;
	OGLTexture2D texture3;
	OGLTexture2D texture4;

	Camera cam = new Camera();
	Mat4 proj;
	OGLTexture2D.Viewer textureViewer;
	OGLRenderTarget renderTarget1, renderTarget2;

	
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
		// e.g. in Eclipse via main menu Project/Properties/Java Build
		// Path/Source
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p03texture/p01quad/textureQuad");
		createBuffers(gl);

		locMat = gl.glGetUniformLocation(shaderProgram, "mat");

		// load texture using JOGL objects
		// texture files are in /res/textures/

		texture = new OGLTexture2D(gl, "/textures/testTexture.png");
		texture2 = new OGLTexture2D(gl, "/textures/testTexture.jpg");
		texture3 = new OGLTexture2D(gl, "/textures/testTexture.gif");
		texture4 = new OGLTexture2D(gl, "/textures/testTexture.bmp");

		texture.setTexImage(addAxes(texture.getTexImage(new OGLTexImageFloat.Format(4))));
		texture2.setTexImage(addAxes(texture2.getTexImage(new OGLTexImageFloat.Format(4))));
		texture3.setTexImage(addAxes(texture3.getTexImage(new OGLTexImageFloat.Format(4))));
		texture4.setTexImage(addAxes(texture4.getTexImage(new OGLTexImageFloat.Format(4))));
		
		cam = cam.withPosition(new Vec3D(0.5, 0.5, 2))
				.withAzimuth(Math.PI /2)
				.withZenith(-Math.PI /2);
		
		
		renderTarget1 = new OGLRenderTarget(gl, texture.getWidth(), texture.getHeight());
		renderTarget2 = new OGLRenderTarget(gl, texture.getWidth(), texture.getHeight());

		
		gl.glEnable(GL2GL3.GL_DEPTH_TEST);
		textureViewer = new OGLTexture2D.Viewer(gl);
	}

	OGLTexImageFloat addAxes(OGLTexImageFloat image){
		int bold = 10;
		//draw axes to texture
		for (int i = 0; i<image.getWidth(); i++)
			for(int j=0; j<bold; j++){
				image.setPixel(i, j, 0, 1.0f); //red
			image.setPixel(i, j, 1, 0.0f); //green
			image.setPixel(i, j, 2, 0.0f); //blue
			}
		for (int i = 0; i<image.getHeight(); i++)
			for(int j=0; j<bold; j++){
			image.setPixel(j, i, 0, 0.0f); //red
			image.setPixel(j, i, 1, 1.0f); //green
			image.setPixel(j, i, 2, 0.0f); //blue
		}
		for (int i = 0; i<bold; i++)
			for(int j=0; j<bold; j++){
			image.setPixel(j, i, 0, 0.0f); //red
			image.setPixel(j, i, 1, 0.0f); //green
			image.setPixel(j, i, 2, 1.0f); //blue
		}
		//update image
		return image;
	}
	void createBuffers(GL2GL3 gl) {
		// vertices are not shared among triangles (and thus faces) so each face
		// can have a correct normal in all vertices
		// also because of this, the vertices can be directly drawn as GL_TRIANGLES
		// (three and three vertices form one face) 
		// triangles defined in index buffer
		float[] cube = {
						// bottom (z-) face
						1, 0, 0,	0, 0, -1,	1, 0, 
						0, 0, 0,	0, 0, -1,	0, 0, 
						1, 1, 0,	0, 0, -1,	1, 1, 
						0, 1, 0,	0, 0, -1,	0, 1
						
		};
		int[] indexBufferData = {0, 1, 2, 1, 2, 3};		
		OGLBuffers.Attrib[] attributes = {
				new OGLBuffers.Attrib("inPosition", 3),
				new OGLBuffers.Attrib("inNormal", 3),
				new OGLBuffers.Attrib("inTextureCoordinates", 2)
		};

		buffers = new OGLBuffers(gl, cube, attributes, indexBufferData);
	}
	
	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		
		//render to texture
		renderTarget1.bind();

		gl.glClearColor(0.9f, 0.9f, 0.6f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);
		
		gl.glUseProgram(shaderProgram); 
		gl.glUniformMatrix4fv(locMat, 1, false,
				//ToFloatArray.convert(cam.getViewMatrix().mul(proj)), 0);
				ToFloatArray.convert((new Mat4Identity())), 0);
		
		texture2.bind(shaderProgram, "textureID", 0);
		
		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		//end of render to texture
		
		//add axis to rendered texture and update
		renderTarget1.getColorTexture().setTexImage(
				addAxes(renderTarget1.getColorTexture().getTexImage(new OGLTexImageFloat.Format(4))));
		
		//render to texture
		renderTarget2.bind();
		gl.glClearColor(0.9f, 0.6f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);
		
		gl.glUseProgram(shaderProgram); 
		gl.glUniformMatrix4fv(locMat, 1, false,
				//ToFloatArray.convert(cam.getViewMatrix().mul(proj)), 0);
				ToFloatArray.convert((new Mat4Identity())), 0);
		
		texture2.bind(shaderProgram, "textureID", 0);
		
		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);
		//end of render to texture

		BufferedImage img = renderTarget2.getColorTexture().toBufferedImage();
		Graphics gr = img.getGraphics();
		gr.setColor(new Color(0xff0000ff)); //red
		gr.drawLine(0, 0, img.getWidth(), 150);
		gr.setColor(new Color(0xff00ff00));  //green
		gr.drawLine(0, 0, 150, img.getHeight());
		gr.setColor(new Color(0xffff0000)); //blue
		gr.drawLine(0, 0, img.getWidth()/2, img.getHeight()/2);
		img.setRGB(0, 0, 0xffffffff); //white
		gr.setColor(new Color(0xffffffff)); //white
		String data = "Buffered Image";
		//char[] data = new char[] {'Buffered Image'};
		gr.drawChars(data.toCharArray(), 0, data.length(), 100, 100);
		renderTarget2.getColorTexture().fromBufferedImage(img);
		
		// set the default render target (screen)
		gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, width, height);

		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);
		
		gl.glUseProgram(shaderProgram); 
		gl.glUniformMatrix4fv(locMat, 1, false,
				//ToFloatArray.convert(cam.getViewMatrix().mul(proj)), 0);
				ToFloatArray.convert((new Mat4Identity())), 0);
		
		renderTarget2.getColorTexture().bind(shaderProgram, "textureID", 0);
		
		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		textureViewer.view(texture, -1, -1, 0.5);
		textureViewer.view(texture2, -1, -0.5, 0.5);
		textureViewer.view(texture3, -1, 0, 0.5);
		textureViewer.view(texture4, -1, 0.5, 0.5);
		
		textureViewer.view(renderTarget1.getColorTexture(), 0, -1, 0.5);

		textureViewer.view(renderTarget2.getColorTexture(), -0.5, -1, 0.5);
		

			
		String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD");
		
		textRenderer.drawStr2D(3, height-20, text);
		textRenderer.drawStr2D(width-90, 3, " (c) PGRF UHK");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		this.width = width;
		this.height = height;
		proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 0.01, 1000.0);
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