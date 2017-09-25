package lvl2advanced.p04target.p05gpgpu;


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

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.Random;

/**
* Ukazka pro praci s shadery v GLSL:
* vytvoreni pole dat, naplneni, vytvoreni textury, mapovani, rendrovani do textury, a porad dokola
* upraveno pro JOGL 2.3.0 a vyssi
* 
* @author PGRF FIM UHK
* @version 2.0
* @since   2015-11-24 
*/

public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height, ox, oy;

	OGLBuffers buffers;
	OGLTextRenderer textRenderer;

	int shaderProgram;

	OGLTexture2D dataTexture;
	
	OGLRenderTarget renderTarget;
	OGLRenderTarget renderTarget2;
	OGLRenderTarget renderTargetHlp;
	
	boolean poprve = true, init = true;
	OGLTexImageFloat dataTexImage = null;
	int dataWidth = 512, dataHeight = 512;
	
	Random random = new Random();
	
	OGLTexture2D.Viewer textureViewer;
	
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		OGLUtils.shaderCheck(gl);

		// get and set debug version of GL class
		gl = OGLUtils.getDebugGL(gl);
		glDrawable.setGL(gl);
		
		OGLUtils.printOGLparameters(gl);
		textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());
		
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p04target/p05gpgpu/gpgpuRoll");
		//shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p04target/p05gpgpu/gpgpuMax");
		
		createBuffers(gl);
		initData(gl);
					
		//dva renderTargets, z jednoho se bude cist a do druheho rendrovat
		renderTarget = new OGLRenderTarget(gl, dataWidth, dataHeight);
		renderTarget2 = new OGLRenderTarget(gl, dataWidth, dataHeight);

		gl.glDisable(GL2GL3.GL_DEPTH_TEST);
		textureViewer = new OGLTexture2D.Viewer(gl);
	
	}

	void initData(GL2GL3 gl) {
		// vytvorime pole hodnot
		dataTexImage = new OGLTexImageFloat(dataWidth, dataHeight, 4);
		for (int i = 0; i < dataHeight; i++){
			for (int j = 0; j < dataWidth; j++) {
				dataTexImage.setPixel(j, i, 0, random.nextFloat()*random.nextFloat());
			}
			dataTexImage.setPixel(i, i, 0, 1.0f);
		}	
		// vytvorime texturu
		dataTexture = new OGLTexture2D(gl, dataTexImage);
	}

	void createBuffers(GL2GL3 gl) {
		// full-screen quad, just NDC positions are needed, texturing
		// coordinates can be calculated from them
		float[] triangleStrip = { 1, -1, 
								1, 1, 
								-1, -1, 
								-1, 1 };
				
		OGLBuffers.Attrib[] attributesStrip = {
				new OGLBuffers.Attrib("inPosition", 2)};

		buffers = new OGLBuffers(gl, triangleStrip, attributesStrip, null);

	}

	public void display(GLAutoDrawable glDrawable) {
		
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		// rendrujeme do renderTarget, ne na obrazovku
		renderTarget.bind();

		gl.glUseProgram(shaderProgram);

		
		//gl.glClearColor(0.5f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT );
		
		if (init) {
			init = false;
			initData(gl);
			dataTexture.bind(shaderProgram, "textureID", 0);
		}
		else{
			renderTarget2.getColorTexture().bind(shaderProgram, "textureID", 0);
		}			
		
		buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram);
		

		
		//ziskani textury, neni treba, pouzijeme primo renderTarget 
		//texture = new OGLTexture(gl,renderTarget.getColorTexture().getTexImage(new OGLTexImageByte.Format(4)));
		
		//vysledek predchoziho renderu pouzijeme jako texturu
		renderTarget.getColorTexture().bind(shaderProgram, "textureID", 0);
		
		
		// nastavime vychozi render target - kreslime do obrazovky
		gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, width, height);

		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT);

		buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram);

		//prehozeni renderTargetu
		renderTargetHlp = renderTarget2;
		renderTarget2 = renderTarget;
		renderTarget = renderTargetHlp;
		
		//rendrujeme bez shaderu pro zobrazeni textury
		//puvodni textura
		textureViewer.view(dataTexture, -1, -1, 0.5, height / (double) width);
		//nova textura
		textureViewer.view(renderTarget.getColorTexture(), -1, -0.5, 0.5, height / (double) width);

		String text = new String(this.getClass().getName() + ": [I]nit");
		
		textRenderer.drawStr2D(3, height-20, text);
		textRenderer.drawStr2D(width-90, 3, " (c) PGRF UHK");
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		this.width = width;
		this.height = height;
		textRenderer.updateSize(width, height);
	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
			boolean deviceChanged) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		ox = e.getX();
		oy = e.getY();
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_I:
			init = true;;
			break;
	
		}
	}
	
	public void keyTyped(KeyEvent e) {
	}

	public void dispose(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		gl.glDeleteProgram(shaderProgram);
	}
}