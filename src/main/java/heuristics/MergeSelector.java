package heuristics;

import mdd.Layer;
import mdd.State;

/**
 * Enables defining heuristics to select nodes to be merged when building a relax MDD.
 *
 * @author Vianney Coppé
 */
public interface MergeSelector {

    /**
     * Selects the states to be merged in order to relax the MDD.
     *
     * @param layer  the layer in which we need to merge states
     * @param number the number of states to merge
     * @return an array of {@code State} objects to be merged in the layer
     */
    State[] select(Layer layer, int number);

}
