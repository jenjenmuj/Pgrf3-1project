package lvl2advanced.p05pipeline.p01geometryshader;


import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLBuffers;
import oglutils.OGLTextRenderer;
import oglutils.OGLUtils;
import oglutils.ShaderUtils;
import oglutils.ToFloatArray;
import oglutils.ToIntArray;
import transforms.Vec2D;
import transforms.Vec3D;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;

import static oglutils.ShaderUtils.GEOMETRY_SHADER_SUPPORT_VERSION;

import java.awt.event.KeyEvent;

/**
 * Ukazka pro praci s shadery v GLSL
 * pouziti geometry shaderu
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since   2015-09-06 
 */

public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height;

	OGLBuffers buffers;
	OGLTextRenderer textRenderer;
	
	int shaderProgram;

	List<Integer> indexBufferData;
	List<Vec2D> vertexBufferDataPos;
	List<Vec3D> vertexBufferDataCol;

	boolean update = true, mode = false;
	
	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		OGLUtils.shaderCheck(gl);
		if (OGLUtils.getVersionGLSL(glDrawable.getGL().getGL2GL3()) < GEOMETRY_SHADER_SUPPORT_VERSION){
			System.err.println("Geometry shader is not supported"); 
			System.exit(0);
		}

		// get and set debug version of GL class
		gl = OGLUtils.getDebugGL(gl);
		glDrawable.setGL(gl);
		
		OGLUtils.printOGLparameters(gl);

		textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());
		
		String extensions = gl.glGetString(GL2GL3.GL_EXTENSIONS);
		if (extensions.indexOf("GL_ARB_enhanced_layouts") == -1)
			shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p05pipeline/p01geometryshader/geometry_OlderSM");
		else
			shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p05pipeline/p01geometryshader/geometry");

		initBuffers();
	}
	
	void initBuffers() {
		indexBufferData = new ArrayList<>();
		vertexBufferDataPos = new ArrayList<>();
		vertexBufferDataCol = new ArrayList<>();
		
		vertexBufferDataPos.add(new Vec2D(-0.5f, 0.0f));
		vertexBufferDataPos.add(new Vec2D(0.0f, 0.5));
		vertexBufferDataPos.add(new Vec2D(0.0f, -0.5f));
		vertexBufferDataPos.add(new Vec2D(0.5f, 0.0f));
		vertexBufferDataPos.add(new Vec2D(0.7f, 0.5f));
		vertexBufferDataPos.add(new Vec2D(0.9f, -0.7f));
		
		Random r = new Random();
		for(int i = 0; i < vertexBufferDataPos.size(); i++){
			indexBufferData.add(i);
			vertexBufferDataCol.add(new Vec3D(r.nextDouble(),r.nextDouble(),r.nextDouble()));
		}
	}
	void updateBuffers(GL2GL3 gl) {
		OGLBuffers.Attrib[] attributesPos = { 
				new OGLBuffers.Attrib("inPosition", 2), };
		OGLBuffers.Attrib[] attributesCol = {
				new OGLBuffers.Attrib("inColor", 3)
		};
		
		buffers = new OGLBuffers(gl, ToFloatArray.convert(vertexBufferDataPos), attributesPos,
				ToIntArray.convert(indexBufferData));
		buffers.addVertexBuffer(ToFloatArray.convert(vertexBufferDataCol), attributesCol);

	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
	
		if (update) {
			updateBuffers(gl);
			update = false;
			System.out.println(indexBufferData.size());
		}
		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

		gl.glUseProgram(shaderProgram); 
		
		buffers.draw(GL3.GL_LINE_STRIP_ADJACENCY, shaderProgram,indexBufferData.size());
		
		String text = new String(this.getClass().getName() + ": [LBM] add point, [r]eset, [m]ode");
		
		textRenderer.drawStr2D(3, height-20, text);
		textRenderer.drawStr2D(width-90, 3, " (c) PGRF UHK");
		
		if (mode) 
			gl.getGL2GL3().glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
		else
			gl.getGL2GL3().glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
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
		double mouseX = (e.getX() / (double) width) * 2 - 1;
		double mouseY = ((height - e.getY()) / (double) height) * 2 - 1;
		indexBufferData.add(indexBufferData.size());
		vertexBufferDataPos.add(new Vec2D(mouseX, mouseY));
		vertexBufferDataCol.add(new Vec3D(mouseX / 2 + 0.5, mouseY / 2 + 0.5, 1));
		update = true;
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
		if (e.getKeyCode() == KeyEvent.VK_R){
			initBuffers();
			update = true;
		}
		if (e.getKeyCode() == KeyEvent.VK_M){
			mode = !mode;
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