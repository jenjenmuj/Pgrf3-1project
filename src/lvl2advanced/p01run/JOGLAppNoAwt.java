package lvl2advanced.p01run;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;

public class JOGLAppNoAwt { // Renderer
	private static String TITLE = "JOGL with NEWT"; // window's title
	private static final int WINDOW_WIDTH = 640; // width of the drawable
	private static final int WINDOW_HEIGHT = 480; // height of the drawable
	private static final int FPS = 60; // animator's target frames per second

	/** The entry main() method */
	public static void main(String[] args) {
		// Get the default OpenGL profile, reflecting the best for your running platform
		// GLProfile glp = GLProfile. getDefault();
		// or choose platform
		GLProfile glp = GLProfile.get(GLProfile.GL2);
		// Specifies a set of OpenGL capabilities, based on your profile.
		GLCapabilities caps = new GLCapabilities(glp);
		// Create the OpenGL rendering canvas
		GLWindow window = GLWindow.create(caps);

		// Create a animator that drives canvas' display() at the specified FPS.
		final FPSAnimator animator = new FPSAnimator(window, FPS, true);

		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDestroyNotify(WindowEvent arg0) {
				// Use a dedicate thread to run the stop() to ensure that the
				// animator stops before program exits.
				new Thread() {
					@Override
					public void run() {
						animator.stop(); // stop the animator loop
						System.exit(0);
					}
				}.start();
			};
		});

		RendererNoAwt ren = new RendererNoAwt();
		window.addGLEventListener(ren);
		window.addMouseListener(ren);
		window.addKeyListener(ren);

		window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		window.setTitle(TITLE);
		window.setVisible(true);
		animator.start();
	}
}
