package lvl2advanced.p05pipeline.p02tesselation;


import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL4;
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

import static oglutils.ShaderUtils.TESSELATION_SUPPORT_VERSION;
import static oglutils.ShaderUtils.GEOMETRY_SHADER_SUPPORT_VERSION;

import java.awt.event.KeyEvent;

/**
* Ukazka pro praci s shadery v GLSL:
* upraveno pro JOGL 2.3.0 a vyssi
* 
* @author PGRF FIM UHK
* @version 2.0
* @since   2016-09-26 
*/
public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height;

	OGLBuffers buffers;
	OGLTextRenderer textRenderer;
	
	int shaderProgram, locTime;

	float time = 1;
	
	int demoType = 0;
	boolean demoTypeChanged = true;
	boolean stop = false;
	
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
		
		if (OGLUtils.getVersionGLSL(gl) >= TESSELATION_SUPPORT_VERSION) {
			int[] maxPatchVertices = new int[1];
			gl.glGetIntegerv(GL4.GL_MAX_PATCH_VERTICES, maxPatchVertices, 0);
			System.out.println("Max supported patch vertices "	+ maxPatchVertices[0]);
		}
		
		createBuffers(gl);
		
	}
	
	void createBuffers(GL2GL3 gl) {
		int[] indexBufferData = { 0,1,2};
		
		float[] vertexBufferDataPos = {
			-0.8f, -0.9f, 
			-0.8f, 0.6F,
			0.6f, 0.8f, 
		};
		
		float[] vertexBufferDataCol = {
			0, 1, 0, 
			1, 0, 0,
			1, 1, 0,
		};
		
		OGLBuffers.Attrib[] attributesPos = {
				new OGLBuffers.Attrib("inPosition", 2),
		};
		OGLBuffers.Attrib[] attributesCol = {
				new OGLBuffers.Attrib("inColor", 3)
		};
		buffers = new OGLBuffers(gl, vertexBufferDataPos, attributesPos,
				indexBufferData);
		buffers.addVertexBuffer(vertexBufferDataCol, attributesCol);

	}

	private int init(GL2GL3 gl, int demoType){
		String extensions = OGLUtils.getExtensions(gl);
		int newShaderProgram = 0;
		switch (demoType){
		case 0: //only VS a FS
			System.out.println("Pipeline: VS + FS");
			if (extensions.indexOf("GL_ARB_enhanced_layouts") == -1)
				newShaderProgram = ShaderUtils.loadProgram(gl, 
						"/lvl2advanced/p05pipeline/p02tesselation/tessel_OlderSM_WithoutGS",
						"/lvl2advanced/p05pipeline/p02tesselation/tessel_OlderSM_WithoutGS",
						null,null,null,null); 
			else
				newShaderProgram = ShaderUtils.loadProgram(gl, 
						"/lvl2advanced/p05pipeline/p02tesselation/tessel",
						"/lvl2advanced/p05pipeline/p02tesselation/tessel",
						null,null,null,null); 
			break;
		case 1: // only VS, FS and GS
			System.out.println("Pipeline: VS + GS + FS");
			if (OGLUtils.getVersionGLSL(gl) >= GEOMETRY_SHADER_SUPPORT_VERSION) {
				if (extensions.indexOf("GL_ARB_enhanced_layouts") == -1)
					newShaderProgram = ShaderUtils.loadProgram(gl, 
							"/lvl2advanced/p05pipeline/p02tesselation/tessel_OlderSM_OnlyGS",
							"/lvl2advanced/p05pipeline/p02tesselation/tessel_OlderSM_OnlyGS", 
							"/lvl2advanced/p05pipeline/p02tesselation/tessel_OlderSM_OnlyGS", 
							null, null,null); 
				else 
					newShaderProgram = ShaderUtils.loadProgram(gl, 
							"/lvl2advanced/p05pipeline/p02tesselation/tessel", 
							"/lvl2advanced/p05pipeline/p02tesselation/tessel",
							"/lvl2advanced/p05pipeline/p02tesselation/tessel",
							null, null, null); 
			} else
				System.out.println("Geometry shader is not supported");
			break;
		case 2: //VS, FS and tess
			System.out.println("Pipeline: VS + tess + FS");
			if (OGLUtils.getVersionGLSL(gl) >= TESSELATION_SUPPORT_VERSION) {
				newShaderProgram = ShaderUtils.loadProgram(gl, 
						"/lvl2advanced/p05pipeline/p02tesselation/tessel",
						"/lvl2advanced/p05pipeline/p02tesselation/tessel",
						null,
						"/lvl2advanced/p05pipeline/p02tesselation/tessel",
						"/lvl2advanced/p05pipeline/p02tesselation/tessel",
						null); 
				}	
			else
				System.out.println("Tesselation is not supported");
			break;
		default: //VS, FS, GS and tess
			System.out.println("Pipeline: VS + tess + GS + FS");
			if (OGLUtils.getVersionGLSL(gl) >= TESSELATION_SUPPORT_VERSION) {
				newShaderProgram = ShaderUtils.loadProgram(gl, 
						"/lvl2advanced/p05pipeline/p02tesselation/tessel");
			}	
			else
				System.out.println("Tesselation is not supported");
		}
		
		return newShaderProgram;	
		
	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		
		if (demoTypeChanged) {
			int oldShaderProgram = shaderProgram;
			shaderProgram = init(gl,demoType);
			if (shaderProgram>0) {
				gl.glDeleteProgram(oldShaderProgram);
			} else {
				shaderProgram = oldShaderProgram;
			}
			locTime = gl.glGetUniformLocation(shaderProgram, "time");
			demoTypeChanged = false;
		}
		
		
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
		
		gl.glClearColor(0.2f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

		if (!stop) time *= 1.01;
		time =  time % 100;
		
		//System.out.println(time);
		
		gl.glUseProgram(shaderProgram); 
		gl.glUniform1f(locTime, time); 
		
		// vykresleni
		switch (demoType){
		case 1: //points VS+GS+FS
			if (OGLUtils.getVersionGLSL(gl) >= 300){
				buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);
			}	
			break;		
		case 2: //tessellation VS+TCS+TES+FS
		case 3: //points VS+TCS+TES+GS+FS
			if (OGLUtils.getVersionGLSL(gl) >= 400){
				gl.getGL3().glPatchParameteri(GL4.GL_PATCH_VERTICES, 3);
				buffers.draw(GL4.GL_PATCHES, shaderProgram);
			}
			break;		
		default: //triangle VS+FS
			buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram); 
			break;		
		}
		//buffers2.draw(GL3.GL_LINES_ADJACENCY, shaderProgram);
		//buffers2.draw(GL2GL3.GL_LINE_STRIP, shaderProgram);
		
		String text = new String(this.getClass().getName() + ": [m]ode ");
		
		text += String.format("%5.1f", time);
		
		if (stop) text += " [S]tart";
		else text += " [S]top";
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
		switch (e.getKeyCode()) {
		case KeyEvent.VK_M:
			demoType = (demoType+1) % 4;
			demoTypeChanged = true;
			break;
		case KeyEvent.VK_S:
			stop = !stop;
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
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		gl.glDeleteProgram(shaderProgram);
	}
}