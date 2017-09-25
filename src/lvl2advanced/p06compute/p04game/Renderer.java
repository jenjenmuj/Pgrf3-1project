package lvl2advanced.p06compute.p04game;

import com.jogamp.opengl.DebugGL4;
import com.jogamp.opengl.GL4;

import static com.jogamp.opengl.GL4.*;
import static oglutils.ShaderUtils.COMPUTE_SHADER_SUPPORT_VERSION;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
//import com.jogamp.opengl.util.texture.Texture;

import oglutils.*;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/**
* GLSL sample:<br/>
* Ukazka pro praci s compute shader v GLSL:
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
	int computeShaderProgram;

	OGLTexture2D texture;
	OGLTexture2D textureOut;
	OGLTexture2D textureIn;
	OGLTexture.Viewer textureViewer;
	
	boolean compute = true;
	boolean continues = false;
	boolean init = true;
	int mouseX, mouseY;
	boolean mouseDown = false;
	
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
		
		//get limits of work group size per dimension
		int[] val = new int[1];
		for (int dim = 0; dim < 3; dim++) {
			gl.glGetIntegeri_v(GL4.GL_MAX_COMPUTE_WORK_GROUP_SIZE, dim, val, 0);
			System.out.println("GL_MAX_COMPUTE_WORK_GROUP_SIZE [" + dim + "] : " + val[0]);
		}
		
		computeShaderProgram = ShaderUtils.loadProgram(gl, null, null, null, null, null, 
				"/lvl2advanced/p06compute/p04game/computeLife"); 
	}	

	void initTexture(GL4 gl) {
		// load test texture
		texture = new OGLTexture2D(gl, "/textures/mosaic.jpg");
		textureViewer = new OGLTexture2D.Viewer(gl);

		// create image as a copy of loaded texture, must have 4 components
		OGLTexImageFloat texImageIn = texture.getTexImage(new OGLTexImageFloat.Format(4));
		// create input texture from the image
		textureIn = new OGLTexture2D(gl, texImageIn);

		// create empty image with size same as loaded texture
		OGLTexImageFloat texImageOut = new OGLTexImageFloat(texture.getWidth(),
				texture.getHeight(), 1, new OGLTexImageFloat.Format(4));
		// create (empty) output texture from the image
		textureOut = new OGLTexture2D(gl, texImageOut);
	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL4 gl = glDrawable.getGL().getGL4();
		
		if (init) {
			init = false;
			initTexture(gl);
		}
		
		int w = texture.getWidth();
		int h = texture.getHeight();
		
		gl.glClearColor(0.3f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);
		
		// recompute? 
		if (compute || continues) {
			compute = false;
			
			gl.glBindImageTexture(0, textureIn.getTextureId(), 0, false, 0, 
					GL4.GL_READ_ONLY, GL4.GL_RGBA32F);
			gl.glBindImageTexture(1, textureOut.getTextureId(), 0, false, 0, 
					GL4.GL_WRITE_ONLY, GL4.GL_RGBA32F);

			// first step
			gl.glUseProgram(computeShaderProgram);
			gl.glDispatchCompute(w/16, h/16, 1);

			gl.glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);


			//change textures 
			gl.glBindImageTexture(0, textureOut.getTextureId(), 0, false, 0, GL4.GL_READ_ONLY,
					GL4.GL_RGBA32F);
			gl.glBindImageTexture(1, textureIn.getTextureId(), 0, false, 0, GL4.GL_WRITE_ONLY,
					GL4.GL_RGBA32F);
			
			//second step
			gl.glUseProgram(computeShaderProgram);
			gl.glDispatchCompute(w/16, h/16, 1);
			gl.glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
		}
		
		if (mouseDown) { //add new generator
			// get image as a copy of input texture
			OGLTexImageFloat texImageIn = textureIn.getTexImage(new OGLTexImageFloat.Format(4));
			int x = 2 * mouseX * texImageIn.getWidth() / width;
			int y = 2 * mouseY * texImageIn.getHeight() / height;
			if (x > 0 && x <texImageIn.getWidth()-1 && 
				y > 0 && y <texImageIn.getHeight()-1){
		/*   //cross shape		
		  		texImageIn.setPixel(x, y, 0, 1.0f); //only red color
				texImageIn.setPixel(x+1, y, 0, 1.0f); //only red color
				texImageIn.setPixel(x, y+1, 0, 1.0f); //only red color
				texImageIn.setPixel(x-1, y, 0, 1.0f); //only red color
				texImageIn.setPixel(x, y-1, 0, 1.0f); //only red color
		*/
			//glider shape
				texImageIn.setPixel(x-1, y, 0, 1.0f); //only red color
				texImageIn.setPixel(x+1, y, 0, 1.0f); //only red color
				texImageIn.setPixel(x+1, y+1, 0, 1.0f); //only red color
				texImageIn.setPixel(x, y+1, 0, 1.0f); //only red color
				texImageIn.setPixel(x+1, y-1, 0, 1.0f); //only red color
			}
			// update input texture from the image
			textureIn.setTexImage(texImageIn);
		}
		
		//draw textures

		//show original texture in right up corner
		textureViewer.view(texture,0,0);
		//show input texture in left up corner
		textureViewer.view(textureIn,-1,-1);
		//show output texture in right down corner
		textureViewer.view(textureOut, 0, -1);
		
		
		String text = new String(this.getClass().getName() + ": [LMB] new life, [n] -start/stop, [m] - step, [i] - reset, ESC - exit ");
		
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
		mouseX = e.getX();
		mouseY = (height - e.getY());
		mouseDown = true;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mouseDown = false;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		mouseX = e.getX();
		mouseY = (height - e.getY());
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE){
			System.exit(0);
		}
		if (e.getKeyCode() == KeyEvent.VK_M){
			compute = true;
		}
		if (e.getKeyCode() == KeyEvent.VK_N){
			continues = !continues;
		}
		if (e.getKeyCode() == KeyEvent.VK_I){
			init = true;
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
		GL4 gl = glDrawable.getGL().getGL4();
		gl.glDeleteProgram(computeShaderProgram);
	}

}