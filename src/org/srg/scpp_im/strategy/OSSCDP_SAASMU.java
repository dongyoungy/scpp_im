package org.srg.scpp_im.strategy;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

import org.srg.scpp_im.game.InformationState;
import	lpsolve.*; 

public class OSSCDP_SAASMU extends SelfConfirmingDistributionPricePrediction {
	
	private static final long serialVersionUID = 100L;
	
	public OSSCDP_SAASMU(int index)
	{
		super(index);
	}

	// Override
	public String getPPName()
	{
		return "OSSCDP_StraightMU";
	}
	
	public int[] bid(InformationState s)
	{
		int[] newBid = new int[NUM_GOODS];
		//int[] singleGoodValue = new int[NUM_GOODS];
		//int[] priceToBid = new int[NUM_GOODS];
		
		/*
		for (int i=0;i<NUM_GOODS;i++)
		{
			for (BitSet bs : bitVector)
			{
				if (bs.cardinality() == 1 && bs.get(i))
				{
					singleGoodValue[i] = typeDist.get(bs).intValue();
				}
			}
		}
		
		// no prediction 
		if (!this.isPricePredicting)
		{
			for (int i=0;i<NUM_GOODS;i++)
			{
				priceToBid[i] = singleGoodValue[i];
			}
		}

		// Given type-distribution and current information state find the subset that gives highest surplus
		double max_surplus = Double.MIN_VALUE;
		BitSet maxSet = new BitSet();
		
		if (!this.isPricePredicting) // No price prediction - baseline case
		{
			//if (PRINT_DEBUG) System.out.println("No PP");
			for (BitSet bs : bitVector)
			{
				int value = typeDist.get(bs).intValue();
				
				// We bid on a set with the highest value.
				if (value > max_surplus)
				{
					maxSet = bs;
					max_surplus = value;
				}
			}
			int extra = (maxSet.cardinality() > 1) ? (int)Math.floor(max_surplus / (double)maxSet.cardinality()) : 0;
			for (int i=0;i<NUM_GOODS;i++)
			{
				if (max_surplus > 0 && maxSet.get(i)) 
				{
					newBid[i] = priceToBid[i] + extra;
				}
				else newBid[i] = 0;
			}
		}
		else*/
		{
			double[] sampleBid = doSAA();
			for (int i=0;i<NUM_GOODS;i++)
			{
				newBid[i] = (int)Math.round(sampleBid[i]);
			}
		}
		
		return newBid;
	}
	
	private double[] doSAA()
	{
		double[][] scenarios = new double[NUM_SCENARIO][NUM_GOODS];
		double[] maxBid = new double[NUM_GOODS];
		BitSet maxBitSet = null;
		Random ran = new Random();
		
		// Sample E scenarios
		for (int e=0;e<NUM_SCENARIO;e++)
		{
			double dist_num;
			
			for (int i=0;i<NUM_GOODS;i++)
			{
				dist_num = cumulPrediction[i][VALUE_UPPER_BOUND] * ran.nextDouble();
				int pos = Arrays.binarySearch(cumulPrediction[i], dist_num);
				if (pos >= 0) 
				{
					while (cumulPrediction[i][pos] == cumulPrediction[i][pos-1])
					{
						pos--;
					}
					scenarios[e][i] = pos;
				}
				else
				{
					scenarios[e][i] = ((pos * -1) - 1);
				}
			}
		}
		
		int numSets = (int)Math.pow(2, NUM_GOODS);
		int numVar = numSets; // * NUM_SCENARIO;
		double[] coeffs = new double[numVar];
		
		for (int e=0;e<NUM_SCENARIO;e++)
		{
			for (int i=0;i<numSets;i++)
			{
				BitSet bs = bitVector[i];
				double value = (double)this.typeDist.get(bs).intValue();
				double price = 0;
				
				for (int k=0;k<NUM_GOODS;k++)
				{
					if (bs.get(k))
					{
						price += scenarios[e][k];
					}
				}
				coeffs[i] += (value - price);
			}
		}
		
		try
		{
			LpSolve solver = LpSolve.makeLp(0, numVar);
			solver.setObjFn(coeffs);
			solver.setMaxim();
			solver.setVerbose(0);
			// all variables are binary, bid or not.
			for (int i=1;i<=numVar;i++)
			{
				solver.setBinary(i, true);
			}
			
			double[] constraint = new double[numVar];
			for (int i=0;i<numSets;i++)
			{
				constraint[i] = 1;
			}
			solver.addConstraint(constraint, LpSolve.LE, 1);
			solver.solve();
			double[] var = solver.getPtrVariables();
			/*
			System.out.println("Value of objective function: " + solver.getObjective());
			double sum = 0;
		      for (int i = 0; i < var.length; i++) {
		       // System.out.println("Value of var[" + i + "] = " + var[i]);
		        sum += var[i];
		      }
		    System.out.println("Sum = " + sum);
		    */
			int bundleToBid = -1;
			for (int i=0;i<numSets;i++)
			{
				if (var[i] == 1) bundleToBid = i;
			}
			
			for (int i=0;i<NUM_GOODS;i++)
			{
				maxBid[i] = 0.0;
			}
			
			if (bundleToBid > 0)
			{
				BitSet bs = bitVector[bundleToBid];
				for (int e=0;e<NUM_SCENARIO;e++)
				{
					for (int k=0;k<NUM_GOODS;k++)
					{
						if (bs.get(k))
						{
							if (maxBid[k] < scenarios[e][k]) maxBid[k] = scenarios[e][k];
						}
					}
				}
			}
			/*
			for (int e=0;e<NUM_SCENARIO;e++)
			{
				for (int i=0;i<numSets;i++)
				{
					if (var[i] > 0)
					{
						BitSet bs = bitVector[i];
						
						for (int k=0;k<NUM_GOODS;k++)
						{
							if (bs.get(k))
							{
								if (maxBid[k] < scenarios[e][k]) maxBid[k] = scenarios[e][k];
							}
						}
					}
				}
			}
			*/
			/*
			double maxSurplus = Double.NEGATIVE_INFINITY;
			
			for (int i=0;i<numSets;i++)
			{
				double totalSurplus = 0;
				BitSet bs = bitVector[i];
				for (int e=0;e<NUM_SCENARIO;e++)
				{
					boolean win = true;
					double cost = 0.0;
					for (int k=0;k<NUM_GOODS;k++)
					{
						if (bs.get(k))
						{
							cost += scenarios[e][k];
							if (maxBid[k] < scenarios[e][k]) win = false;
						}
					}
					
					if (win)
					{
						double value = (double)this.typeDist.get(bs).intValue();
						totalSurplus += (value - cost); 
					}
				}
				if (totalSurplus > maxSurplus)
				{
					maxSurplus = totalSurplus;
					maxBitSet = bs;
				}
			}
			*/
			double surplus = solver.getObjective();
			for (int k=0;k<NUM_GOODS;k++)
			{
				if (surplus < 0) // || !maxBitSet.get(k))
				{
					maxBid[k] = 0;
				}
			}
			
			solver.deleteLp();
		}
		catch (LpSolveException e)
		{
			e.printStackTrace();
		}
		//double[] temp = new double[NUM_GOODS];
		return maxBid;
	}
}
