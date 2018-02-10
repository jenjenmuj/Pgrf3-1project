package lvl2advanced.p07feedback.p01vs;

import com.jogamp.common.nio.Buffers;

import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLBuffers;
import oglutils.OGLTextRenderer;
import oglutils.OGLUtils;
import oglutils.ShaderUtils;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.nio.FloatBuffer;

import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;

import java.awt.event.KeyEvent;

/**
 * GLSL sample:<br/>
 * Transform feedback sample, elements generated in vertex shader are used in next pass  
 * Requires JOGL 2.3.0 or newer
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2016-11-11
 */

public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height;
	float time = 0;

	OGLBuffers buffers;
	OGLTextRenderer textRenderer;
	
	int shaderProgram, shaderProgramPre;
	int[] buffer_name;
	boolean compute = true;
	int[] query, result;
	
	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		OGLUtils.shaderCheck(gl);
						
		if (OGLUtils.getVersionGLSL(gl) < 400){
			System.err.println("Transform feedback is not supported"); 
			System.exit(0);
		}
				
		gl = OGLUtils.getDebugGL(gl);
		glDrawable.setGL(gl);
		
		textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());
		
		if (OGLUtils.getVersionGLSL(gl) < 450)
			shaderProgramPre = ShaderUtils.loadProgram(gl, "/lvl2advanced/p07feedback/p01vs/feedbackVS");
		else
			shaderProgramPre = ShaderUtils.loadProgram(gl, "/lvl2advanced/p07feedback/p01vs/feedbackVSLayout");
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p07feedback/p01vs/feedbackDraw");
		
		String[] name = {"outData"};
		gl.glTransformFeedbackVaryings( shaderProgramPre, 1, name, GL2GL3.GL_INTERLEAVED_ATTRIBS);
		
		initTransformFeedback(gl);
		createInputBuffer(gl);		
	}
	
	private void createInputBuffer(GL2GL3 gl) {
		int[] indexBufferData = { 0, 1, 2, 3 };
		
		float[] vertexBufferDataPos1 = {
				-.5f, -.1f,  0.0f, 1.0f, 0.1f,
				-.3f, .5f,  0.0f, 1.0f, 1.0f,
				.2f, -.4f,  0.0f, 0.5f, 0.5f,
				.3f, .8f,  0.0f, 0.1f, 1.0f, 
					};
			
			OGLBuffers.Attrib[] attributesPos = {
				new OGLBuffers.Attrib("inPosition", 2),
				new OGLBuffers.Attrib("inColor", 3),
		};
		buffers = new OGLBuffers(gl, vertexBufferDataPos1, attributesPos,
				indexBufferData);
	}
	
	private void initTransformFeedback(GL2GL3 gl) {		
		buffer_name = new int[1];
	
		gl.glGenBuffers(1, buffer_name, 0);
		
		// Create transform feedback output buffer
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, buffer_name[0]);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 4*4*(2 + 3), null, GL2GL3.GL_DYNAMIC_DRAW);
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);
		
        query = new int[1];
        result = new int[1];
    	gl.glGenQueries(1, query, 0);
	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

		if (compute) {
			compute = false;
		
			gl.glUseProgram(shaderProgramPre);

			gl.glEnable(GL2GL3.GL_RASTERIZER_DISCARD);

			gl.glBindBufferBase(GL2GL3.GL_TRANSFORM_FEEDBACK_BUFFER, 0, buffer_name[0]);

			gl.glBeginTransformFeedback(GL2GL3.GL_POINTS);

			// Query- how many elements were transformed
			gl.glBeginQuery(GL2GL3.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN, query[0]);

			// Draw buffer to get transform feedback
			buffers.draw(GL2GL3.GL_POINTS, shaderProgramPre);

			// Query how many elements were transformed
			gl.glEndQuery(GL2GL3.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN);
			gl.glGetQueryObjectiv(query[0], GL2GL3.GL_QUERY_RESULT, result, 0);
			System.out.println("GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN: " + result[0]);

			gl.glEndTransformFeedback();
			// Unbind the transform feedback for safety
			gl.glBindBufferBase(GL2GL3.GL_TRANSFORM_FEEDBACK_BUFFER, 0, 0);

			// Disable the rasterizer discard, because we need rasterization in drawing.
			gl.glDisable(GL2GL3.GL_RASTERIZER_DISCARD);

			gl.glFlush();

			// Print transform feedback buffer to check
			FloatBuffer f = Buffers.newDirectFloatBuffer(4 * (2 + 3));
			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, buffer_name[0]);
			gl.glGetBufferSubData(GL2GL3.GL_ARRAY_BUFFER, 0, f.limit() * 4, f);

			for (int i = 0; i < f.limit(); i += 5) {
				System.out.println(" XY:" + f.get(i) + ", " + f.get(i + 1) + " RGB:" + f.get(i + 2) + "," + f.get(i + 3)
						+ "," + f.get(i + 4));
			}

		}

		// drawing pipeline
		gl.glUseProgram(shaderProgram);

		// draw original buffer
		gl.glPointSize(5f);
		buffers.draw(GL2GL3.GL_POINTS, shaderProgram);

		// draw date get from transform feedback
		gl.glPointSize(10f);
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, buffer_name[0]);
		gl.glEnableVertexAttribArray(0); // attribute inPossition
		// first attribute, size=2 floats, stride=20 bytes, offset=0 bytes
		gl.glVertexAttribPointer(0, 2, GL2GL3.GL_FLOAT, false, 20, 0); 
		
		gl.glEnableVertexAttribArray(1); // attribute inColor
		// second attribute, size=2 floats, stride=20 bytes, offset=8 bytes
		gl.glVertexAttribPointer(1, 3, GL2GL3.GL_FLOAT, false, 20, 8);
		
		gl.glDrawArrays(GL2GL3.GL_POINTS, 0, 4); // number of elements

		gl.glDisableVertexAttribArray(0);
		gl.glDisableVertexAttribArray(1);
		
		
		String text = new String(this.getClass().getName() + " transform feedback, see console output");
		
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
		gl.glDeleteProgram(shaderProgramPre);
		gl.glDeleteBuffers(1, buffer_name, 0);
		gl.glDeleteQueries(1, query, 0);
	}

}