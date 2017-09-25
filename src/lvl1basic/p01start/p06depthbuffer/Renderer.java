package lvl1basic.p01start.p06depthbuffer;

import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLBuffers;
import oglutils.OGLTextRenderer;
import oglutils.OGLUtils;
import oglutils.ShaderUtils;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/**
 * GLSL sample:<br/>
 * Draw two different geometries with two different shader programs using depth buffer<br/>
 * Requires JOGL 2.3.0 or newer
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2015-09-05
 */
public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height;

	OGLBuffers buffers, buffers2;
	OGLTextRenderer textRenderer;
	
	int shaderProgram, shaderProgram2, locTime, locTime2;

	float time = 0;

	
	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		OGLUtils.shaderCheck(gl);
		
		OGLUtils.printOGLparameters(gl);

		textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());
		
		// shader files are in /shaders/ directory
		// shaders directory must be set as a source directory of the project
		// e.g. in Eclipse via main menu Project/Properties/Java Build Path/Source
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl1basic/p01start/p06depthbuffer/start");
		shaderProgram2 = ShaderUtils.loadProgram(gl, "/lvl1basic/p01start/p06depthbuffer/start2");
		createBuffers(gl);
		
		locTime = gl.glGetUniformLocation(shaderProgram, "time");
		locTime2 = gl.glGetUniformLocation(shaderProgram2, "time"); 
		
		//enable depth test
		gl.glEnable(GL2GL3.GL_DEPTH_TEST);
	}
	
	void createBuffers(GL2GL3 gl) {
		float[] vertexBufferData = {
			-1, -1, 	0.7f, 0, 0, 
			 1,  0,		0, 0.7f, 0,
			 0,  1,		0, 0, 0.7f 
		};
		int[] indexBufferData = { 0, 1, 2 };
		OGLBuffers.Attrib[] attributes = {
				new OGLBuffers.Attrib("inPosition", 2),
				new OGLBuffers.Attrib("inColor", 3)
		};
		
		buffers = new OGLBuffers(gl, vertexBufferData, attributes,
				indexBufferData);
		
		float[] vertexBufferDataPos = {
			-1, 1, 
			0.5f, 0,
			-0.5f, -1 
		};
		float[] vertexBufferDataCol = {
			0, 1, 1, 
			1, 0, 1,
			1, 1, 1 
		};
		OGLBuffers.Attrib[] attributesPos = {
				new OGLBuffers.Attrib("inPosition", 2),
		};
		OGLBuffers.Attrib[] attributesCol = {
				new OGLBuffers.Attrib("inColor", 3)
		};
		
		buffers2 = new OGLBuffers(gl, vertexBufferDataPos, attributesPos,
				indexBufferData);
		buffers2.addVertexBuffer(vertexBufferDataCol, attributesCol);
	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		
		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);
		
		time += 0.1;
		// set the current shader to be used
		gl.glUseProgram(shaderProgram); 
		gl.glUniform1f(locTime, time); // correct shader must be set before this

		// bind and draw
		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);
				
		// set the current shader to be used
		gl.glUseProgram(shaderProgram2); 
		gl.glUniform1f(locTime2, time); // correct shader must be set before this
		
		// bind and draw
		buffers2.draw(GL2GL3.GL_TRIANGLES, shaderProgram2);
		
		String text = new String(this.getClass().getName());
		textRenderer.drawStr2D(3, height - 20, text);
		textRenderer.drawStr2D(width - 90, 3, " (c) PGRF UHK");

	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		this.width = width;
		this.height = height;
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
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
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
		gl.glDeleteProgram(shaderProgram2);
	}

}