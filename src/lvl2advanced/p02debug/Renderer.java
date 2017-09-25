package lvl2advanced.p02debug;


import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.TraceGL2;

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
 * Debugging tools sample, base on sample glsl01_start_4 
 * Requires JOGL 2.3.0 or newer
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2015-09-05
 */
public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height;

	OGLBuffers buffers;
	OGLTextRenderer textRenderer;

	int shaderProgram, locTime;

	float time = 0;

	long oldmils;
	double fps = 0;

	enum DEBUGMODE {
		DEBUG, //using DebugGL class - generate exception after any glError
		TRACE, //using TraceGL class - print names of called GL methods 
		NONE,  //no special debug mode
		INDIVIDUAL // manually checking glError after calling GL method 
		};
	DEBUGMODE debugMode = DEBUGMODE.NONE;
	
	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		OGLUtils.shaderCheck(glDrawable.getGL().getGL2GL3());
		
		// get context of GL
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		
		switch (debugMode) { 
		case DEBUG: 
			//generate GLEception immediately after GLError
			gl = new DebugGL2(glDrawable.getGL().getGL2());
			//or gl = OGLUtils.getDebugGL(gl);
			
			// set new context with DebugGL
			glDrawable.setGL(gl);
			break;
		case TRACE:
			gl = new TraceGL2(glDrawable.getGL().getGL2(), System.err); 
			glDrawable.setGL(gl);
			break;
		default:
		}	
		
		System.out.println(glDrawable.getClass().getName());
		System.out.println(glDrawable.getGL().getClass().getName());
		System.out.println(GLProfile.glAvailabilityToString());
		System.out.println(GLProfile.getMaxProgrammable(true));
		
		System.out.println("\nGL_PROFILE_LIST_ALL:");
		for (String s : GLProfile.GL_PROFILE_LIST_ALL) {
			System.out.print(s + " " + GLProfile.isAvailable(s) + ", ");
		}
		
		System.out.println("\nGL_PROFILE_LIST_MAX_PROGSHADER:");
		for (String s : GLProfile.GL_PROFILE_LIST_MAX_PROGSHADER) {
			System.out.print(s + " " + GLProfile.isAvailable(s) + ", ");
		}

		System.out.println("\nGL_PROFILE_LIST_MAX:");
		for (String s : GLProfile.GL_PROFILE_LIST_MAX) {
			System.out.print(s + " " + GLProfile.isAvailable(s) + ", ");
		}
		System.out.println();
		
		//jogl parameters
		OGLUtils.printJOGLparameters();
		System.out.println();
		
		//java parameters
		OGLUtils.printJAVAparameters();
		System.out.println();
		
		//opengl parameters
		OGLUtils.printOGLparameters(gl);
		System.out.println();
		
		textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());
		
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p02debug/start");
		
		//sample shader files with many errors - try to find and correct them
		//shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p02debug/startError"); 
		
		createBuffers(gl);

		locTime = gl.glGetUniformLocation(shaderProgram, "time");
	
		//ERROR - wrong constant of setting depth test, correct should be set by gl.glEnable(GL2GL3.GL_DEPTH_TEST);
		gl.glEnable(GL2GL3.GL_DEPTH); 
		
		//checking GLErrors at the end of initialization, useful to leave it here   
		if (debugMode == DEBUGMODE.INDIVIDUAL)
		OGLUtils.checkGLError(gl,"at the end of init: " + this.getClass().getName() + "." +
				Thread.currentThread().getStackTrace()[1].getMethodName(), true);
		
	}

	void createBuffers(GL2GL3 gl) {
		float[] vertexBufferData = {
			-1, -1, 	0.7f, 0, 0, 
			 1,  0,		0, 0.7f, 0,
			 0,  1,		0, 0, 0.7f 
		};
		int[] indexBufferData = { 0, 1, 2 };

		// vertex binding description, concise version
		OGLBuffers.Attrib[] attributes = {
				new OGLBuffers.Attrib("inPosition", 2), // 2 floats
				new OGLBuffers.Attrib("inColor", 3) // 3 floats
		};
		buffers = new OGLBuffers(gl, vertexBufferData, attributes,
				indexBufferData);
		// the concise version requires attributes to be in this order within
		// vertex and to be exactly all floats within vertex

/*		full version for the case that some floats of the vertex are to be ignored 
 * 		(in this case it is equivalent to the concise version): 
 		OGLBuffers.Attrib[] attributes = {
				new OGLBuffers.Attrib("inPosition", 2, 0), // 2 floats, at 0 floats from vertex start
				new OGLBuffers.Attrib("inColor", 3, 2) }; // 3 floats, at 2 floats from vertex start
		buffers = new OGLBuffers(gl, vertexBufferData, 5, // 5 floats altogether in a vertex
				attributes, indexBufferData); 
*/
	}

	
	@Override
	public void display(GLAutoDrawable glDrawable) {
		//frame per second calculation
		long mils = System.currentTimeMillis();
		if ((mils - oldmils)>0){
			fps = 1000 / (double)(mils - oldmils + 1);
			oldmils=mils;
		}
		System.out.println("Display method call, FPS = " + String.format("%3.1f", fps));
		
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);
		
		time += 0.1;
		
		//ERROR - wrong setting uniform variable, correct shader must be set before this
		gl.glUniform1f(locTime, time); 
		//checking GLErrors 
		if (debugMode == DEBUGMODE.INDIVIDUAL)
			OGLUtils.checkGLError(gl,"after setting uniform variable: " + this.getClass().getName() + "." +
						Thread.currentThread().getStackTrace()[1].getMethodName(), true);
				
		
		//ERROR - wrong id of shader program 
		//gl.glUseProgram(2); 
		
		//checking GLErrors 
		if (debugMode == DEBUGMODE.INDIVIDUAL)
			OGLUtils.checkGLError(gl,"after setting shader program: " + this.getClass().getName() + "." +
						Thread.currentThread().getStackTrace()[1].getMethodName(), true);
		gl.glUseProgram(shaderProgram); 
		
		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);
		
		time += 0.1;
		gl.glUniform1f(locTime, time); 
		
		//ERROR - wrong constant in setting polygonMode, correct should be set by gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
		//gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_LINES);
		//checking GLErrors 
		if (debugMode == DEBUGMODE.INDIVIDUAL)
			OGLUtils.checkGLError(gl,"after setting polygon mode: " + this.getClass().getName() + "." +
				Thread.currentThread().getStackTrace()[1].getMethodName(), true);
		
		// bind and draw
		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);
		
		
		gl.glUseProgram(0); 
		String text = new String(this.getClass().getName());
		textRenderer.drawStr2D(3, height - 20, text);
		textRenderer.drawStr2D(width - 90, 3, " (c) PGRF UHK");

		//checking GLErrors at the end of display method, 
		//useful to check at least one per frame, 
		//leave it here, end of display method  
		if (debugMode == DEBUGMODE.INDIVIDUAL)
			OGLUtils.checkGLError(gl,"at the end of display: " + this.getClass().getName() + "." +
				Thread.currentThread().getStackTrace()[1].getMethodName(), true);
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
	}

}