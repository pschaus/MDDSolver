package examples;

import core.Problem;
import core.Solver;
import core.Variable;
import dp.Layer;
import dp.State;
import dp.StateRepresentation;
import heuristics.MinLPDeleteSelector;
import heuristics.MinLPMergeSelector;
import heuristics.VariableSelector;
import utils.InconsistencyException;

import java.io.File;
import java.util.*;

/**
 * Implementation of the Maximum Independent Set Problem.
 * 
 * @author Vianney Coppé
 */
public class MISP implements Problem {

    private double[] weights;
    private LinkedList<Integer>[] g;

    private int nVariables;
    private State root;
	
	/**
	 * Creates the representation of the MISP problem.
	 * @param n the number of vertices
	 * @param weights the weights of the vertices
	 * @param edges an array of {@code Edge} objects with vertices in [0,n-1]
	 */
	public MISP(int n, double [] weights, Edge [] edges) {
		this(weights, toGraph(n, edges));
	}
	
	/**
	 * Creates the representation of the MISP problem.
	 * @param weights the weights of the vertices
	 * @param g the adjacency lists
	 */
	public MISP(double [] weights, LinkedList<Integer> [] g) {
		this.nVariables = weights.length;
		this.weights = weights;
		this.g = g;
		
		Variable [] variables = new Variable[this.nVariables];
		for(int i = 0; i < this.nVariables; i++) {
			variables[i] = new Variable(i, 2);
		}

		this.root = new State(new MISPState(this.nVariables), variables, 0);
	}

	public State root() {
		return this.root;
	}

	public int nVariables() {
		return this.nVariables;
	}

    public static LinkedList<Integer>[] toGraph(int n, Edge[] edges) {
        @SuppressWarnings("unchecked")
        LinkedList<Integer>[] adj = new LinkedList[n];
        for (int i = 0; i < n; i++) {
            adj[i] = new LinkedList<>();
        }

        for (Edge e : edges) {
            adj[e.u].add(e.v);
            adj[e.v].add(e.u);
		}

        return adj;
	}

	public State merge(Set<State> states) {
		Variable [] variables = null;
		double maxValue = Double.MIN_VALUE;
		MISPState mispState = null;
		
		for(State state : states) {
			if(mispState == null) {
				variables = state.variables();
				mispState = ((MISPState) state.stateRepresentation()).copy();
			}
			
			mispState.bs.or(((MISPState) state.stateRepresentation()).bs);
			maxValue = Math.max(maxValue, state.value());
		}
		
		return new State(mispState, variables, maxValue);
	}

    /**
     * Instances can be found on <a href="https://turing.cs.hbg.psu.edu/txn131/clique.html#DIMACS_cliques">this website</a>.
     * Since they are maximum clique problems, we take the complement graph to use our MISP solver.
     *
     * @param path path to an input file in DIMACS edge format
     */
    public static MISP readDIMACS(String path) {
        int n = 0, m, i = 0;
        double opt = -1;
        Edge[] edges = null;

        try {
            Scanner scan = new Scanner(new File(path));

            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                String[] tokens = line.split("\\s+");

                if (tokens.length > 0) {
                    if (tokens[0].equals("c")) {
                        if (tokens.length > 2 && tokens[1].equals("opt")) {
                            opt = Double.valueOf(tokens[2]);
                        }
                        continue;
                    }
                    if (tokens[0].equals("p")) {
                        assert (tokens.length == 4);
                        assert (tokens[1].equals("edge"));
                        n = Integer.valueOf(tokens[2]);
                        m = Integer.valueOf(tokens[3]);
                        edges = new Edge[m];
                    } else {
                        if (tokens.length == 3) {
                            int u = Integer.valueOf(tokens[1]) - 1;
                            int v = Integer.valueOf(tokens[2]) - 1;
                            edges[i++] = new Edge(u, v);
                        }
                    }
                }
            }

            scan.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        double[] weights = new double[n];

        LinkedList<Integer>[] g = MISP.toGraph(n, edges);
        @SuppressWarnings("unchecked")
        LinkedList<Integer>[] complement = new LinkedList[n];

        for (i = 0; i < n; i++) {
            weights[i] = 1;
            complement[i] = new LinkedList<>();

            for (int j = 0; j < n; j++)
                if (i != j && !g[i].contains(j)) {
                    complement[i].add(j);
                }
        }

        if (opt != -1) {
            System.out.println("Value to reach : " + opt);
        }

        return new MISP(weights, complement);
    }

    public static void main(String[] args) {
	    /*
		double [] weights = {3, 4, 2, 2, 7};
		Edge [] edges = {new Edge(0, 1), new Edge(0, 2), new Edge(1, 2), new Edge(1, 3), new Edge(2, 3), new Edge(3, 4)};

		Problem p = new MISP(5, weights, edges);

		Solver solver = new Solver(p, new MinLPMergeSelector(), new MinLPDeleteSelector(), new MISP.MISPVariableSelector());
		solver.solve();*/

        Solver solver = new Solver(readDIMACS("data/misp/easy/johnson8-4-4.clq"), new MinLPMergeSelector(), new MinLPDeleteSelector(), new MISP.MISPVariableSelector());

        long t0 = System.currentTimeMillis();
        System.out.println(solver.solve(100).value());
        System.out.println("time:" + (System.currentTimeMillis() - t0));

    }

    public Set<State> successors(State s, Variable var) {
        int u = var.id();
        Set<State> ret = new HashSet<>();
        MISPState mispState = ((MISPState) s.stateRepresentation());
        Variable[] variables = s.variables();

        // assign 0
        MISPState mispState0 = mispState.copy();
        mispState0.bs.clear(u);
        State dontTake = new State(mispState0, variables, s.value());

        try {
            dontTake.assign(u, 0);
        } catch (InconsistencyException e) {
            e.printStackTrace();
        }

        ret.add(dontTake);

        if (!mispState.isFree(u)) {
            return ret;
        }

        // assign 1
        MISPState mispState1 = mispState.copy();
        mispState1.bs.clear(u);

        for (int v : g[u]) {
            mispState1.bs.clear(v);
        }

        State take = new State(mispState1, variables, s.value() + this.weights[u]);

        try {
            take.assign(u, 1);
        } catch (InconsistencyException e) {
            e.printStackTrace();
        }

        ret.add(take);

        return ret;
    }

    public static class MISPVariableSelector implements VariableSelector {

        public Variable select(Variable[] vars, Layer layer) {
            int minCount = Integer.MAX_VALUE, index = -1;
            int[] count = new int[vars.length];

            for (State state : layer.states()) {
                for (int i = 0; i < vars.length; i++)
                    if (!vars[i].isAssigned()) {
                        if (((MISPState) state.stateRepresentation()).isFree(i)) {
                            count[i]++;
                        }

                        if (count[i] < minCount) {
                            minCount = count[i];
                            index = i;
                        }
                    }
            }

            return vars[index];
        }

    }

    public class MISPState implements StateRepresentation {

        int size;
        BitSet bs;

        public MISPState(int size) {
            this.size = size;
            this.bs = new BitSet(size);
            this.bs.flip(0, size);
        }

        public MISPState(BitSet bitSet) {
            this.size = bitSet.size();
            this.bs = (BitSet) bitSet.clone();
        }

        public int hashCode() {
            return this.bs.hashCode();
        }

        public boolean equals(Object o) {
            return o instanceof MISPState && this.bs.equals(((MISPState) o).bs);
        }

        public boolean isFree(int u) {
            return this.bs.get(u);
        }

        public MISPState copy() {
            return new MISPState(this.bs);
        }

        public double rank(State state) {
            return state.value();
        }

        public String toString() {
            return this.bs.toString();
        }
    }
}