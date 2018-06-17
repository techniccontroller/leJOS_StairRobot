package com.techniccontroller.main;

import java.io.DataInputStream;
import java.io.IOException;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.MotorPort;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.TouchSensor;
import lejos.util.Delay;
import lejos.util.TextMenu;

public class Main {

	// Attribute
	private static NXTRegulatedMotor motorDrive; 	// Motor um zu Fahren
	private static NXTRegulatedMotor motorFront; 	// Motor für vordere
													// Höhenverstellung und das
													// Lenken
	private static NXTRegulatedMotor motorBack; 	// Motor für hintere
													// Höhenverstellung
	private static TouchSensor touchFront; 			// Referenz-TouchSensor für
													// Kalibrierung des vorderen Motors
	private static TouchSensor touchBack; 			// Referenz-Touchsenosr für
													// Kalibrierung des hinteren Motors
	private static UltrasonicSensor sonicFront; 	// Abstandssensor zur Erkennung
													// einer Treppe
	
	private static int speed_Drive = 100; 			// Motorgeschwindigkeit beim Fahren
	private static int speed_Steer = 100; 			// Motorgeschwindigkeit beim Lenken
	private static int speed_Heben = 700; 			// Motorgeschwindigkeit beim Heben
	private static int speed_DriveSlow = 30; 		// Motorgeschwindigkeit beim langsamen
													// Fahren
	private static Thread startautomatik;
	private static Thread suchautomatik;
	private static Thread treppenautomatik;
	private static Thread livedaten;
	

	public static void main(String[] args) throws IOException, InterruptedException {
		Boolean isrunning = true;
		init();
		// main loop
		while (true) {

			LCD.drawString("waiting", 0, 0);

			LCD.refresh();

			// Listen for incoming connection

			NXTConnection btc = Bluetooth.waitForConnection();

			btc.setIOMode(NXTConnection.RAW);

			LCD.clear();

			LCD.drawString("connected", 0, 0);
			System.out.println("connected");
			LCD.refresh();

			// The InputStream for read data

			DataInputStream dis = btc.openDataInputStream();

			// loop for read data
			int code = 0;
			
			livedaten();
			
			while (isrunning) {

				Byte n = -1;
				n = dis.readByte();

				code = (int) n;
				dis.close();
				// LCD.drawInt(code, 0, 1);

				LCD.clear();

				switch (code) {
				case 0:
					LCD.drawString("Stop", 0, 5);
					stop();
					break;
				case 1:
					LCD.drawString("Vor", 0, 5);
					driveforward();
					break;
				case 2:
					LCD.drawString("Zuruck", 0, 5);
					drivebackward();
					break;
				case 3:
					LCD.drawString("links", 0, 5);
					steerleft();
					break;
				case 4:
					LCD.drawString("rechts", 0, 5);
					steerright();
					break;
				case 5:
					LCD.drawString("Heben Vorne", 0, 5);
					hebenFront();
					break;
				case 6:
					LCD.drawString("Senken Vorne", 0, 5);
					senkenFront();
					break;
				case 7:
					LCD.drawString("Heben Hinten", 0, 5);
					hebenBack();
					break;
				case 8:
					LCD.drawString("Senken Hinten", 0, 5);
					senkenBack();
					break;
				case 10:
					LCD.drawString("NotStop", 0, 5);
					notstop();
					break;
				case 15:
					LCD.drawString("Start", 0, 5);
					Treppesearch();
					break;
				case 20:
					LCD.drawString("Startpos U", 0, 5);
					startPosition(3);
					break;
				case 21:
					LCD.drawString("Startpos M", 0, 5);
					startPosition(2);
					break;
				case 22:
					LCD.drawString("Startpos O", 0, 5);
					startPosition(1);
					break;
				default:
					break;
				}

			}
			dis.close();
			Thread.sleep(100); // wait for data to drain
			LCD.clear();
			LCD.drawString("closing", 0, 0);
			LCD.refresh();
			btc.close();
			LCD.clear();

		}

	}

	// Initialisierung aller Komponenten und Variablen
	public static void init() {
		touchFront = new TouchSensor(SensorPort.S2);
		touchBack = new TouchSensor(SensorPort.S1);
		sonicFront = new UltrasonicSensor(SensorPort.S3);
		motorBack = new NXTRegulatedMotor(MotorPort.A);
		motorFront = new NXTRegulatedMotor(MotorPort.C);
		motorDrive = new NXTRegulatedMotor(MotorPort.B);
	}

	// Methode damit das Treppenfahrzeug vorwärts fährt
	public static void driveforward() {
		LCD.drawString("Vor", 0, 2);
		motorDrive.setSpeed(speed_Drive);
		LCD.drawString("Vor", 0, 3);
		motorDrive.forward();
		LCD.drawString("Vor", 0, 4);
	}

	// Methode damit das Treppenfahrzeug rückwärts fährt
	public static void drivebackward() {
		motorDrive.setSpeed(speed_Drive);
		motorDrive.backward();
	}

	// Methode damit das Treppenfahrzeug nach rechts lenkt
	public static void steerright() {
		if (touchFront.isPressed() && touchBack.isPressed()) {
			motorFront.setSpeed(speed_Steer);
			motorFront.backward();
		}
	}

	// Methode damit das Treppenfahrzeug nach links lenkt
	public static void steerleft() {
		if (touchFront.isPressed() && touchBack.isPressed()) {
			motorFront.setSpeed(speed_Steer);
			motorFront.forward();
		}
	}

	// Methode für das Heben der vorderen Räder
	public static void hebenFront() {
		if(!touchBack.isPressed()){
			motorFront.setSpeed(speed_Heben);
			if (touchFront.isPressed()) {
				motorFront.rotate(100);
				motorFront.rotate(-100);
			}
			motorFront.forward();
		}
		
	}

	// Methode für das Senken der vorderen Räder
	public static void senkenFront() {
		if (!touchBack.isPressed()) {
			motorFront.setSpeed(speed_Heben);
			motorFront.backward();
		}
	}

	// Methode für das Heben der hinteren Räder
	public static void hebenBack() {
		motorBack.setSpeed(speed_Heben);
		motorBack.forward();
	}

	// Methode für das Senken der hinteren Räder
	public static void senkenBack() {
		motorBack.setSpeed(speed_Heben);
		motorBack.backward();
	}

	// Methode um alle Motoren zu stoppen
	public static void stop() {
		motorFront.setSpeed(speed_Steer);
		motorFront.stop();
		motorDrive.setSpeed(speed_Drive);
		motorDrive.stop();
		motorBack.setSpeed(speed_Heben);
		motorBack.stop();
	}

	// Methoden zum Beenden aller Aktivitäten
	public static void notstop() {
		if (startautomatik != null) {
			startautomatik = null;
		}
		if (treppenautomatik != null) {
			treppenautomatik = null;
		}
		if (suchautomatik != null) {
			suchautomatik = null;
		}
		if (livedaten != null) {
			livedaten = null;
		}
		motorFront.setSpeed(speed_Steer);
		motorFront.stop();
		motorDrive.setSpeed(speed_Drive);
		motorDrive.stop();
		motorBack.setSpeed(speed_Heben);
		motorBack.stop();
	}

	// Motoren fahren in Startposition
	public static void startPosition(final int posStart) {

		startautomatik = new Thread() {
			public void run() {
				if (touchBack.isPressed()) {
					motorBack.backward();
					while (touchBack.isPressed())
						;
					Delay.msDelay(700);
					motorBack.stop();
				}

				// Startposition einnehmen
				motorFront.setSpeed(700);
				motorFront.backward();
				while (!touchFront.isPressed())
					;
				motorFront.stop();
				motorFront.resetTachoCount();
				motorBack.resetTachoCount();
				motorBack.setSpeed(700);
				
				if(posStart == 2 || posStart ==3){
					motorBack.forward();
					while (!touchBack.isPressed())
						;
					motorBack.rotate(736);
				}
				
				motorBack.backward();
				while (!touchBack.isPressed())
					;
				motorBack.rotate(-736);
				motorBack.resetTachoCount();
				Delay.msDelay(2000);
			}
		};
		startautomatik.start();

	}

	// Treppenfahrzeug fährt bis zur Treppe
	public static void Treppesearch() {
		suchautomatik = new Thread() {
			public void run() {
				motorDrive.setSpeed(100);
				motorDrive.forward();
				while (sonicFront.getDistance() > 12 && !Button.ESCAPE.isDown()) {
					Delay.msDelay(100);
				}
				motorDrive.stop();
				Treppe();
			}
		};
		suchautomatik.start();
	}

	// Diese Methode gibt liveauskunft über die TachoCounts
	public static void livedaten() {
		livedaten = new Thread() {
			public void run() {
				while (true) {
					try {
						LCD.clear();
						LCD.drawString("MoD:", 0, 1);
						LCD.drawString("MoF:", 0, 2);
						LCD.drawString("MoB:", 0, 3);
						LCD.drawInt(motorDrive.getTachoCount(), 6, 1);
						LCD.drawInt(motorFront.getTachoCount(), 6, 2);
						LCD.drawInt(motorBack.getTachoCount(), 6, 3);
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		livedaten.start();
	}

	// Treppenfahrzeug beginnt die Treppe zu erklimmen
	public static void Treppe() {
		treppenautomatik = new Thread() {
			public void run() {
				motorBack.rotateTo(2200);
				motorFront.setSpeed(speed_Heben);
				if (touchFront.isPressed()) {
					motorFront.rotateTo(150);
					Delay.msDelay(500);
					motorFront.rotateTo(0);
					Delay.msDelay(1000);
				}
				motorFront.rotateTo(3200);
				motorDrive.setSpeed(speed_DriveSlow);
				motorDrive.forward();
				while (sonicFront.getDistance() > 7) {
					Delay.msDelay(100);
				}
				motorDrive.stop();
				motorFront.rotateTo(13800);
				motorDrive.resetTachoCount();
				motorDrive.rotate(235);

				motorFront.rotateTo(2250, true);
				motorBack.rotateTo(-9300);
				motorDrive.rotate(250);
				motorBack.forward();
				while (!touchBack.isPressed())
					;
				motorBack.rotate(736);
				motorBack.backward();
				while (!touchBack.isPressed())
					;
				motorBack.rotate(-736);
				motorBack.resetTachoCount();
				motorBack.rotateTo(2200);
				if (sonicFront.getDistance() < 20) {
					Treppesearch();
				}
			}
		};
		treppenautomatik.start();
	}

}
