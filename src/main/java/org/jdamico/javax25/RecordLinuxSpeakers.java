package org.jdamico.javax25;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RecordLinuxSpeakers {

	Process process = null;

	public RecordLinuxSpeakers() {
	}

	public void startRecording(String outputFileName) throws IOException {
		close();

		File outputFile = new File(outputFileName);

		if (outputFile.exists()) {
			log.warn("Output file '{}' exists.  It will be overwritten.", outputFile.getCanonicalPath());
		}

		String SPEAKER_MONITOR = System.getenv("SPEAKER_MONITOR");

		// assumes linux Pulse Audio
		String command = "/usr/bin/parec";

		// https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ProcessBuilder.html
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command, "-d", SPEAKER_MONITOR);
			processBuilder.redirectOutput(outputFile);
			process = processBuilder.start();

		} catch (IOException e) {
			log.error("Do you need to install {}?  sudo apt-get install pulseaudio-utils", command);
			log.error(e.toString(), e);
			throw e;
		}

		if (process.isAlive()) {
			log.debug("process is alive");
		} else {
			log.debug("process is not alive");
		}

	}

	public void close() {
		log.info("entering close") ;
		if (null != process && process.isAlive()) {
			log.info("destroying process") ;
			process.destroy();
			process = null;
		}
		log.info("leaving close") ;

	}

}
