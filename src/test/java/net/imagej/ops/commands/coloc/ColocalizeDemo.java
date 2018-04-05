package net.imagej.ops.commands.coloc;

import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;

public class ColocalizeDemo {
	
	public static void main(String[] args ) throws Exception {
		Context ctx = new Context();
		DatasetIOService io = ctx.service(DatasetIOService.class);
//		Object image1 = io.open("/Users/ellenarena/Desktop/Desktop/C1-FluorescentCells.tif");
//		Object image2 = io.open("/Users/ellenarena/Desktop/Desktop/C2-FluorescentCells.tif");
		Dataset image1 = io.open("/Users/ellenarena/Desktop/Desktop/C1-confocal-series-0012_crop.tif");
		Dataset image2 = io.open("/Users/ellenarena/Desktop/Desktop/C2-confocal-series-0012_crop.tif");
		CommandModule m = ctx.service(CommandService.class).run(Colocalize.class, true, "image1", image1, "image2", image2, "algorithm", "K-Tau").get();
		Object pValueTest = m.getOutput("pValue");
		System.out.print("p-value = " + pValueTest);
	}

}

