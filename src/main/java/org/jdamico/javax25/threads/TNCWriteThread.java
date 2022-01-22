package org.jdamico.javax25.threads;

abstract public class TNCWriteThread extends Thread
{
	final protected TNCQueue queue = new TNCQueue();

	volatile static int TNCSpeed = 1200;
	volatile static int TNCVias = 7;
	volatile static int TNCIMax = 256;

	/**
	 *  Pace packets so TNC is not overrun
	 * @param ax25Length length of ax.25 frame, including FEND's and FCS
	 */
	public void pace(TNCInterface.AX25Packet f)
	{
		if (TNCSpeed >= 8)
		{
			try {sleep((f.addresses.length*7+f.restOfPacket.length+4)*1000/(TNCSpeed>>3));}
			catch (Exception se){se.printStackTrace();}
		}
	}

	/**
	 * Override with your operating code.
	 */
	abstract public void run();
}
