package lvl2advanced.p05pipeline.p03query;

import com.jogamp.opengl.DebugGL3;
import com.jogamp.opengl.GL3;
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
 * Query sample 
 * Requires JOGL 2.3.0 or newer
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2016-11-11
 */

public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height;

	OGLBuffers buffers;
	OGLTextRenderer textRenderer;
	
	int shaderProgram;
	int[] query, result;
	long[] resultLong;
	int mode = 0;
	
	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		OGLUtils.shaderCheck(glDrawable.getGL().getGL2GL3());
		if (OGLUtils.getVersionGLSL(glDrawable.getGL().getGL2GL3()) < 330){
					System.err.println("Query is not supported"); 
					System.exit(0);
		}

		glDrawable.setGL(new DebugGL3(glDrawable.getGL().getGL3()));
		GL3 gl = glDrawable.getGL().getGL3();

		textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());

		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p05pipeline/p03query/feedbackDraw");

		createInputBuffer(gl);
		result = new int[4];
		resultLong = new long[1];
		query = new int[4];
		
	}

	private void createInputBuffer(GL3 gl) {
		int[] indexBufferData = { 0, 1, 2, 3 };
	
		float[] vertexBufferDataPos1 = {
				-.5f, -.1f,  0.0f, 1.0f, 0.1f,
				-.3f, .5f,  0.0f, 1.0f, 1.0f,
				.2f, -.4f,  0.0f, 0.5f, 0.5f,
				.3f, .8f,  0.0f, 0.1f, 1.0f, 
					};
			
		OGLBuffers.Attrib[] attributesPos = { 
				new OGLBuffers.Attrib("inPosition", 2),
				new OGLBuffers.Attrib("inColor", 3), };
		buffers = new OGLBuffers(gl, vertexBufferDataPos1, attributesPos, indexBufferData);
	}
	
	
	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL3 gl = glDrawable.getGL().getGL3();

		gl.glGenQueries(query.length, query, 0);

		// Query how samples (pixels) were rasterized
		gl.glBeginQuery(GL3.GL_SAMPLES_PASSED, query[0]);
		
		// Query how many elements were drawn
		gl.glBeginQuery(GL3.GL_PRIMITIVES_GENERATED, query[1]);

		// Query time counter of rendering
		gl.glBeginQuery(GL3.GL_TIME_ELAPSED, query[2]);

		gl.glPointSize(5f);
		gl.glUseProgram(shaderProgram);
		gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
		
		String text = new String(this.getClass().getName() );
		
		switch(mode%6){
		case 0: 
			text += ": " + "[E]lements GL_POINTS" ;
			buffers.draw(GL3.GL_POINTS, shaderProgram);
			break;
		case 1: 
			text += ": " + "[E]lements GL_LINES" ;
			buffers.draw(GL3.GL_LINES, shaderProgram);
			break;
		case 2: 
			text += ": " + "[E]lements GL_LINE_LOOP" ;
			buffers.draw(GL3.GL_LINE_LOOP, shaderProgram);
			break;
		case 3: 
			text += ": " + "[E]lements GL_TRIANGLES" ;
			buffers.draw(GL3.GL_TRIANGLES, shaderProgram);
			break;
		case 4: 
			text += ": " + "[E]lements GL_TRIANGLE_STRIP" ;
			buffers.draw(GL3.GL_TRIANGLE_STRIP, shaderProgram);
			break;
		case 5: 
			text += ": " + "[E]lements NONE" ;
			break;
		}
		
		
		gl.glEndQuery(GL3.GL_SAMPLES_PASSED);
		gl.glGetQueryObjectiv(query[0], GL3.GL_QUERY_RESULT, result, 0);

		gl.glEndQuery(GL3.GL_PRIMITIVES_GENERATED);
		gl.glGetQueryObjectiv(query[1], GL3.GL_QUERY_RESULT, result, 1);

		gl.glEndQuery(GL3.GL_TIME_ELAPSED);
		gl.glGetQueryObjectiv(query[2], GL3.GL_QUERY_RESULT, result, 2);

		gl.glQueryCounter(query[3], GL3.GL_TIMESTAMP);
		gl.glGetQueryObjecti64v(query[3], GL3.GL_QUERY_RESULT, resultLong, 0);

		gl.glDeleteQueries(query.length, query, 0);
		
		
		
		text += ", " + "Primitives " + result[1];
		text += ", " + "Samples " + result[0];
		text += ", " + "Time stamp " +  String.format("%4.1f s", resultLong[0]/1e9);
		text += ", " + "Pass time " + String.format("%4.2f ms", result[2]/1e6);		
		
		textRenderer.drawStr2D(3, height-20, text);
		textRenderer.drawStr2D(width-90, 3, " (c) PGRF UHK");

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
		if (e.getKeyCode() == KeyEvent.VK_E)
			mode ++;
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
		GL3 gl = glDrawable.getGL().getGL3();
		gl.glDeleteProgram(shaderProgram);
	}

}