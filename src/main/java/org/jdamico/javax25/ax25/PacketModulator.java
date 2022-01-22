package org.jdamico.javax25.ax25;

public interface PacketModulator extends SoundcardProducer {
	public float[] getTxSamplesBuffer();
	public int getSamples();
	public void prepareToTransmit(Packet p);
	
}
