package util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;

import com.opencsv.CSVReader;

/*all rights reserved - Karishma Jain
 * Licensed under MIT public license
 * kjkarishma785@gmail.com
 */
public class Worker extends Thread {
	private static final String MACADRESS = "00:1a:7d:11:0f:35";
	private static final String USERNAME = "sumitlubal@hotmail.com",
			PASSWORD = "Ankita@9992", DOMAIN = "";
	File workingDirectory, dataDirectory, backupDirectory;
	static String commandBluetoothView, commandLockStatus, commandLogonExpert;
	int fileCount = 1;
	CSVReader reader = null;
	boolean isDeviceConnected = false;
	RegisterWorkspaceEvent eventProcess;
	private boolean lockAllUser;

	public Worker() {
		setPars();
		eventProcess = new RegisterWorkspaceEvent();
		eventProcess.start();
		storeCredentials();
	}

	// /le.exe setcredentials username [password] [domain]
	private void storeCredentials() {
		System.out.println("Adding credentials in logon expert");
		startProcess(new String[] { commandLogonExpert, "/setcredentials",
				USERNAME, PASSWORD, DOMAIN });
		System.out.println("Credentials added");
	}

	public void run() {
		// clear log folder
		clearLogFolder();
		while (true) {
			try {
				// start bluetooth.exe and wait for it to complete and dump//
				// output
				// to tmpLog
				String dumpFilePath = dataDirectory + "/dump" + fileCount
						+ ".log";
				startProcess(new String[] { commandBluetoothView, "/scomma",
						dumpFilePath });
				// Analyze log file
				reader = new CSVReader(new FileReader(dumpFilePath));
				List<String[]> myEntries = reader.readAll();
				reader.close();
				ArrayList<Device> devices = getDevicesFromLog(myEntries);

				for (Device d : devices) {
					if (d.isConnected.equalsIgnoreCase("yes")
							&& d.macAddress.equalsIgnoreCase(MACADRESS)) {
						isDeviceConnected = true;
						break;
					} else {
						isDeviceConnected = false;
					}
				}
				// check computer lock unlock status
				if (eventProcess.setOrGetValue(true, false)
						&& isDeviceConnected) {
					System.out
							.println("Device connected & com is still locked! Now unlocking computer");
					performUnlock();
				} else if (!isDeviceConnected) {
					System.out
							.println("Device disconnected & computer is unlocked! Now locking computer");
					performLock();
				} else {
					System.out.println("Nothing to do as:");
					System.out.println("Computer lock status: "
							+ eventProcess.setOrGetValue(true, false));
					System.out.println("Device connection status:"
							+ isDeviceConnected);
				}
				// do operation of lock and unlock
			} catch (Exception e) {
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e1) {
						System.out.println("Failed to close reader");
						e1.printStackTrace();
					}
				e.printStackTrace();
			}
		}
	}

	// /logon [username password] [domain]
	private void performUnlock() {
		System.out.println("In unlock method");
		startProcess(new String[] { commandLogonExpert, "/logon", USERNAME,
				PASSWORD, DOMAIN });
		System.out.println("Computer unlock complete!");
	}

	// logoff [username] or logoff *
	private void performLock() {
		System.out.println("In lock method");
		if (!lockAllUser)
			startProcess(new String[] { "rundll32.exe", "user32.dll,",
					"LockWorkStation" });
		// System.out.println("fake lock");
		else {
			startProcess(new String[] { commandLogonExpert, "/logoff", "*" });
		}
		System.out.println("Computer Lock Complete!");
	}

	private ArrayList<Device> getDevicesFromLog(
			List<String[]> myEntries) {
		ArrayList<Device> devices = new ArrayList<Device>();
		for (String[] line : myEntries) {
			Device d = new Device();
			for (int i = 0; i < line.length; i++) {
				d.deviceName = line[0];
				d.deviceDescription = line[1];
				d.macAddress = line[2];
				d.majorType = line[3];
				d.minorType = line[4];
				d.firstDetected = line[5];
				d.lastDetected = line[6];
				d.detectionCounter = line[7];
				d.noDetectionCounter = line[8];
				d.percDetection = line[9];
				d.isConnected = line[10];
				d.isRemembered = line[11];
				d.isAuthenticated = line[12];
				d.companyName = line[13];
				d.connectionResult = line[14];
			}
			devices.add(d);
		}
		return devices;
	}

	public static String startProcess(String[] command) {
		String output = new String();
		System.out.println("firing command:");
		for (int i = 0; i < command.length; i++) {
			System.out.print(command[i] + " ");
		}
		Scanner sc = null;
		try {
			Process process = new ProcessBuilder(command).start();
			sc = new Scanner(process.getInputStream());
			while (sc.hasNext() && process.exitValue() != 0) {
				output += " " + sc.nextLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (sc != null)
			sc.close();
		return output;
	}

	/*
	 * check if folder exists No - create one yes - move all data to backup then
	 * clear the folder for next use
	 */
	private void clearLogFolder() {
		if (!backupDirectory.exists()) {
			try {
				FileUtils.forceMkdir(backupDirectory);
			} catch (IOException e) {
				System.out
						.println("Unable to create folder:" + backupDirectory);
				e.printStackTrace();
			}
		}
		if (dataDirectory.exists()) {
			try {
				FileUtils.copyDirectory(dataDirectory, backupDirectory);
				System.out.println("data backed up");
				FileUtils.cleanDirectory(dataDirectory);
				System.out.println("data directory cleaned");
			} catch (IOException e) {
				System.out.println("Unable to copy/detele data folder : "
						+ dataDirectory);
				e.printStackTrace();
			}
		} else {
			try {
				FileUtils.forceMkdir(dataDirectory);
			} catch (IOException e) {
				System.out.println("Unable to create data folder : "
						+ dataDirectory);
				e.printStackTrace();
			}
		}
	}

	private void setPars() {
		lockAllUser = false;
		workingDirectory = new File(System.getProperty("user.dir"));
		dataDirectory = new File(workingDirectory.getAbsolutePath()
				+ "/appdata");
		backupDirectory = new File(workingDirectory.getAbsolutePath()
				+ "/backup/" + System.currentTimeMillis());
		System.out.println(dataDirectory.getAbsolutePath());
		String binDirPath = workingDirectory.getAbsolutePath() + "/bin";
		commandBluetoothView = binDirPath + "/" + "bv.exe";
		commandLockStatus = binDirPath + "/" + "ls.exe";
		commandLogonExpert = binDirPath + "/" + "le.exe";
	}
}
