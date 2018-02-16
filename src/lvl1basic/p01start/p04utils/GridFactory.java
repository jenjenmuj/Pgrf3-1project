package lvl1basic.p01start.p04utils;

import com.jogamp.opengl.GL2GL3;
import oglutils.OGLBuffers;

class GridFactory {

    /**
     * @param gl gl context
     * @param m  počet vrcholů v řádku
     * @param n  počet vrcholů ve sloupci
     * @return OGLBuffers
     */
    static OGLBuffers generateGrid(GL2GL3 gl, int m, int n) {
        float[] vb = new float[m * n * 2];
        int index = 0;
        for (int j = 0; j < n; j++) {
            float y = j / (float) (n - 1);
            for (int i = 0; i < m; i++) {
                float x = i / (float) (m - 1);
                vb[index++] = x;
                vb[index++] = y;
            }
        }

        int[] ib = new int[(m - 1) * (n - 1) * 2 * 3];
        int index2 = 0;
        for (int r = 0; r < n - 1; r++) {
            for (int c = 0; c < m - 1; c++) {
                ib[index2++] = r * m + c;
                ib[index2++] = r * m + c + 1;
                ib[index2++] = r * m + c + m;

                ib[index2++] = r * m + c + m;
                ib[index2++] = r * m + c + 1;
                ib[index2++] = r * m + c + m + 1;
            }
        }

        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 2) // 2 floats
        };

        return new OGLBuffers(gl, vb, attributes, ib);
    }
}
