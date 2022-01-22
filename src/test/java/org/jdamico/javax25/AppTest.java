package org.jdamico.javax25;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.jdamico.javax25.ax25.Afsk1200Modulator;
import org.jdamico.javax25.ax25.Afsk1200MultiDemodulator;
import org.jdamico.javax25.ax25.Packet;
import org.jdamico.javax25.ax25.PacketDemodulator;
import org.jdamico.javax25.ax25.PacketHandler;
import org.jdamico.javax25.soundcard.Soundcard;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import lombok.extern.slf4j.Slf4j;

/**
 * Unit test for simple App.
 */
@Slf4j
public class AppTest extends TestCase {
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	/**
	 * Rigourous Test :-)
	 */
	public void testApp() {
		Soundcard.enumerate();

		/////////////////////////////////////////
		// create test packet to transmit
		/////////////////////////////////////////

		String callsign = "NOCALL";
		System.out.println("Callsign in test packet is: " + callsign);

		Packet packet = new Packet("APRS", callsign, new String[] { "WIDE1-1", "WIDE2-2" }, Packet.AX25_CONTROL_APRS,
				Packet.AX25_PROTOCOL_NO_LAYER_3, ">Java Modem Test Packet".getBytes());

		System.out.println(packet.toString());

		/////////////////////////////////////////
		// set up modulator / demodulator
		/////////////////////////////////////////

		Properties properties = System.getProperties();

		int rate = 48000;
//		int filter_length = 32;

		PacketHandler packetHandlerStdout = new PacketHandlerStdout();
		Afsk1200Modulator modulator = null;
		PacketDemodulator demodulator = null;
		try {
			demodulator = new Afsk1200MultiDemodulator(rate, packetHandlerStdout);
			modulator = new Afsk1200Modulator(rate);
		} catch (Exception e) {
			System.out.println("Exception trying to create an Afsk1200 object: " + e.getMessage());
		}

		/////////////////////////////////////////
		// write a test packet to a text file
		/////////////////////////////////////////

		String fout = System.getProperty("java.io.tmpdir") + File.separator + "japrs.txt";
		File packetFile = new File(fout);
		if (fout != null) {
			System.out.printf("Writing transmit packets to <%s>\n", fout);
			FileOutputStream f = null;
			PrintStream printStream = null;
			try {
				f = new FileOutputStream(packetFile);
				printStream = new PrintStream(f);
			} catch (FileNotFoundException fnfe) {
				System.err.println("File " + fout + " not found: " + fnfe.getMessage());
			}
			modulator.prepareToTransmit(packet);
			int n;
			float[] tx_samples = modulator.getTxSamplesBuffer();
			while ((n = modulator.getSamples()) > 0) {
				for (int i = 0; i < n; i++)
					printStream.printf("%09e\n", tx_samples[i]);
			}
			printStream.close();
		}
		// ptl: what is the file used for?

		/////////////////////////////////////////
		// prepare soundcard to transmit and receive tones
		/////////////////////////////////////////

		// Use a patch cable to connect headphone jack to microphone
		// There are linux solutions, too: pipe parec to virtual microphone
		// https://stackoverflow.com/questions/51128790/linux-pipe-audio-to-virtual-microphone-using-pactl
		// https://askubuntu.com/questions/229352/how-to-record-output-to-speakers

		String audioInputFile = System.getProperty("java.io.tmpdir") + File.separator + "packet_test.ax25";
		String microphone = "default [default]"; // microphone
		String speakers = "default [default]"; // speakers

		int buffer_size = -1;
		try {
			// our default buffer_size is 100ms
			buffer_size = Integer.parseInt(properties.getProperty("latency", "100").trim());
		} catch (Exception e) {
			System.err.println("Exception parsing buffersize " + e.toString());
		}

		Soundcard soundcard = null;
		try {
			soundcard = new Soundcard(rate, audioInputFile, speakers, buffer_size, demodulator, modulator);
		} catch (Exception e) {
			log.error(e.toString(), e);
		}

		if (properties.containsKey("audio-level")) {
			soundcard.displayAudioLevel();
		}

		/////////////////////////////////////////
		// start recording speakers
		/////////////////////////////////////////
		RecordLinuxSpeakers recordLinuxSpeakers = new RecordLinuxSpeakers();
		try {
			recordLinuxSpeakers.startRecording(audioInputFile);
		} catch (Exception e) {
			log.error(e.toString(), e);
		}

		/////////////////////////////////////////
		// transmit flags
		/////////////////////////////////////////

		int tones_duration = -1; // in seconds
		try {
			tones_duration = Integer.parseInt(properties.getProperty("tones", "-1").trim());
		} catch (Exception e) {
			System.err.println("Exception parsing tones " + e.toString());
		}
		if (tones_duration > 0) {
			modulator.prepareToTransmitFlags(tones_duration);
			soundcard.transmit();
		}

		/////////////////////////////////////////
		// transmit data
		/////////////////////////////////////////

		if (speakers != null) {
			modulator.prepareToTransmit(packet);
			System.out.printf("Start transmitter\n");
			soundcard.transmit();
			System.out.printf("Stop transmitter\n");
			recordLinuxSpeakers.close();

//			int n;
//			while ((n = ae.afsk.getSamples()) > 0){
//			ae.afsk.addSamples(Arrays.copyOf(tx_samples, n));
//			}

		}

		/////////////////////////////////////////
		// soundcard receive from audioFile
		/////////////////////////////////////////
		soundcard.setInputDevice(audioInputFile);
		soundcard.receive();
		
		
//		/////////////////////////////////////////
//		// listen on microphone
//		/////////////////////////////////////////
//		Microphone microphone = new Microphone(System.getProperties(), "microphone_main.ax25");
//
//		try {
//			microphone.start();
//			Thread.sleep(10000);
//		} catch (Exception e) {
//			log.error(e.toString(), e);
//		}
//
//		/////////////////////////////////////////
//		// play file
//		/////////////////////////////////////////
//		PlayLinuxMicrophone playLinuxMicrophone = new PlayLinuxMicrophone() ;
//		try {
//			playLinuxMicrophone.startPlaying(recordedSounds);
//		} catch(Exception e) {
//			log.error(e.toString(),e );
//		} finally {
//			if (null != playLinuxMicrophone) {
//				playLinuxMicrophone.close() ;
//			}
//			playLinuxMicrophone = null ;
//		}
//
//		/////////////////////////////////////////
//		// stop listening
//		/////////////////////////////////////////
//		try {
//			microphone.interrupt();
//		} catch(Exception e ) {
//			log.error(e.toString(), e);
//		} finally {
//			if (null != microphone) {
//				microphone.close() ;
//				microphone = null ;
//			}
//		}

	}

//	public void testNativeLibrary() {
//		// rxtxSerial
//		SerialTransmitController.enumerate();
//	}

}
