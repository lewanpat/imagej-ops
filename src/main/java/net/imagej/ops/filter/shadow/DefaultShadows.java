
package net.imagej.ops.filter.shadow;

import java.util.Arrays;

import net.imagej.Extents;
import net.imagej.Position;
import net.imagej.ops.Ops;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleNeighborhood;
import net.imglib2.algorithm.neighborhood.RectangleNeighborhoodFactory;
import net.imglib2.algorithm.neighborhood.RectangleShape.NeighborhoodsAccessible;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Ops.Filter.Shadows.class)
public class DefaultShadows<T extends RealType<T>> extends
	AbstractUnaryFunctionOp<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>>
	implements Ops.Filter.Shadows
{

	static final double sqrt2 = Math.sqrt(2);
	final double[] r = { sqrt2, 1, sqrt2, 1, 0, 1, sqrt2, 1, sqrt2 };
	static final double[] phi = { 0.75 * Math.PI, 0.5 * Math.PI, 0.25 * Math.PI,
		Math.PI, 0, 0, 1.25 * Math.PI, 1.5 * Math.PI, 1.75 * Math.PI };
	static final double[] kernel = new double[9];
	double scale;

	@Parameter(min = "0", max = "2 * Math.PI")
	private double theta;

	@Override
	public RandomAccessibleInterval<T> calculate(
		final RandomAccessibleInterval<T> input)
	{
		final RandomAccessibleInterval<T> output = ops().copy().rai(input);

		final long[] planeDims = new long[input.numDimensions() - 2];
		for (int i = 0; i < planeDims.length; i++)
			planeDims[i] = input.dimension(i + 2);
		final Extents extents = new Extents(planeDims);
		final Position planePos = extents.createPosition();
		if (planeDims.length == 0) {
			computePlanar(planePos, input, output);
		}
		else {
			while (planePos.hasNext()) {
				planePos.fwd();
				computePlanar(planePos, input, output);
			}

		}
		System.out.println("done");

		return output;

	}

	private void computePlanar(final Position planePos,
		final RandomAccessibleInterval<T> input,
		final RandomAccessibleInterval<T> output)
	{

		// CONSTRUCT KERNEL

		// build x vector arrays
		final double[] x1 = new double[9];
		for (int i = 0; i < x1.length; i++) {
			x1[i] = Math.cos(phi[i]);
		}
		System.out.println("Cos: " + Arrays.toString(x1));
		final double[] x2 = new double[9];
		for (int i = 0; i < x2.length; i++) {
			x2[i] = Math.sin(phi[i]);
		}
		System.out.println("Sin: " + Arrays.toString(x2));

		// angle vector
		final double cosTheta = Math.cos(theta);
		final double sinTheta = Math.sin(theta);

		System.out.println("Theta = (" + cosTheta + ", " + sinTheta + ")");

		// kernel equal to unit vector of (x dot angle)
		for (int i = 0; i < kernel.length; i++) {
			kernel[i] = 2 * (x1[i] * cosTheta + x2[i] * sinTheta);
		}
		// N.B. the rules of the surrounding pixels do not apply to the center pixel
		kernel[4] = 1;

		System.out.println(Arrays.toString(kernel));

		scale = 0;
		for (final double d : kernel)
			scale += d;
		if (scale == 0) scale = 1; // TODO is this necessary?

		final T type = Util.getTypeFromInterval(input);

		final long[] imageDims = new long[input.numDimensions()];
		input.dimensions(imageDims);

		// create all objects needed for NeighborhoodsAccessible
		RandomAccessibleInterval<T> slicedInput = ops().copy().rai(input);
		for (int i = planePos.numDimensions() - 1; i >= 0; i--) {
			slicedInput = Views.hyperSlice(slicedInput, input.numDimensions() - 1 - i,
				planePos.getLongPosition(i));
		}

		final RandomAccessible<T> refactoredInput = Views.extendMirrorSingle(
			slicedInput);
		final RectangleNeighborhoodFactory<T> factory = RectangleNeighborhood
			.factory();
		final FinalInterval neighborhoodSpan = new FinalInterval(new long[] { -1,
			-1 }, new long[] { 1, 1 });

		final NeighborhoodsAccessible<T> neighborhoods =
			new NeighborhoodsAccessible<>(refactoredInput, neighborhoodSpan, factory);

		// create cursors and random accesses for loop.
		final Cursor<T> cursor = Views.iterable(input).localizingCursor();
		final RandomAccess<T> outputRA = output.randomAccess();
		for (int i = 0; i < planePos.numDimensions(); i++) {
			outputRA.setPosition(planePos.getLongPosition(i), i + 2);
		}
		final RandomAccess<Neighborhood<T>> neighborhoodsRA = neighborhoods
			.randomAccess();

		int algorithmIndex = 0;
		double sum;
		final double[] n = new double[9];
		while (cursor.hasNext()) {
			cursor.fwd();
			neighborhoodsRA.setPosition(cursor);
			final Neighborhood<T> current = neighborhoodsRA.get();
			final Cursor<T> neighborhoodCursor = current.cursor();

			algorithmIndex = 0;
			sum = 0;
			while (algorithmIndex < n.length) {
				neighborhoodCursor.fwd();
				n[algorithmIndex++] = neighborhoodCursor.get().getRealDouble();
			}

			for (int i = 0; i < kernel.length; i++) {
				sum += kernel[i] * n[i];
			}

			double value = sum / scale;

			outputRA.setPosition(cursor.getLongPosition(0), 0);
			outputRA.setPosition(cursor.getLongPosition(1), 1);
			if (value > type.getMaxValue()) value = type.getMaxValue();
			if (value < type.getMinValue()) value = type.getMinValue();
			outputRA.get().setReal(value);
		}
	}

}