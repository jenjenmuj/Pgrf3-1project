package lvl2advanced.p06compute.p02buffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.DebugGL4;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.*;

import static oglutils.ShaderUtils.COMPUTE_SHADER_SUPPORT_VERSION;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.nio.FloatBuffer;
import java.util.Random;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/**
 * GLSL sample:<br/>
 * Using computy shader for searching minimal key value<br/>
 * Requires JOGL 4.3.0 or newer
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2016-09-09
 */
public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height;

	OGLTextRenderer textRenderer;
	int computeShaderProgram;
	int locOffset;
	int[]  locBuffer; 
	
	//size = numberOfItems * [key(float) + paddingKey(3xfloat) + value(vec3) + paddingValue(float)]
	final int dataSize = 8*(1+3+3+1); 
	
	FloatBuffer data = FloatBuffer.allocate(dataSize);
	FloatBuffer dataOut = Buffers.newDirectFloatBuffer(dataSize);
	
	int offset = 4, compute = 0;
	
	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		OGLUtils.shaderCheck(glDrawable.getGL().getGL2GL3());
		if ((OGLUtils.getVersionGLSL(glDrawable.getGL().getGL2GL3()) < COMPUTE_SHADER_SUPPORT_VERSION)
				&& (OGLUtils.getExtensions(glDrawable.getGL().getGL2GL3()).indexOf("compute_shader") == -1)){
			System.err.println("Compute shader is not supported"); 
			System.exit(0);
		}

		glDrawable.setGL(new DebugGL4(glDrawable.getGL().getGL4()));
		GL4 gl = glDrawable.getGL().getGL4();
	
		textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());
		
		computeShaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p06compute/p02buffer/computeBuffer"); 
		
		locOffset = gl.glGetUniformLocation(computeShaderProgram, "offset");

		// buffer initialization
		data.rewind();
		Random r = new Random();
		for (int i = 0; i < dataSize; i++) {
			data.put(i, r.nextFloat());
		}	
		
		System.out.print("Input Data values ");
		for (int i = 0; i < dataSize; i++) {
			if (i % 8 == 0)
				System.out.println();
			System.out.print(String.format("%3.2f ", data.get(i)));
		}
		System.out.println();

		// declare and generate a buffer object name
		locBuffer = new int[2];
		gl.glGenBuffers(2, locBuffer, 0);
		
		// bind the buffer and define its initial storage capacity
		gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, locBuffer[0]);
		gl.glBufferData(GL4.GL_SHADER_STORAGE_BUFFER, 4 * dataSize, data, GL4.GL_DYNAMIC_DRAW);

		// bind the buffer and define its initial storage capacity
		gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, locBuffer[1]);
		gl.glBufferData(GL4.GL_SHADER_STORAGE_BUFFER, 4 * dataSize, dataOut, GL4.GL_DYNAMIC_DRAW);

		// unbind the buffer
		gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, 0);
		
		//assign the index of shader storage block to the binding point (see shader)  
		gl.glShaderStorageBlockBinding(computeShaderProgram, 0, 0); //input buffer
		gl.glShaderStorageBlockBinding(computeShaderProgram, 1, 1); //output buffer
		
		System.out.print("key values: ");
		for (int i = 0; i < dataSize; i += 8) {
			System.out.print(String.format("%4.2f ", data.get(i)));
		}
		System.out.println();
		
	}	
	
	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL4 gl = glDrawable.getGL().getGL4();
	
		if (offset>0) {
			gl.glUseProgram(computeShaderProgram);

			gl.glUniform1i(locOffset, offset);
					
			//set input and output buffer
			if (compute % 2 == 0) {
				//bind input buffer
				gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, locBuffer[0]);
		    	gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 0, locBuffer[0]);
		    	//bind output buffer
				gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, locBuffer[1]);
				gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 1, locBuffer[1]);
			}else{
				//bind input buffer
				gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, locBuffer[1]);
				gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 0, locBuffer[1]);
		    	//bind output buffer
				gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, locBuffer[0]);
				gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 1, locBuffer[0]);
			}
			
			gl.glDispatchCompute(offset, 1, 1);
			
			// make sure writing to image has finished before read
			gl.glMemoryBarrier(GL4.GL_SHADER_STORAGE_BARRIER_BIT);
			
			{// just for print after one 
				if (compute % 2 == 0) {
					gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, locBuffer[1]);
				} else {
					gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, locBuffer[0]);
				}

				gl.glGetBufferSubData(GL4.GL_SHADER_STORAGE_BUFFER, 0, 4 * dataSize, dataOut);

				System.out.print("Output data values after iteration " + (compute+1) + " offset " + offset);
				dataOut.rewind();
				for (int i = 0; i < dataSize; i++) {
					if (i % 8 == 0)
						System.out.println();
					System.out.print(String.format("%4.2f ", dataOut.get(i)));
				}
				System.out.println();

				if (offset <= 1) {
					System.out.println(String.format("minimal key value is %3.2f", dataOut.get(0)));
				}
			}
			
			compute ++;
			offset = offset/2;
			
		}
		
		gl.glUseProgram(0);
		
		gl.glClearColor(0.5f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);
		
		String text = new String(this.getClass().getName() + " nothing to render, see console output");
		
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
		GL4 gl = glDrawable.getGL().getGL4();
		gl.glDeleteProgram(computeShaderProgram);
		gl.glDeleteBuffers(2, locBuffer, 0);
		gl.glGenBuffers(2, locBuffer, 0);
	}

}