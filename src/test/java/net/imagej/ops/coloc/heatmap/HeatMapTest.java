
package net.imagej.ops.coloc.heatmap;

import net.imagej.ops.Op;
import net.imagej.ops.Ops;
import net.imagej.ops.coloc.ColocalisationTest;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;

import org.junit.Test;

/**
 * Tests {@link DefaultHeatMap}.
 *
 * @author Curtis Rueden
 * @author Ellen T Arena
 */
public class HeatMapTest extends ColocalisationTest {

	@Test
	public void testHeatMap() {
		final Img<UnsignedByteType> image1 = ArrayImgs.unsignedBytes(new byte[] { //
			1, 2, 3, 4, 5, 6, //
			7, 8, 9, 10, 11, 12, //
			13, 14, 0, 16, 17, 18, //
			19, 20, 21, 22, 23, 24, //
			25, 26, 27, 28, 29, 30, //
			31, 32, 33, 34, 35, 36 //
		}, 6, 6);
		final Img<UnsignedByteType> image2 = ArrayImgs.unsignedBytes(new byte[] { //
			1, 2, 3, 4, 5, 6, //
			7, 8, 9, 10, 11, 12, //
			13, 14, 15, 16, 17, 18, //
			19, 20, 21, 22, 23, 24, //
			25, 26, 27, 28, 29, 30, //
			31, 32, 33, 34, 35, 36 //
		}, 6, 6);
		final Op colocOp = ops.op(Ops.Coloc.ICQ.class, image1, image2);
		final Img<DoubleType> heatmap = ArrayImgs.doubles(6, 6);
		ops.run(DefaultHeatMap.class, heatmap, image1, image2, 1, colocOp);
		System.out.println(heatmap);
		int count = 0;
		for (final DoubleType v : heatmap) {
			if (count++ % 6 == 0) System.out.println();
			System.out.print(v + " ");
		}
		System.out.println();
	}

}
