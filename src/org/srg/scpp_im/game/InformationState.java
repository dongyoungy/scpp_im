package org.srg.scpp_im.game;

public class InformationState extends GameSetting {
	
	private int numGoods;
	private int round;
	private double[] bidPrice;
	private double[][] bids;
	private int[] bidWinning;
	
	public InformationState(int numGoods)
	{
		this.numGoods = numGoods;
		bids = new double[NUM_GOODS][HIERARCHICAL_REDUCTION_LEVEL * NUM_AGENT];
		bidPrice = new double[numGoods];
		bidWinning = new int[numGoods];
	}
	
	public double[] getCurrentBidPrice()
	{
		return bidPrice;
	}
	
	public int[] getCurrentBidWinning()
	{
		return bidWinning;
	}
	
	public void setBidPrice(double[] newBidPrice)
	{
		if (newBidPrice.length != numGoods) return;
		bidPrice = newBidPrice;
	}
	
	public void setBidWinning(int[] newBidWinning)
	{
		if (newBidWinning.length != numGoods) return;
		bidWinning = newBidWinning;
	}
	
	public void setBids(double[][] newBids)
	{
		bids = newBids;
	}
	
	public void setRound(int r)
	{
		round = r;
	}
	public int getRound()
	{
		return round;
	}
}
