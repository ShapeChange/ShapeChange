package de.interactive_instruments.ShapeChange.Target;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * Targets implementing this interface indicate that all memory intensive output
 * writing tasks can be deferred until the end of the overall model processing
 * performed by ShapeChange. Targets can create intermediate files during the
 * initial run of the write/writeAll methods (defined by the Target/SingleTarget
 * interfaces). Once all transformer and targets have been processed, the
 * converter will release all resources associated with the input model and any
 * intermediate model created through transformations. Then it will go through
 * the list of targets, identify those that support this interface, initialize
 * them as defined via the configuration, and call the writeOutput method of
 * this interface.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 * 
 */
public interface DeferrableOutputWriter {

	/**
	 * Perform any initialization required to perform the deferred write.
	 * 
	 * @param o tbd
	 * @param r tbd
	 */
	public void initialise(Options o, ShapeChangeResult r);

	/**
	 * Execute the deferred write of the desired output.
	 */
	public void writeOutput();

}
