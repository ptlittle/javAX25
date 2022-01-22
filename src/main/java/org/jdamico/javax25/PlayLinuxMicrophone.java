package org.jdamico.javax25;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlayLinuxMicrophone {

	Process process = null;

	public PlayLinuxMicrophone() {
		log.info("entering constructor");
		log.info("leaving constructor");
	}

	public void startPlaying(String inputFileName) throws IOException {
		log.info("entering startPlaying {}", inputFileName);

		File inputFile = new File(System.getProperty("java.io.tmpdir") + File.separator + inputFileName);

		if (! inputFile.exists()) {
			throw new IOException(String.format("Input file '%s' does not exist.", inputFile.getCanonicalPath()));
		}

		String command = "/usr/bin/ffmpeg";

		// https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ProcessBuilder.html
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command, "-re","-i",inputFile.getCanonicalPath(),"-f","s16le","-ar","16000","-ac","1","-");
			processBuilder.redirectOutput(inputFile);
			process = processBuilder.start();
		} catch (IOException e) {
			log.error("Do you need to install {}?  sudo apt-get install pulseaudio-utils", command);
			log.error(e.toString(),e);
			throw e ;
		}

		log.info("leaving startPlaying {}", inputFileName);
		
	}
	
	public void close() {
		log.info("entering close");
		if (null != process && process.isAlive()) {
			process.destroy();
			process = null;
		}
		log.info("leaving close");
	}
	

}
