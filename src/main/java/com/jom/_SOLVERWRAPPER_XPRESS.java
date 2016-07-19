/**
 * Copyright (c) 2015 Pablo Pavon Mari�o.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mari�o - initial API and implementation
 */

/**
 *
 */

package com.jom;

/* PABLO: CHANGE POM TO COMPILE: THE JAR SHOULD BE LOCAL? SOME FORM TO COMPILE WITHOUT THE JAR?
 * VEr: http://stackoverflow.com/questions/8325819/how-can-i-create-an-executable-jar-without-dependencies-using-maven
 * PABLO: Any benefit from loafglobal64 in Java? I mean that all the arrays have at most 2^32 vals...
 * PABLO: Which is the control for getting the best bound in LP?
 * PABLO: Which is the control for getting the output status when LP found feasible but not optimal solution?
 * PABLO: Error getting the primal solution: calcObjective gives error inside XPROSprob (line 5793), no further message
 * PABLO: how to get multipliers of LB and UB constraints
 *   */

/*
 * PABLO: I may have a bound even if a feasible solution was not found?
 * PABLO: can happen that dual and primal feasible, but not optimal??  
 * PABLO: can happen that dual feasible, primal infeasible => then I still have a bound?
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import com.dashoptimization.DoubleHolder;
import com.dashoptimization.XPRS;
import com.dashoptimization.XPRSconstants;
import com.dashoptimization.XPRSenumerations;
import com.dashoptimization.XPRSprob;

import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.jet.math.tdouble.DoubleFunctions;

/** @author Pablo */
class _SOLVERWRAPPER_XPRESS
{
	private final HashMap<String, Object> param;
	private final _INTERNAL_SolverIO      s;
	private final String                  solverLibraryName;

	_SOLVERWRAPPER_XPRESS(_INTERNAL_SolverIO s, HashMap<String, Object> param)
	{
		this.s = s;
		this.solverLibraryName = (String) param.get("solverLibraryName");
		this.param = param;
	}

	int solve()
	{
		XPRSprob p = null;

		try
		{
			XPRS.init(solverLibraryName); 
			p = new XPRSprob();
            final int ncols = s.in.numDecVariables;
            final int nrows = s.in.numConstraints;
            final double[] _drhs = (s.in.numConstraints == 0) ? new double[0] : 
    				Arrays.copyOf(s.in.lhsMinusRhsAccumulatedConstraint.getAffineExpression().getConstantCoefArray(), s.in
    						.lhsMinusRhsAccumulatedConstraint.getAffineExpression().getConstantCoefArray().length);
            final byte[] _srowtypes = new byte [s.in.numConstraints]; Arrays.fill(_srowtypes , (byte) 'R');
    		for (int contC = 0; contC < _srowtypes.length; contC++)
    		{
    			_drhs[contC] = -_drhs[contC];
    			final double lb = s.in.constraintLowerBound.get(contC); // 0 or -Double.MAX_VALUE
    			final double ub = s.in.constraintUpperBound.get(contC); // 0 or Double.MAX_VALUE
    			if (lb == ub)
    				_srowtypes[contC] = 'E';
    			else if ((lb == -Double.MAX_VALUE) && (ub == Double.MAX_VALUE))
    				throw new JOMException("JOM - XPRESS interface. Unbounded contraints are not allowed"); // cType.set(contC,GLP_FR);
    			else if ((lb != -Double.MAX_VALUE) && (ub == Double.MAX_VALUE))
    				_srowtypes[contC] = 'G';
    			else if ((lb == -Double.MAX_VALUE) && (ub != Double.MAX_VALUE))
    				_srowtypes[contC] = 'L';
    			else
    				throw new JOMException("JOM - XPRESS interface. Double bounded contraints are supposed not to exist in JOM");
    		}
            final double [] _drange = null;
            final double[] _dobj = s.in.objectiveFunction.getAffineExpression().getCellLinearCoefsFull(0);
			_INTERNAL_AffineExpressionCoefs constraintsMatrix = s.in.lhsMinusRhsAccumulatedConstraint.getAffineExpression();
			int[][] return_mnel = new int [1][]; 
			int[][] return_mrwind = new int [1][];
			int[][] return_mstart = new int [1][];
			double[][] return_dmatval = new double [1][];
			constraintsMatrix.getNonZerosRowColValForXpressSolver(return_mnel, return_mrwind, return_mstart, return_dmatval, ncols);
			final double [] _dlb = new double [ncols];
			final double [] _dub = new double [ncols];
			for (int col = 0 ; col < ncols ; col ++)
			{
				_dlb [col] = s.in.primalSolutionLowerBound.get(col) == -Double.MAX_VALUE? XPRSconstants.MINUSINFINITY : s.in.primalSolutionLowerBound.get(col);
				_dub [col] = s.in.primalSolutionUpperBound.get(col) == Double.MAX_VALUE? XPRSconstants.PLUSINFINITY : s.in.primalSolutionUpperBound.get(col);
			}
			final int ngents = s.in.primalSolutionIsInteger.zSum();
			final int nsets = 0;
			final byte[] _qgtype = new byte [ngents]; 
			int[] _mgcols = new int [ngents]; 
			if (ngents > 0)
			{
				Arrays.fill(_qgtype , (byte) 'I');
				IntArrayList nonZerosCols = new IntArrayList ();
				s.in.primalSolutionIsInteger.getNonZeros(nonZerosCols , new IntArrayList ());
				_mgcols = nonZerosCols.elements();
			}
			final double[] _dlim = null;
			final byte[] _stype = null;
			final int[] _msstart = null;
			final int[] _mscols = null;
			final double[] _dref = null;
			
			/* Set the environment parameters, including the maxSolverTime */
			for (Entry<String, Object> entry : this.param.entrySet())
			{
				String keyStr = entry.getKey();
				if (keyStr.equalsIgnoreCase("solverLibraryName")) continue;
				int key = new Integer(entry.getKey());
				Object value = entry.getValue();
				if (value instanceof String)
					p.setStrControl(key , (String) value);
				else if (value instanceof Integer)
					p.setIntControl(key , (Integer) value);
				else if (value instanceof Double)
					p.setDblControl(key , (double) value);
				else
					throw new JOMException("JOM - XPRESS interface. Unknown value type in parameters");
			}
			
			
			if (s.in.hasIntegerVariables)
			{
				/* load the problem */
				p.loadGlobal("" , ncols, nrows, _srowtypes, _drhs, _drange, _dobj, return_mstart [0],
						return_mnel [0], return_mrwind [0], return_dmatval [0], _dlb, _dub, ngents, nsets, _qgtype, _mgcols, _dlim,
						_stype, _msstart, _mscols, _dref);
				/* Minimize or maximize */
				p.chgObjSense(s.in.toMinimize? XPRSenumerations.ObjSense.MINIMIZE : XPRSenumerations.ObjSense.MAXIMIZE);

				/* Call the solver */
				p.mipOptimize();

				s.problemAlreadyAttemptedTobeSolved = true;
				s.out.bestOptimalityBound = p.getDblAttrib(XPRSconstants.BESTBOUND);
				s.out.statusCode = p.getIntAttrib(XPRSconstants.ERRORCODE);
				s.out.statusMessage = p.getLastError();

				final int mipStatus = p.getIntAttrib(XPRSconstants.MIPSTATUS);
				s.out.solutionIsOptimal = (mipStatus == XPRSconstants.MIP_OPTIMAL);
				s.out.solutionIsFeasible = s.out.solutionIsOptimal || (mipStatus == XPRSconstants.MIP_SOLUTION);
				s.out.feasibleSolutionDoesNotExist = (mipStatus == XPRSconstants.MIP_INFEAS);
				s.out.foundUnboundedSolution = (mipStatus == XPRSconstants.MIP_UNBOUNDED);

				/* I may have a bound even if a feasible solution was not found */
				s.out.primalCost = p.getDblAttrib(XPRSconstants.MIPBESTOBJVAL);

				if (!s.out.solutionIsFeasible)
					return s.out.statusCode;

				/* Check the number of constraitns and variables */
				if (p.getIntAttrib(XPRSconstants.ROWS) != s.in.numConstraints) throw new JOMException("JOM - XPRESS interface. Unexpected error");
				if (p.getIntAttrib(XPRSconstants.COLS)  != s.in.numDecVariables) throw new JOMException("JOM - XPRESS interface. Unexpected error");

				/* Retrieve the optimal primal solution */
				double [] primalSolution = new double [s.in.numDecVariables];
				double [] slackSolution = new double [s.in.numConstraints];
				p.getMipSol(primalSolution , slackSolution);
				s.out.primalSolution = DoubleFactory1D.dense.make(primalSolution);

				/* Retrieve the values of the constraints in the solution */
				double[] rhsCplex = (s.in.numConstraints == 0) ? new double[0] :
						Arrays.copyOf(s.in.lhsMinusRhsAccumulatedConstraint.getAffineExpression().getConstantCoefArray(), s.in
								.lhsMinusRhsAccumulatedConstraint.getAffineExpression().getConstantCoefArray().length);
				for (int cont = 0; cont < rhsCplex.length; cont++) rhsCplex[cont] = -rhsCplex[cont];
				
				double[] slack = new double[s.in.numConstraints];
				p.calcSlacks(primalSolution , slack);
				s.out.primalValuePerConstraint = DoubleFactory1D.dense.make(rhsCplex).assign(DoubleFactory1D.dense.make(slack), DoubleFunctions.minus);
				s.out.multiplierOfConstraint = DoubleFactory1D.dense.make(s.in.numConstraints);
				s.out.multiplierOfLowerBoundConstraintToPrimalVariables = DoubleFactory1D.dense.make(s.in.numDecVariables);
				s.out.multiplierOfUpperBoundConstraintToPrimalVariables = DoubleFactory1D.dense.make(s.in.numDecVariables);
				
				return s.out.statusCode;
			}
			else
			{
				/* load the problem */
				p.loadLp("" , ncols, nrows, _srowtypes, _drhs, _drange, _dobj, return_mstart [0], return_mnel [0], return_mrwind [0], 
						return_dmatval [0], _dlb, _dub);
				/* Minimize or maximize */
				p.chgObjSense(s.in.toMinimize? XPRSenumerations.ObjSense.MINIMIZE : XPRSenumerations.ObjSense.MAXIMIZE);

				/* Call the solver */
				p.lpOptimize();

				p.writeProb("c:\\Dropbox\\mpsFileProb");
				p.writeSol("c:\\Dropbox\\mpsFileSol");
				
				s.problemAlreadyAttemptedTobeSolved = true;
				s.out.bestOptimalityBound = s.in.toMinimize? -Double.MAX_VALUE : Double.MAX_VALUE; //p.getDblAttrib(XPRSconstants.BESTBOUND); // pablo: this may not be the one for this
				s.out.statusCode = p.getIntAttrib(XPRSconstants.ERRORCODE);
				s.out.statusMessage = p.getLastError();

				final int lpStatus = p.getIntAttrib(XPRSconstants.LPSTATUS);
				s.out.solutionIsOptimal = (lpStatus == XPRSconstants.LP_OPTIMAL);
				final int a = p.getIntAttrib(XPRSconstants.PRIMALINFEAS);
				s.out.solutionIsFeasible = s.out.solutionIsOptimal || ((lpStatus == XPRSconstants.LP_UNFINISHED) && (p.getIntAttrib(XPRSconstants.PRIMALINFEAS) == 0));  
				s.out.feasibleSolutionDoesNotExist = (lpStatus == XPRSconstants.LP_INFEAS);
				s.out.foundUnboundedSolution = (lpStatus == XPRSconstants.LP_UNBOUNDED);

				if (!s.out.solutionIsFeasible)
					return s.out.statusCode;

				/* Check the number of constraitns and variables */
				if (p.getIntAttrib(XPRSconstants.ROWS) != s.in.numConstraints) throw new JOMException("JOM - XPRESS interface. Unexpected error");
				if (p.getIntAttrib(XPRSconstants.COLS)  != s.in.numDecVariables) throw new JOMException("JOM - XPRESS interface. Unexpected error");

				/* Retrieve the optimal primal solution */
				final double [] primalSolution = new double [s.in.numDecVariables];
				double [] slackSolution = new double [s.in.numConstraints];
				double [] mulipliersSolution = new double [s.in.numConstraints];
				double [] reducedCosts = new double [s.in.numDecVariables];
				p.getLpSol(primalSolution , slackSolution , mulipliersSolution , reducedCosts);
				s.out.primalSolution = DoubleFactory1D.dense.make(primalSolution);
				//DoubleHolder obj = new DoubleHolder(); p.calcObjective(primalSolution , obj);
				s.out.primalCost = p.getDblAttrib(XPRSconstants.LPOBJVAL);//s.in.objectiveFunction.evaluate_internal(primalSolution).toValue();
				//s.out.bestOptimalityBound;
				
				
				/* Retrieve the values of the constraints in the solution */
				double[] rhsCplex = (s.in.numConstraints == 0) ? new double[0] :
						Arrays.copyOf(s.in.lhsMinusRhsAccumulatedConstraint.getAffineExpression().getConstantCoefArray(), s.in
								.lhsMinusRhsAccumulatedConstraint.getAffineExpression().getConstantCoefArray().length);
				for (int cont = 0; cont < rhsCplex.length; cont++) rhsCplex[cont] = -rhsCplex[cont];
				
				double[] slack = new double[s.in.numConstraints];
				p.calcSlacks(primalSolution , slack);
				s.out.primalValuePerConstraint = DoubleFactory1D.dense.make(rhsCplex).assign(DoubleFactory1D.dense.make(slack), DoubleFunctions.minus);
				s.out.multiplierOfConstraint = DoubleFactory1D.dense.make(mulipliersSolution);
				s.out.multiplierOfLowerBoundConstraintToPrimalVariables = DoubleFactory1D.dense.make(s.in.numDecVariables);
				s.out.multiplierOfUpperBoundConstraintToPrimalVariables = DoubleFactory1D.dense.make(s.in.numDecVariables);
				for (int dv = 0; dv < s.in.numDecVariables; dv++)
				{
					double lb = s.in.primalSolutionLowerBound.get(dv);
					double ub = s.in.primalSolutionUpperBound.get(dv);
					double val = s.out.primalSolution.get(dv);
					if (lb == ub)
					{
						s.out.multiplierOfUpperBoundConstraintToPrimalVariables.set(dv, reducedCosts[dv]);
						s.out.multiplierOfUpperBoundConstraintToPrimalVariables.set(dv, -reducedCosts[dv]);
					} else if (Math.abs(val - lb) > Math.abs(val - ub))
						s.out.multiplierOfUpperBoundConstraintToPrimalVariables.set(dv, reducedCosts[dv]);
					else
						s.out.multiplierOfLowerBoundConstraintToPrimalVariables.set(dv, reducedCosts[dv]);
				}
				
				return s.out.statusCode;
			}
	
		} catch (RuntimeException e)
		{
			try { XPRS.free(); } catch (Exception ee) {} // frees the license, can fail if already done
			e.printStackTrace();
			System.out.println(e.getLocalizedMessage());
			if (p != null) p.destroy(); // frees any memory associated to the object
			throw e;
		} 
		
	}

}
