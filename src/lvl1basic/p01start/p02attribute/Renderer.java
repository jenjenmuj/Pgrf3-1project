package lvl1basic.p01start.p02attribute;

import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLUtils;

import com.jogamp.common.nio.Buffers;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * GLSL sample:<br/>
 * Adding another attribute (color) to vertices via vertex buffer<br/>
 * Requires JOGL 2.3.0 or newer
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2015-09-05
 */
public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height;

	int[] vertexBuffer = new int[1], indexBuffer = new int[1];

	int shaderProgram;

	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		OGLUtils.shaderCheck(gl);

		OGLUtils.printOGLparameters(gl);

		
		createBuffers(gl);
		createShaders(gl);
	}

	void createBuffers(GL2GL3 gl) {
		// create and fill vertex buffer data
		float[] vertexBufferData = {
			-1, -1, 	0.7f, 0, 0, 
			 1,  0,		0, 0.7f, 0,
			 0,  1,		0, 0, 0.7f 
		};
		// create buffer required for sending data to a native library
		FloatBuffer vertexBufferBuffer = Buffers
				.newDirectFloatBuffer(vertexBufferData); 

		gl.glGenBuffers(1, vertexBuffer, 0);
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vertexBuffer[0]);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, vertexBufferData.length * 4,
				vertexBufferBuffer, GL2GL3.GL_STATIC_DRAW);

		// create and fill index buffer data (element buffer in OpenGL terminology)
		short[] indexBufferData = { 0, 1, 2 };
		// create buffer required for sending data to a native library
		ShortBuffer indexBufferBuffer = Buffers
				.newDirectShortBuffer(indexBufferData);

		gl.glGenBuffers(1, indexBuffer, 0);
		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, indexBuffer[0]);
		gl.glBufferData(GL2GL3.GL_ELEMENT_ARRAY_BUFFER,
				indexBufferData.length * 2, indexBufferBuffer,
				GL2GL3.GL_STATIC_DRAW);
	}

	void createShaders(GL2GL3 gl) {
		String shaderVertSrc[] = {
			"#version 150\n",
			"in vec2 inPosition;", // input from the vertex buffer
			"in vec3 inColor;", // input from the vertex buffer
			"out vec3 vertColor;", // output from this shader to the next pipeline stage
			"void main() {",
			"	vec2 position = inPosition;",
			"   position.x += 0.1;",
			" 	gl_Position = vec4(position, 0.0, 1.0);", 
			"	vertColor = inColor;",
			"}" 
		};
		// gl_Position - built-in vertex shader output variable containing
		// vertex position before w-clipping and dehomogenization, must be
		// filled

		String shaderFragSrc[] = { 
			"#version 150\n",
			"in vec3 vertColor;", // input from the previous pipeline stage
			"out vec4 outColor;", // output from the fragment shader
			"void main() {",
			" 	outColor = vec4(vertColor, 1.0);", 
			"}" 
		};

		// vertex shader
		int vs = gl.glCreateShader(GL2GL3.GL_VERTEX_SHADER);
		gl.glShaderSource(vs, shaderVertSrc.length, shaderVertSrc,
				(int[]) null, 0);
		gl.glCompileShader(vs);
		System.out.println("Compile VS error: " + checkLogInfo(gl, vs, GL2GL3.GL_COMPILE_STATUS));

		// fragment shader
		int fs = gl.glCreateShader(GL2GL3.GL_FRAGMENT_SHADER);
		gl.glShaderSource(fs, shaderFragSrc.length, shaderFragSrc,
				(int[]) null, 0);
		gl.glCompileShader(fs);
		System.out.println("Compile FS error: " + checkLogInfo(gl, fs, GL2GL3.GL_COMPILE_STATUS));

		// link program
		shaderProgram = gl.glCreateProgram();
		gl.glAttachShader(shaderProgram, vs);
		gl.glAttachShader(shaderProgram, fs);
		gl.glLinkProgram(shaderProgram);
		System.out.println("Link error: " + checkLogInfo(gl, shaderProgram, GL2GL3.GL_LINK_STATUS));
		
		if (vs >0) gl.glDetachShader(shaderProgram, vs);
		if (fs >0) gl.glDetachShader(shaderProgram, fs);
		if (vs >0) gl.glDeleteShader(vs);
		if (fs >0) gl.glDeleteShader(fs);
	}

	void bindBuffers(GL2GL3 gl) {
		// internal OpenGL ID of a vertex shader input variable
		int locPosition = gl.glGetAttribLocation(shaderProgram, "inPosition"); 
		int locColor = gl.glGetAttribLocation(shaderProgram, "inColor");
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vertexBuffer[0]);
		gl.glEnableVertexAttribArray(locPosition);
		gl.glVertexAttribPointer(
				locPosition, // shader variable ID
				2, // number of components (coordinates, color channels,...)
				GL2GL3.GL_FLOAT, // component data type
				false, // normalize integer data to [0,1]
				20, // size of a vertex in bytes
				0); // number of bytes from vertex from vertex start to the first component
		gl.glEnableVertexAttribArray(locColor);
		gl.glVertexAttribPointer(locColor, 3, GL2GL3.GL_FLOAT, false, 20, 8);
	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		
		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

		// set the current shader to be used, could have been done only once (in
		// init) in this sample (only one shader used)
		gl.glUseProgram(shaderProgram); 
		// to use the default shader of the "fixed pipeline", call
		// gl.glUseProgram(0);

		// bind the vertex and index buffer to shader, could have been done only
		// once (in init) in this sample (only one geometry used)
		bindBuffers(gl);
		// draw
		gl.glDrawElements(GL2GL3.GL_TRIANGLES, 3, GL2GL3.GL_UNSIGNED_SHORT, 0);

	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		this.width = width;
		this.height = height;
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
		glDrawable.getGL().getGL2GL3().glDeleteProgram(shaderProgram);
	}


	static private String checkLogInfo(GL2GL3 gl, int programObject, int mode) {
		switch (mode) {
		case GL2GL3.GL_COMPILE_STATUS:
			return checkLogInfoShader(gl, programObject, mode);
		case GL2GL3.GL_LINK_STATUS:
		case GL2GL3.GL_VALIDATE_STATUS:
			return checkLogInfoProgram(gl, programObject, mode);
		default:
			return "Unsupported mode.";
		}
	}

	static private String checkLogInfoShader(GL2GL3 gl, int programObject, int mode) {
		int[] error = new int[] { -1 };
		gl.glGetShaderiv(programObject, mode, error, 0);
		if (error[0] != GL2GL3.GL_TRUE) {
			int[] len = new int[1];
			gl.glGetShaderiv(programObject, GL2GL3.GL_INFO_LOG_LENGTH, len, 0);
			if (len[0] == 0) {
				return null;
			}
			byte[] errorMessage = new byte[len[0]];
			gl.glGetShaderInfoLog(programObject, len[0], len, 0, errorMessage,
					0);
			return new String(errorMessage, 0, len[0]);
		}
		return null;
	}

	static private String checkLogInfoProgram(GL2GL3 gl, int programObject, int mode) {
		int[] error = new int[] { -1 };
		gl.glGetProgramiv(programObject, mode, error, 0);
		if (error[0] != GL2GL3.GL_TRUE) {
			int[] len = new int[1];
			gl.glGetProgramiv(programObject, GL2GL3.GL_INFO_LOG_LENGTH, len, 0);
			if (len[0] == 0) {
				return null;
			}
			byte[] errorMessage = new byte[len[0]];
			gl.glGetProgramInfoLog(programObject, len[0], len, 0, errorMessage,
					0);
			return new String(errorMessage, 0, len[0]);
		}
		return null;
	}
}