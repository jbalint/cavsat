package com.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.beans.Stats;

public class ExecCommand {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

	public int executeCommand(String[] command, String outputfilename) {

		// StringBuffer output = new StringBuffer();

		// Process p;
		/*
		 * try { p = Runtime.getRuntime().exec(command); p.waitFor(); BufferedReader
		 * reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		 * 
		 * String line = ""; int index=0; while ((line = reader.readLine()) != null) {
		 * if(index++%50==0) System.out.println(index+" lines processed"); if
		 * (line.startsWith("v")) output.append(line + "\n"); }
		 * 
		 * } catch (Exception e) { e.printStackTrace(); }
		 */
		/*
		 * try { Runtime rt = Runtime.getRuntime(); Process proc = rt.exec(command);
		 * InputStream is = proc.getInputStream(); InputStreamReader isr = new
		 * InputStreamReader(is); int exitVal = proc.waitFor(); BufferedReader br = new
		 * BufferedReader(isr); String line = null; while ((line = br.readLine()) !=
		 * null) { System.out.println(line); if (line.startsWith("v"))
		 * output.append(line + "\n"); } System.out.println("Process exitValue: " +
		 * exitVal); } catch (Throwable t) { t.printStackTrace(); }
		 */
		ProcessBuilder pb = new ProcessBuilder(command);
		if (null != outputfilename) {
			// send standard output to a file
			pb.redirectOutput(new File(outputfilename));
			// merge standard error with standard output
			pb.redirectErrorStream(true);
		}
		try {
			Process p = pb.start();
			int exitVal = p.waitFor();
			return exitVal;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	public String readOutput(String filename) {
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				if (sCurrentLine.startsWith("v"))
					return sCurrentLine;
			}
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Stats isSAT(String filename, String solvername) {
		Stats stats = new Stats();
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String sCurrentLine;
			if (solvername.equalsIgnoreCase("MaxHS")) {
				while ((sCurrentLine = br.readLine()) != null) {
					if (sCurrentLine.startsWith("c Solved: Number")) {
						stats.setSolved(true);
						return stats;
						// return Integer.parseInt(sCurrentLine.split("=")[1].replaceAll(" ", "")) == 0;
					}
				}
			} else if (solvername.equalsIgnoreCase("Glucose") || solvername.equalsIgnoreCase("Lingeling")) {
				while ((sCurrentLine = br.readLine()) != null) {
					if (sCurrentLine.startsWith("s SATISFIABLE")) {
						stats.setSolved(true);
					} else if (sCurrentLine.startsWith("c conflicts")) {
						stats.setConflicts(Long.parseLong(sCurrentLine.split(":")[1].trim().split(" ")[0]));
					} else if (sCurrentLine.startsWith("c decisions")) {
						stats.setDecisions(Long.parseLong(sCurrentLine.split(":")[1].trim().split(" ")[0]));
					} else if (sCurrentLine.startsWith("c propagations")) {
						stats.setPropagations(Long.parseLong(sCurrentLine.split(":")[1].trim().split(" ")[0]));
					} else if (sCurrentLine.startsWith("c CPU time")) {
						stats.setTime(Double.parseDouble(sCurrentLine.split(":")[1].trim().split(" ")[0]));
					}
				}
				return stats;
			}
			return stats;
		} catch (IOException e) {
			e.printStackTrace();
			return stats;
		}
	}
}
