package lvl1basic.p02geometry.p02strip;

import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLBuffers;
import oglutils.OGLTextRenderer;
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
 * Draw 3D geometry, use camera and projection transformations<br/>
 * Requires JOGL 2.3.0 or newer
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2015-09-05
 */

public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height, ox, oy;

	OGLBuffers buffers, buffers2, buffers3;
	OGLTextRenderer textRenderer;

	int shaderProgram, locMat;
	int mode = 0;
	boolean polygons = true;
	
	Camera cam = new Camera();
	Mat4 proj; // created in reshape()

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
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl1basic/p02geometry/p02strip/simple");
		createBuffers(gl);

		locMat = gl.glGetUniformLocation(shaderProgram, "mat");

		cam = cam.withPosition(new Vec3D(5, 5, 2.5))
				.withAzimuth(Math.PI * 1.25)
				.withZenith(Math.PI * -0.125);
		
		gl.glEnable(GL2GL3.GL_DEPTH_TEST);
	}

	void createBuffers(GL2GL3 gl) {
		// triangles defined in vertex buffer
		float[] strip = {
				// first triangle
				1, 0, 0,	0, 0, -1,
				0, 0, 0,	0, 0, -1,
				1, 1, 0,	0, 0, -1,
				// second triangle
				0, 0, 0,	0, -1, 0,
				1, 1, 0,	0, -1, 0,
				0, 1, 0,	0, -1, 0,
				// 3st triangle
				1, 1, 0,	-1, 0, 0,
				0, 1, 0,	-1, 0, 0,
				1, 2, 0,	-1, 0, 0,
				// 4th triangle
				0, 1, 0,	0, 1, 0,
				1, 2, 0,	0, 1, 0,
				0, 2, 0,	0, 1, 0,
				// 5th triangle
				1, 2, 0,	0, 0, 1,
				0, 2, 0,	0, 0, 1,
				1, 3, 0,	0, 0, 1,
				// 6th triangle
				0, 2, 0,	1, 0, 0,
				1, 3, 0,	1, 0, 0,
				0, 3, 0,	1, 1, 1,
						
		};

		OGLBuffers.Attrib[] attributes = {
				new OGLBuffers.Attrib("inPosition", 3),
				new OGLBuffers.Attrib("inNormal", 3)
		};

		//create geometry without index buffer as the triangle list 
		buffers = new OGLBuffers(gl, strip, attributes, null);
		
		int[] indexBufferData = new int[9];
		for (int i = 0; i<9; i+=3){
			indexBufferData[i] = 2*i;
			indexBufferData[i+1] = 2*i+1;
			indexBufferData[i+2] = 2*i+2;
		}
		//create geometry with index buffer as the triangle list
		buffers2 = new OGLBuffers(gl, strip, attributes, indexBufferData);
				
		int[] indexBufferData2 = {0,1,2,5,8,11,14,17};
		//create geometry with index buffer as the triangle strip
		buffers3 = new OGLBuffers(gl, strip, attributes, indexBufferData2);
		
	}

	
	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		
		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);
		
		gl.glUseProgram(shaderProgram); 
		gl.glUniformMatrix4fv(locMat, 1, false,
				ToFloatArray.convert(cam.getViewMatrix().mul(proj)), 0);
		
		String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD");
		
		if (polygons){
			gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
			text +=", [p]olygon: fill";
			} else{
			gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
			text +=", [p]olygon: line";
			}
			
		switch(mode % 9){
		case 0:
			text +=", [m]ode: all triangles of triangle list, without index buffer";
			buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);
		break;
		case 1:
			text +=", [m]ode: first 3 triangles of triangle list, without index buffer";
			//number of vertices 
			buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram, 9);
		break;
		case 2:
			text +=", [m]ode: 3rd, 4th and 5th triangles of triangle list, without index buffer";
			//number of vertices, index of the first vertex 
			buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram, 9, 6);
		break;
		case 3:
			text +=", [m]ode: odd triangles of triangle list, with defined index buffer";
			buffers2.draw(GL2GL3.GL_TRIANGLES, shaderProgram);
		break;
		case 4:
			text +=", [m]ode: 1st and 2nd odd triangles of triangle list, with defined index buffer";
			//number of vertices
			buffers2.draw(GL2GL3.GL_TRIANGLES, shaderProgram, 6);
		break;
		case 5:
			text +=", [m]ode: 2nd and 3rd odd triangles of triangle list, with defined index buffer";
			//number of vertices, index of the first vertex 
			buffers2.draw(GL2GL3.GL_TRIANGLES, shaderProgram, 6, 3);
		break;
		case 6:
			text +=", [m]ode: all triangles of triangle strip, with defined index buffer";
			buffers3.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram);
		break;
		case 7:
			text +=", [m]ode: first 3 triangles of triangle strip, with defined index buffer";
			//number of vertices
			buffers3.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram, 5);
		break;
		case 8:
			text +=", [m]ode: 3rd and 4th triangles of triangle strip, with defined index buffer";
			//number of vertices, index of the first vertex 
			buffers3.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram, 4, 2);
		break;
		}
		
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
		case KeyEvent.VK_M:
			mode++;
			break;
		case KeyEvent.VK_P:
			polygons = !polygons;
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