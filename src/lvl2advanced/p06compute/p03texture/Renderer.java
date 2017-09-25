package lvl2advanced.p06compute.p03texture;

import com.jogamp.opengl.DebugGL4;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.*;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;

import static oglutils.ShaderUtils.COMPUTE_SHADER_SUPPORT_VERSION;

import java.awt.event.KeyEvent;

/**
* GLSL sample:<br/>
* In compute shader: load two textures to compute shader, fill output image<br/>
* In fragment shader: load image and map it as a texture to surface<br/>
* Requires OpenGL 4.3.0 or newer
* Requires JOGL 2.3.0 or newer
*  
* @author PGRF FIM UHK
* @version 2.0
* @since   2016-09-09 
*/
public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height;

	OGLBuffers buffers;
	OGLTextRenderer textRenderer;
	int shaderProgram, locMode, computeShaderProgram;

	OGLTexture2D texture1, texture2;
	OGLTexture2D.Viewer textureViewer;
	int mode = 0;
	int[] tex_output;
	int tex_w = 256, tex_h = 256;

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
		
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p06compute/p03texture/drawImage"); 
		computeShaderProgram = ShaderUtils.loadProgram(gl, null, null, null, null, null, "/lvl2advanced/p06compute/p03texture/computeTexture"); 
		
		createBuffers(gl);

		locMode = gl.glGetUniformLocation(computeShaderProgram, "mode");
		
		tex_output = new int[1];
		gl.glGenTextures(1, tex_output, 0);
		gl.glBindTexture(GL4.GL_TEXTURE_2D, tex_output[0]);
		gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA32F, tex_w,
				tex_h, 0, GL4.GL_RGBA, GL4.GL_FLOAT, null);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_LINEAR);
		
			
		//Limits on work group size per dimension
		int[] val = new int[1];
		for (int dim = 0; dim < 3; dim++) {
			gl.glGetIntegeri_v(GL4.GL_MAX_COMPUTE_WORK_GROUP_SIZE, dim, val, 0);
			System.out.println("GL_MAX_COMPUTE_WORK_GROUP_SIZE [" + dim + "] : " + val[0]);
		}
		
		texture1 = new OGLTexture2D(gl, "/textures/bricks.jpg");
		texture2 = new OGLTexture2D(gl, "/textures/bricksn.png");
		textureViewer = new OGLTexture2D.Viewer(gl);


	}	
	void createBuffers(GL4 gl) {
		float[] vertexBufferData = {
			-1, -1, 	0.7f, 0, 0, 	0.0f, 0.0f,
			 1,  0,		0, 0.7f, 0,		0, 1,
			 0,  1,		0, 0, 0.7f, 	1, 0
		};
		int[] indexBufferData = { 0, 1, 2 };

		OGLBuffers.Attrib[] attributes = {
				new OGLBuffers.Attrib("inPosition", 2, 0), 
				new OGLBuffers.Attrib("inColor", 3, 2),  
				new OGLBuffers.Attrib("inTexCoord", 2, 5) }; 
		buffers = new OGLBuffers(gl, vertexBufferData, 7, 
				attributes, indexBufferData); 

	}

	
	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL4 gl = glDrawable.getGL().getGL4();
		
		gl.glBindTexture(GL4.GL_TEXTURE_2D, tex_output[0]);
		gl.glBindImageTexture (0, tex_output[0], 0, false, 0, GL4.GL_WRITE_ONLY, GL4.GL_RGBA32F);
		gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA32F, tex_w,
				tex_h, 0, GL4.GL_RGBA, GL4.GL_FLOAT, null);
						
		//fill texture in compute shader
		gl.glUseProgram(computeShaderProgram);
		
		texture1.bind(computeShaderProgram, "texture1", 0);
		texture2.bind(computeShaderProgram, "texture2", 1);
		
		gl.glUniform1i(locMode, mode%3); 
		
		gl.glDispatchCompute(256/32, 256/32, 1);
		
		// make sure writing to image has finished before read
		gl.glMemoryBarrier(GL4.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
		
		//draw result texture
		gl.glUseProgram(shaderProgram); 
				
		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);
		
		gl.glBindTexture(GL4.GL_TEXTURE_2D, tex_output[0]);
		gl.glBindImageTexture (0, tex_output[0], 0, false, 0, GL4.GL_READ_ONLY, GL4.GL_RGBA32F);

		// draw
		buffers.draw(GL4.GL_TRIANGLES, shaderProgram);
		
		textureViewer.view(texture1,-1,0.5,0.5);
		textureViewer.view(texture2,-1,0.0,0.5);
		
		String text = new String(this.getClass().getName() + " [m]ode: " + mode);
		
		textRenderer.drawStr2D(3, height - 20, text);
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
		if (e.getKeyCode() == KeyEvent.VK_M){
			mode++;
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
		GL4 gl = glDrawable.getGL().getGL4();
		gl.glDeleteProgram(shaderProgram);
		gl.glDeleteTextures(1, tex_output, 0);
	}

}