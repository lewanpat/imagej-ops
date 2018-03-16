/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2017 ImageJ developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.ops.coloc.heatmap;

import net.imagej.ops.Ops;
import net.imagej.ops.special.chain.RAIs;
import net.imagej.ops.special.computer.AbstractBinaryComputerOp;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory.Boundary;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Computes pixel-wise colocalization statistic by neighborhood using a
 * {@link Shape}.
 *
 * @author Curtis Rueden
 * @author Ellen T Arena
 * @param <T> element type of images
 */
@Plugin(type = Ops.Morphology.Erode.class)
public class DefaultHeatMap<T extends RealType<T> & NativeType<T>> extends
	AbstractBinaryComputerOp<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>, IterableInterval<DoubleType>>
//	implements /*Ops.Coloc.HeatMap,*/ Contingent
{

	@Parameter
	private int span;

	@Parameter
	private BinaryFunctionOp<Iterable<T>, Iterable<T>, Double> colocOp;

	@Parameter(required = false)
	private OutOfBoundsFactory<T, RandomAccessibleInterval<T>> outOfBounds;

	private BinaryFunctionOp<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>, Double> pValueOp;

	@Override
	public void initialize() {
		if (outOfBounds == null) {
			outOfBounds = new OutOfBoundsMirrorFactory<>(Boundary.DOUBLE);
		}
		pValueOp = Functions.binary(ops(), Ops.Coloc.PValue.class, Double.class,
			in1(), in2(), colocOp, 50);
	}

	@Override
	public void compute(final RandomAccessibleInterval<T> in1,
		final RandomAccessibleInterval<T> in2,
		final IterableInterval<DoubleType> output)
	{
		// TODO: validate input images dimensions and compatibility

		final RandomAccessibleInterval<T> extended1 = RAIs.extend(in1, outOfBounds);
		final RandomAccessibleInterval<T> extended2 = RAIs.extend(in2, outOfBounds);
		final RectangleShape shape = new RectangleShape(span, false);
		final IterableInterval<Neighborhood<T>> neighborhoods1 = shape
			.neighborhoodsSafe(extended1);
		final IterableInterval<Neighborhood<T>> neighborhoods2 = shape
			.neighborhoodsSafe(extended2);
		final Cursor<Neighborhood<T>> nc1 = neighborhoods1.cursor();
		final Cursor<Neighborhood<T>> nc2 = neighborhoods2.cursor();
		final Cursor<DoubleType> outCursor = output.cursor();

		Img<T> buffer1 = null, buffer2 = null;
		while (nc1.hasNext() && nc2.hasNext()) {
			final Neighborhood<T> neighborhood1 = nc1.next();
			final Neighborhood<T> neighborhood2 = nc2.next();
			if (buffer1 == null) {
				// TODO: consider using create.img op
				final ArrayImgFactory<T> factory = new ArrayImgFactory<>();
				buffer1 = factory.create(neighborhood1, neighborhood1.firstElement());
				buffer2 = factory.create(neighborhood2, neighborhood2.firstElement());
			}
			alignAndCopy(neighborhood1, buffer1);
			alignAndCopy(neighborhood2, buffer2);
			final Double value = pValueOp.calculate(buffer1, buffer2);
			outCursor.next().set(value == null ? Double.NaN : value);
		}
	}

	private void alignAndCopy(final IterableInterval<T> src,
		final RandomAccessibleInterval<T> dest)
	{
		final IntervalView<T> alignedDest = Views.translate(dest, Intervals.minAsLongArray(src));

		// TODO: instead, call a copy op once one exists
		final Cursor<T> cursor = src.localizingCursor();
		final RandomAccess<T> access = alignedDest.randomAccess();
		while (cursor.hasNext()) {
			final T value = cursor.next().copy();
			access.setPosition(cursor);
			access.get().set(value);
		}
	}
}
