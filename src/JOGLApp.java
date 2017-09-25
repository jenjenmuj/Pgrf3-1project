import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingUtilities;

public class JOGLApp {
	private static final int FPS = 60; // animator's target frames per second

	private GLCanvas canvas = null;

	private Frame testFrame;
	private int demoId = 1;
	static String[] names = { "lvl1basic.p00.p01withoutShaders", "lvl1basic.p01start.p01buffer",
			"lvl1basic.p01start.p02attribute", "lvl1basic.p01start.p03uniform", "lvl1basic.p01start.p04utils",
			"lvl1basic.p01start.p05multiple", "lvl1basic.p01start.p06depthbuffer", "lvl1basic.p02geometry.p01cube",
			"lvl1basic.p02geometry.p02strip", "lvl1basic.p02geometry.p03obj", "lvl1basic.p03texture.p01intro",
			"lvl1basic.p03texture.p02utils", "lvl1basic.p04target.p01intro", "lvl1basic.p04target.p02utils",
			"lvl1basic.p04target.p03postproces", "lvl2advanced.p01run", "lvl2advanced.p02debug",
			"lvl2advanced.p03texture.p01quad", "lvl2advanced.p03texture.p02cubetexture",
			"lvl2advanced.p03texture.p03volume", "lvl2advanced.p03texture.p04filtering",
			"lvl2advanced.p04target.p01save", "lvl2advanced.p04target.p02draw",
			"lvl2advanced.p04target.p03bufferedimage", "lvl2advanced.p04target.p04multiple",
			"lvl2advanced.p04target.p05gpgpu", "lvl2advanced.p05pipeline.p01geometryshader",
			"lvl2advanced.p05pipeline.p02tesselation", "lvl2advanced.p05pipeline.p03query",
			"lvl2advanced.p06compute.p01intro", "lvl2advanced.p06compute.p02buffer",
			"lvl2advanced.p06compute.p03texture", "lvl2advanced.p06compute.p04game", "lvl2advanced.p07feedback.p01vs",
			"lvl2advanced.p07feedback.p02gs", };

	private KeyAdapter keyAdapter;

	public void start() {
		try {
			testFrame = new Frame("TestFrame");
			testFrame.setSize(512, 384);

			makeGUI(testFrame);

			setApp(testFrame, 1);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void makeGUI(Frame testFrame) {
		ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				demoId = Integer
						.valueOf(ae.getActionCommand().substring(0, ae.getActionCommand().lastIndexOf('-') - 1).trim());
				setApp(testFrame, demoId);
			}
		};

		MenuBar menuBar = new MenuBar();
		Menu menu = new Menu("Menu");
		MenuItem m;
		for (int i = 0; i < names.length; i++) {
			m = new MenuItem(new Integer(i + 1).toString() + " - " + names[i]);
			m.addActionListener(actionListener);
			menu.add(m);
		}

		keyAdapter = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_HOME:
					demoId = 1;
					setApp(testFrame, demoId);

					break;
				case KeyEvent.VK_END:
					demoId = names.length;
					setApp(testFrame, demoId);
					break;
				case KeyEvent.VK_LEFT:
					if (demoId > 1)
						demoId--;
					setApp(testFrame, demoId);
					break;
				case KeyEvent.VK_RIGHT:
					if (demoId < names.length)
						demoId++;
					setApp(testFrame, demoId);
					break;
				}
			}

		};

		menuBar.add(menu);
		testFrame.setMenuBar(menuBar);
	}

	private void setApp(Frame testFrame, int type) {
		Dimension dim;
		if (canvas != null){
			testFrame.remove(canvas);
			dim = canvas.getSize();
		} else {
			dim = new Dimension(600, 400);
		}
			
		// setup OpenGL version
		GLProfile profile = GLProfile.getMaximum(true);
		GLCapabilities capabilities = new GLCapabilities(profile);
		capabilities.setRedBits(8);
		capabilities.setBlueBits(8);
		capabilities.setGreenBits(8);
		capabilities.setAlphaBits(8);
		capabilities.setDepthBits(24);

		canvas = new GLCanvas(capabilities);
		canvas.setSize(dim);
		testFrame.add(canvas);

		Object ren = null;
		Class<?> renClass;
		try {
			renClass = Class.forName(names[type - 1] + ".Renderer");
			ren = renClass.newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e1) {
			e1.printStackTrace();
		}
		;

		canvas.addGLEventListener((GLEventListener) ren);
		canvas.addKeyListener((KeyListener) ren);
		canvas.addKeyListener(keyAdapter);
		canvas.addMouseListener((MouseListener) ren);
		canvas.addMouseMotionListener((MouseMotionListener) ren);
		canvas.requestFocus();

		final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);
		testFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				new Thread() {
					@Override
					public void run() {
						if (animator.isStarted())
							animator.stop();
						System.exit(0);
					}
				}.start();
			}
		});
		
		testFrame.setTitle(ren.getClass().getName());

		testFrame.pack();
		testFrame.setVisible(true);
		animator.start(); // start the animation loop
}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new JOGLApp().start());
	}

}