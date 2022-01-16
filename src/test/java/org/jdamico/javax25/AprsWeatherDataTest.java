package org.jdamico.javax25;

import java.util.Properties;

import org.jdamico.javax25.ax25.Afsk1200Modulator;
import org.jdamico.javax25.ax25.Afsk1200MultiDemodulator;
import org.jdamico.javax25.ax25.Packet;
import org.jdamico.javax25.ax25.PacketDemodulator;
import org.jdamico.javax25.radiocontrol.TransmitController;
import org.jdamico.javax25.soundcard.Soundcard;
import org.junit.Before;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AprsWeatherDataTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		String callsign = "NOCALL";
		System.out.println("Callsign in test packet is: " + callsign);

		String hms_position = "092122z2332.53S/04645.51W";
		String weather_data = "_220/004g005t-07r000p000P000h50b09900xSCI";

		String complete_weather_data = hms_position + weather_data;

		Packet packet = new Packet("APRS", callsign, new String[] { "WIDE1-1", "WIDE2-2" }, Packet.AX25_CONTROL_APRS,
				Packet.AX25_PROTOCOL_NO_LAYER_3, complete_weather_data.getBytes());

		System.out.println(packet);

		Soundcard.enumerate();

		Properties properties = System.getProperties();

		int rate = 48000;
		int filter_length = 32;

		PacketHandlerImpl packetHandlerImpl = new PacketHandlerImpl();
		Afsk1200Modulator modulator = null;
		PacketDemodulator demodulator = null;
		try {
			demodulator = new Afsk1200MultiDemodulator(rate, packetHandlerImpl);
			modulator = new Afsk1200Modulator(rate);
		} catch (Exception e) {
			System.out.println("Exception trying to create an Afsk1200 object: " + e.getMessage());
		}

		String input = null;
		// String output = "PulseAudio Mixer";
		String output = "default";

		int buffer_size = -1;
		try {
			// our default is 100ms
			buffer_size = Integer.parseInt(properties.getProperty("latency", "100").trim());
		} catch (Exception e) {
			System.err.println("Exception parsing buffersize " + e.toString());
		}

		Soundcard soundcard = null;

		try {
			soundcard = new Soundcard(rate, input, output, buffer_size, demodulator, modulator);
		} catch (Exception e) {
			log.error(e.toString(), e) ;
		}

		if (properties.containsKey("audio-level")) {
			soundcard.displayAudioLevel();
		}

		/*** generate test tones and exit ***/

		TransmitController transmitController = null;

		int tones_duration = -1; // in seconds
		try {
			tones_duration = Integer.parseInt(properties.getProperty("tones", "-1").trim());
		} catch (Exception e) {
			System.err.println("Exception parsing tones " + e.toString());
		}
		if (tones_duration > 0) {
			// soundcard.openSoundOutput(output);
			modulator.prepareToTransmitFlags(tones_duration);
			soundcard.transmit();
		}

		if (output != null) {
			// soundcard.openSoundOutput(output);
			modulator.prepareToTransmit(packet);
			System.out.printf("Start transmitter\n");
			// soundcard.startTransmitter();
			if (transmitController != null) {
				transmitController.startTransmitter();
			}
			soundcard.transmit();
			System.out.printf("Stop transmitter\n");
			if (transmitController != null) {
				transmitController.stopTransmitter();
			}
			if (transmitController != null) {
				transmitController.close();
			}
			// if (transmitController != null) transmitController.stopTransmitter());
			// soundcard.stopTransmitter();
			// int n;
			// while ((n = ae.afsk.getSamples()) > 0){
			// ae.afsk.addSamples(Arrays.copyOf(tx_samples, n));
			// }
		}

	}

}
