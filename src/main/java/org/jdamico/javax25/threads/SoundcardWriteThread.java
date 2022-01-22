package org.jdamico.javax25.threads;

import org.jdamico.javax25.ax25.Packet;
import org.jdamico.javax25.ax25.PacketDemodulator;
import org.jdamico.javax25.ax25.PacketModulator;
import org.jdamico.javax25.ax25.Soundcard;
import org.jdamico.javax25.radiocontrol.TransmitController;
import org.jdamico.javax25.threads.TNCInterface.AX25Packet;

public final class SoundcardWriteThread extends TNCWriteThread {
	protected Soundcard soundcard;

	protected PacketDemodulator demodulator;
	protected PacketModulator modulator;

	protected double persistence; // CSMA access probability
	protected int slot_time;      // wait between CSMA attemps in 10ms units

	protected TransmitController ptt;

	public SoundcardWriteThread(
			PacketDemodulator demodulator,
			PacketModulator modulator,
			Soundcard soundcard,
			double persistence, int slot_time,
			TransmitController ptt
      ) {
		super();
		this.ptt = ptt;
		//this.afsk = afsk;
		this.demodulator = demodulator;
		this.modulator = modulator;
		this.soundcard   = soundcard;
		this.persistence = persistence;
		this.slot_time   = slot_time;
		setName("Soundcard Write Thread");
		start();
	}

	public void run() {
		// if (output == null) return;
		for (;;) {
			TNCInterface.AX25Packet outline = (TNCInterface.AX25Packet) queue.getFromQueue();
			if (outline == null)
				return;

			byte[] newpacket =  outline.getAX25Packet();
			if (newpacket == null) {
				yield();
				continue;
			}
			try {
				//output.write(newpacket);
				// does the packet include the CRC, or do we need to add it?
				// the contstructor of Packet does add the CRC, so hopefully it it not 
				// included.
				
				while (demodulator.dcd()) yield(); // wait for a channel that is not busy
				while (Math.random() > persistence) {
					try { sleep(10*slot_time); } catch (InterruptedException ie) {}
					while (demodulator.dcd()) yield(); // wait for a channel that is not busy
				}
				
				modulator.prepareToTransmit(new org.jdamico.javax25.ax25.Packet(newpacket));
				// key PTT
				if (ptt != null) ptt.startTransmitter();
				soundcard.transmit();
				// unkey PTT; we have drained the audio buffer
				if (ptt != null) ptt.stopTransmitter();
				
				pace(outline);
			} catch (Exception e) {
				queue.putOnQueue(null);
			}
			yield();
		}
	}
}
