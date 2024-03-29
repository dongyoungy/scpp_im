package org.srg.scpp_im.strategy;

import org.srg.scpp_im.game.Strategy;
import org.srg.scpp_im.game.InformationState;
import org.srg.scpp_im.game.GameSetting;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.BitSet;


public class SelfConfirmingOwnEffectPricePredictionNoExplorer extends GameSetting implements Serializable, Strategy {

	private static final long serialVersionUID = 100L;
	private static final int BETA = 127;
	
	private int index;
	private int batchCount;
	private int lastUnitsToBuy;
	private boolean isSingleUnitDemand;
	//private boolean isExplorer;
	private Map<BitSet, Integer> typeDist;
	private int[] cumulativeVal;
	private double[][] predictionMatrix;
	private double[][] prevPredictionMatrix;
	private double[][] priceObservation;
	private int[] observationCount;
	private BitSet[] bitVector;
	
	public SelfConfirmingOwnEffectPricePredictionNoExplorer(int index)
	{
		this.index = index;
		//this.observationCount = 0;
		this.batchCount = 0;
		this.lastUnitsToBuy = 0;
		this.isSingleUnitDemand = true;
		//this.isExplorer = false;
		//if (index == 1) this.isExplorer = true;
		cumulativeVal = new int[NUM_GOODS];
		observationCount = new int[NUM_GOODS];
		predictionMatrix = new double[NUM_GOODS][NUM_GOODS];
		prevPredictionMatrix = new double[NUM_GOODS][NUM_GOODS];
		priceObservation = new double[NUM_GOODS][NUM_GOODS];
		for (int i=0;i<NUM_GOODS;i++)
		{
			this.observationCount[i] = 0;
			for (int j=0;j<NUM_GOODS;j++)
			{
				predictionMatrix[i][j] = 0;
				prevPredictionMatrix[i][j] = 0;
				priceObservation[i][j] = 0;
			}
		}
		bitVector = new BitSet[(int)Math.pow(2,NUM_GOODS)];
		for (int i=0;i<Math.pow(2, NUM_GOODS);i++)
		{
			BitSet bs = new BitSet();
			String bits = Integer.toBinaryString(i);
			bits = new StringBuffer(bits).reverse().toString();
			for (int j=0;j<bits.length();j++)
			{
				char bitChar = bits.charAt(j);
				String bitStr = String.valueOf(bitChar);
				int bit = Integer.parseInt(bitStr);
				if (bit == 1) bs.set(j, true);
				else bs.set(j, false);
			}
			bitVector[i] = bs;
		}
	}
	
	public int getIndex()
	{
		return index;
	}
	public Map<BitSet, Integer> getTypeDist()
	{
		return typeDist;
	}
	public void setTypeDist(Map<BitSet, Integer> typeDist)
	{
		this.typeDist = typeDist;
		this.checkSingleDemand();
		for (BitSet bs : bitVector)
		{
			if (bs.cardinality() != 0)
			{
				int val = typeDist.get(bs).intValue();
				cumulativeVal[bs.cardinality()-1] = val;
			}
		}
	}
	public <T>void setPricePrediction(T pp)
	{
		this.predictionMatrix = (double[][])pp;
	}
	
	public int getCurrentSurplus(InformationState s)
	{
		int[] currentBid = s.getCurrentBidPrice();
		int[] currentWinning = s.getCurrentBidWinning();
		
		BitSet bs = new BitSet();
		int cost = 0;
		for (int i=0;i<NUM_GOODS;i++)
		{
			if (currentWinning[i] == index)
			{
				bs.set(i);
				cost += currentBid[i];
			}
		}
		int value;
		value = typeDist.get(bs) != null ? typeDist.get(bs).intValue() : 0;
		return (value - cost);
	}
	public void printPrediction()
	{
		for (int i=0;i<NUM_GOODS;i++)
		{
			for (int j=0;j<NUM_GOODS;j++)
			{
				System.out.print(predictionMatrix[i][j] + " ");
			}
			System.out.println();
		}
		
	}
	public void setNewPrediction()
	{
		//pricePrediction = priceObservation;
		//priceObservation = new int[NUM_GOODS][BETA+1];
		//int goodNum = this.batchCount % NUM_GOODS;
		for (int i=0;i<NUM_GOODS;i++)
		{
			for (int j=0;j<NUM_GOODS;j++)
			{
				prevPredictionMatrix[i][j] = predictionMatrix[i][j];
			}
		}
		for (int i=0;i<NUM_GOODS;i++)
		{
			//prevPredictionMatrix[i][goodNum] = predictionMatrix[i][goodNum];
			for	(int j=0;j<NUM_GOODS;j++)
			{
				predictionMatrix[i][j] = (this.observationCount[j] == 0) ? 0 :(double)priceObservation[i][j] / (double)this.observationCount[j];
				priceObservation[i][j] = 0;
			}
		}
		this.batchCount++;
		for (int i=0;i<NUM_GOODS;i++)
		{
			this.observationCount[i] = 0;
		}
	}
	public void resetObservation()
	{
		for (int i=0;i<NUM_GOODS;i++)
		{
			for (int j=0;j<NUM_GOODS;j++)
			{
				priceObservation[i][j] = 0;
			}
		}
		for (int i=0;i<NUM_GOODS;i++)
		{
			this.observationCount[i] = 0;
		}
	}
	public double getMaxDist()
	{
		double maxDist = 0;
		for (int i=0;i<NUM_GOODS;i++)
		{
			for (int j=0;j<NUM_GOODS;j++)
			{
				double dist = predictionMatrix[i][j] - prevPredictionMatrix[i][j];
				if (Math.abs(dist) > Math.abs(maxDist))
				{
					maxDist = dist;
				}
			}
		}
		return maxDist;
	}
	public void addObservation(InformationState s)
	{
		int[] finalPrice = s.getCurrentBidPrice();
		int[] finalWinner = s.getCurrentBidWinning();
		int numWin = 0;
		for (int i=0;i<NUM_GOODS;i++)
		{
			if (finalWinner[i] == index)
			{
				numWin++;
			}
		}
		if (numWin == lastUnitsToBuy)
		{
			for (int i=0;i<NUM_GOODS;i++)
			{
				priceObservation[i][lastUnitsToBuy] += finalPrice[i];
			}
			this.observationCount[lastUnitsToBuy]++;
		}
	}
	
	public int[] bid(InformationState s)
	{
		int[] newBid = new int[NUM_GOODS];
		int[] currentBid = s.getCurrentBidPrice();
		int[] currentWinning = s.getCurrentBidWinning();
		double[] currentPrediction = new double[NUM_GOODS];
		int unitsToBuy = 0;
		
		int maxSurplus = 0;
		//if (this.isExplorer) unitsToBuy = this.batchCount % NUM_GOODS;
		for (int i=0;i<NUM_GOODS;i++)
		{
			double[] priceGivenSize = new double[NUM_GOODS];
			for (int j=0;j<NUM_GOODS;j++)
			{
				priceGivenSize[j] = predictionMatrix[j][i];
			}
			Arrays.sort(priceGivenSize);
			int cost = 0;
			for (int j=0;j<=i;j++)
			{
				cost += priceGivenSize[j];
			}
			int surplus = cumulativeVal[i] - cost;
			
			if (surplus > maxSurplus)
			{
				maxSurplus = surplus;
				unitsToBuy = i;
			}
		}
		
		lastUnitsToBuy = unitsToBuy;
		
		for (int i=0;i<NUM_GOODS;i++)
		{
			if (currentWinning[i] == index)
			{
				currentPrediction[i] = (predictionMatrix[i][unitsToBuy] > (double)currentBid[i]) ? predictionMatrix[i][unitsToBuy] : (double)currentBid[i];
			}
			else
			{
				currentPrediction[i] = (predictionMatrix[i][unitsToBuy] > (double)currentBid[i] + 1) ? predictionMatrix[i][unitsToBuy] : (double)currentBid[i] + 1;
			}
		}
		
		// Initially play SB
		
		/*
		for (int i=0;i<NUM_GOODS;i++)
		{
			if (currentPrediction[i] == 0 || this.isSingleUnitDemand)
				currentPrediction[i] = (currentWinning[i] == index) ?  (double)currentBid[i] : (double)currentBid[i] + 1;
		}
		*/
		/*
		System.out.print("Agent " + index + "'s prediction : ");
		for (int i=0;i<NUM_GOODS;i++)
		{
			System.out.print(currentPrediction[i] + " ");
		}
		System.out.println();
		*/
		
		double max_surplus = Double.MIN_VALUE;
		BitSet maxSet = new BitSet();
		for (BitSet bs : bitVector)
		{
			if (bs.cardinality() == unitsToBuy + 1)
			{
				int value = typeDist.get(bs).intValue();
				
				double cost = 0.0;
				for (int j=0;j<bs.length();j++)
				{
					if (bs.get(j)) 
					{
						if (currentWinning[j] == index) cost += currentBid[j];
						else cost += currentBid[j] + 1;
					}
				}
				double surplus = (double)value - cost;
				if (surplus > max_surplus)
				{
					max_surplus = surplus;
					maxSet = bs;
				}
			}
		}
		for (int i=0;i<NUM_GOODS;i++)
		{
			if (maxSet.get(i) && max_surplus > 0) 
			{
				if (currentWinning[i] == index)
				{
					newBid[i] = currentBid[i];
				}
				else 
				{
					if (currentBid[i] < VALUE_UPPER_BOUND)
						newBid[i] = currentBid[i] + 1;
					else newBid[i] = VALUE_UPPER_BOUND;
				}
			}
			else newBid[i] = 0;
		}
		return newBid;
	}
	public boolean isSingleUnitDemand()
	{
		return this.isSingleUnitDemand;
	}
	private void checkSingleDemand()
	{
		int[] singleValue = new int[NUM_GOODS];
		for (BitSet bs : bitVector)
		{
			if (bs.cardinality() == 1)
			{
				singleValue[bs.length()-1] = this.typeDist.get(bs).intValue();
			}
		}
		for (BitSet bs : bitVector)
		{
			if (bs.cardinality() > 1)
			{
				int value = this.typeDist.get(bs).intValue();
				for (int j=0;j<bs.length();j++)
				{
					if (bs.get(j))
					{
						if (value > singleValue[j])
						{
							this.isSingleUnitDemand = false;
						}
					}
				}
			}
		}
	}
	
}
