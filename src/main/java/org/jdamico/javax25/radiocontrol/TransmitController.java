package org.jdamico.javax25.radiocontrol;

// Receive-Transmit control of half-duplex
public interface TransmitController {
	public void startTransmitter();
	public void stopTransmitter();
	public void close();
}
