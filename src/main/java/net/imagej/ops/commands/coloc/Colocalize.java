package net.imagej.ops.commands.coloc;

import java.util.Arrays;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imagej.ops.coloc.pValue.PValueResult;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

@Plugin(type=Command.class)
public class Colocalize<T extends RealType<T>, U extends RealType<U>> implements Command {

	@Parameter
	private OpService ops;
	
	@Parameter(callback="imageChanged")
	private Img<T> image1;
	
	@Parameter(callback="imageChanged")
	private Img<U> image2;
	
	@Parameter
	private String psfSize;
	
	@Parameter(choices= {"ICQ", "K-Tau", "Pearsons", "MTKT"})
	private String algorithm;

	@Parameter(type=ItemIO.OUTPUT)
	private Double pValue; // GLOBAL OUTPUT: calculated p-value
	
	@Parameter(type=ItemIO.OUTPUT)
	private Double colocValue; // GLOBAL OUTPUT: original calculated coloc measure
	
	@Parameter(type=ItemIO.OUTPUT)
	private double[] colocValuesArray; // GLOBAL OUTPUT: array of coloc measures from each shuffle in PValue
	
	// TODO if algorithm requires divisibility of blockSize, then check image dimensions and provide cropping options (min, max, center) to user
	
	// TODO GLOBAL OUTPUT (OPTION): plot colocValuesArray as histogram/distribution of colocValues... show original colocValue on plot, and simply display p-value somewhere 
	@Parameter(type=ItemIO.OUTPUT)
	private boolean generatePlot;
	
	@Override
	public void run() {
		BinaryFunctionOp<Iterable<T>, Iterable<U>, Double> colocOp;
		PValueResult result = new PValueResult();
		if (algorithm.equals("ICQ")) {
			colocOp = Functions.binary(ops, Ops.Coloc.ICQ.class, Double.class, image1, image2);
		} else if(algorithm.equals("K-Tau")) {
			colocOp = Functions.binary(ops, Ops.Coloc.KendallTau.class, Double.class, image1, image2);
		} else if(algorithm.equals("Pearsons")) {
			colocOp = Functions.binary(ops, Ops.Coloc.Pearsons.class, Double.class, image1, image2);
		} else if(algorithm.equals("MTKT")) {
			colocOp = Functions.binary(ops, Ops.Coloc.MaxTKendallTau.class, Double.class, image1, image2);
		} else {
			throw new IllegalStateException("Unknown algorithm: " + algorithm);
		}
		result = (PValueResult) ops.run(Ops.Coloc.PValue.class, image1, image2, colocOp, parsePsf());
		this.pValue = result.getPValue();
		this.colocValue = result.getColocValue();
		this.colocValuesArray = result.getColocValuesArray();
	}
	
	private Dimensions parsePsf() {
		long[] dims = Arrays.asList(psfSize.split(",")).stream()//
				.mapToLong(Long::parseLong)//
				.toArray();
		return new FinalDimensions(dims);
	}

	@SuppressWarnings("unused")
	private void imageChanged() {
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (int d = 0; d < image1.numDimensions(); d++) {
			final long size = (long) Math.floor(Math.sqrt(image1.dimension(d)));
			if (first) first = false;
			else sb.append(",");
			sb.append(size);
		}
		psfSize = sb.toString();
	}
//	/* TODO PIXEL-WISE - to be implemented in second iteration of Colocalize command
//	 * OUTPUTS:
//	 * 1) array of z-scores
//	 * 2) array of SigPixel (0 or 1)
//	 * 3) overlay of SigPixels
//	 * 4) heat map overlay of z-scores
//	 */
////	@Parameter(type=ItemIO.OUTPUT)
////	private Img<DoubleType> heatMap; // PIXEL-WISE OUTPUT
//	
//	// OPTION: add Parameter for neighborhood size/shape for pixel-wise measure
}
