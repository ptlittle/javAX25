package org.jdamico.javax25.threads;

import org.jdamico.javax25.TNCInterface;
import org.jdamico.javax25.ax25.PacketModulator;
import org.jdamico.javax25.radiocontrol.TransmitController;
import org.jdamico.javax25.soundcard.Soundcard;

public final class SoundcardWriteThread extends TNCWriteThread {
	//private sivantoledo.ax25.Afsk1200 afsk;
	private org.jdamico.javax25.soundcard.Soundcard soundcard;

	private org.jdamico.javax25.ax25.PacketDemodulator demodulator;
	private org.jdamico.javax25.ax25.PacketModulator modulator;

	private double persistence; // CSMA access probability
	private int slot_time;      // wait between CSMA attemps in 10ms units

	private TransmitController ptt;

	public SoundcardWriteThread(//sivantoledo.ax25.Afsk1200 afsk,
			org.jdamico.javax25.ax25.PacketDemodulator demodulator,
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
