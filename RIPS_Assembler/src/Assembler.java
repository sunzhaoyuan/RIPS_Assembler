import java.util.ArrayList;
import java.util.HashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * This is an assembler for RIPS.
 *
 * @author sunz1. Created Feb 8, 2017.
 */
public class Assembler {
	final Integer OPCODE_LENGTH = 5;
	final Integer NUM_OF_INST = 26;
	final String[] NAMES = { "add", "addi", "sub", "and", "or", "sll", "srl", "sra", "load", "store", "sstor", "lacc",
			"loadsp", "storesp", "addsp", "ja", "jal", "bez", "bnez", "slt", "sltsp", "addmsp", "submsp", "andi", "ori",
			"orui", "loadi", "input", "stemp", "itemp" };

	HashMap<String, String> inst;

	// Test Plan:
	// PASS: Test Only Normal Operations
	// PASS: Test Only branch
	// PASS: Test mixed branch and NorOp
	// PASS: Test jal
	// TODO: Test jump
	// TODO: Test Invalid Operation
	// TODO: Test input larger than 11-bit

	/**
	 * main class, it generates final output
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// read the file
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Hi. Whatup");
		System.out.println("Attention: please make sure you have prefix for all numbers.");
		System.out.println("What is the file path? ");
		String filePath = br.readLine();
		ArrayList<String> file = new ArrayList<>();
		file = fileReader(filePath);
		// write in files, DONE: it worked
		try (PrintWriter writer = new PrintWriter(filePath.replaceAll(".txt", "") + "_Output" + ".txt")) {
			for (String str : file) { // write output in format
				writer.println(str);
			}
		}
		System.out.println("system terminalted.");
		// convert the binary (Assembler takes care of it)

		// DONE: WHAT IF THERE ARE SEVERAL SPACES BETWEEN INST AND VALUE
		// DONE: WHAT ABOUT BRANCH AND LABEL -> HashMap(String, ForHashMap)
		// TODO: WHAT ABOUT THEY HAVE SAME MEMORY ADDR -> What???
		// TODO: WHAT ABOUT JUMP?? -> same logic as brach -> slightly
		// different(ja)
		// DONE: NEED TO BE IN FORMATTED???
		// DONE: write in file, not working fixed
		// TODO: ??? I forgot. Feb 14
	}

	/*
	 * put every instructions into HashMap in right order; initialize
	 * everything.
	 */
	private Assembler() {
		this.inst = new HashMap<>();
		this.inst.put("noop", "00000"); // add noop
		for (int i = 1; i < NUM_OF_INST; i++) {
			String opcode = Integer.toBinaryString(i);
			if (opcode.length() < OPCODE_LENGTH) { // make it length of 5
				int generation = (OPCODE_LENGTH - opcode.length());
				opcode = new String(new char[generation]).replace("\0", "0") + opcode;
			}
			this.inst.put(NAMES[i - 1], opcode);
		}
	}

	/*
	 * it reads the file, and return a ArrayList that contains the final outputs
	 * 
	 * 01101 111 1111 1111
	 * 
	 */
	private static ArrayList<String> fileReader(String file) {
		File inputFile = new File(file);
		Assembler assembler = new Assembler();
		ArrayList<String> outputArray = new ArrayList<>();
		HashMap<String, ForBranchMap> branchMap = new HashMap<>();
		try {
			@SuppressWarnings("resource")
			BufferedReader bReader = new BufferedReader(new FileReader(inputFile));
			String line = bReader.readLine();
			while (line != null) {
				// deal with hashMap: TODO:extremely inefficient, it's ok.
				if (!branchMap.isEmpty()) {
					for (String key : branchMap.keySet()) {
						if (!branchMap.get(key).isSecondTime) {
							Integer currentLines = branchMap.get(key).lines;
							Integer newValue = currentLines + 1;
							branchMap.put(key, assembler.new ForBranchMap(branchMap.get(key).label, newValue, false));
						}
					}
				}
				line = line.trim(); // first delete leading and tailing
									// spaces,
									// then we can do detector
				// DONE: need detect ":" -> cover this in detector
				ForDetector forDetector = assembler.instHandler(line);
				int index = forDetector.index;
				line = line.replaceAll("\\s+", ""); // replace all
													// whiteSpaces
													// and invisible chars
				// String label = line.substring(index, line.length());
				if (forDetector.isLabel) {
					String label = line.substring(0, index);
					if (branchMap.containsKey(label)) {
						Integer currentLines = branchMap.get(label).lines;
						branchMap.put(label, assembler.new ForBranchMap(label, currentLines, true));
					} else { // first time see label
						ForBranchMap forHashMap = assembler.new ForBranchMap(label, 1, false);
						branchMap.put(label, forHashMap);
					}
				} else if (forDetector.isBranch) {
					String label = line.substring(index, line.length());
					String op = line.substring(0, index);
					String opCode = assembler.inst.get(op); // in Bin
					if (branchMap.containsKey(label)) {
						Integer currentLines = branchMap.get(label).lines;
						branchMap.put(label, assembler.new ForBranchMap(label, currentLines, true));
						outputArray.add(opCode + "-" + label);
					} else { // first time see branch
						ForBranchMap forBranchMap = assembler.new ForBranchMap(label, 1, false);
						branchMap.put(label, forBranchMap);
						outputArray.add(opCode + "+" + label);
					}
				} else {// other instructions
					String label = line.substring(0, index);
					String opCode = assembler.inst.get(label);
					System.out.println(label);
					// DONE: works only when you have one space between op
					// and
					// value, fix index problem
					String value = line.substring(index, line.length());
					String valueBin = valToBin(value);
					System.out.println(line.substring(index, line.length()));
					String finalBin = opCode + valueBin;
					// String finalHex = binToHex(finalBin);
					// outputArray.add(finalHex); //final value is hex
					outputArray.add(finalBin); // firstly add them as binary
				}
				line = bReader.readLine(); // read next line
			}
			System.out.println("###OUT PUT###" + outputArray.toString());
			System.out.println("###HashMap###" + toHMString(branchMap));

			// DONE: second loop for correcting brach value
			for (int i = 0; i < outputArray.size(); i++) {
				String finalBin = outputArray.get(i);
				if (!finalBin.matches("[0-9]+")) { // check if it has only
													// numbers
					String operation = finalBin.substring(0, 5);
					// TODO: label is wrong
					String label = finalBin.substring(6, finalBin.length());
					String sign = finalBin.substring(5, 7); // it gets + or -
															// sign
//					System.out.println(i + " " + label);
					String value = Integer.toString(branchMap.get(label).lines);
					if (sign.equals("-")) {
						value = sign + value;
					}
					String valInBin = valToBin(value); // value in Bin
					finalBin = operation + valInBin;
					String finalHex = binToHex(finalBin);
					outputArray.set(i, finalHex);
					// }
				} else { // finalBin contains only numbers
					String finalHex = binToHex(finalBin);
					outputArray.set(i, finalHex);
				}
			}
		} catch (FileNotFoundException e) {
			outputArray.add(e.getStackTrace().toString());
			e.printStackTrace();
			System.out.println("FileNotFound");
		} catch (IOException e) {
			outputArray.add(e.getStackTrace().toString());
			e.printStackTrace();
			System.out.println("IOException");
		}
		return outputArray;
	}

	/*
	 * a better hashMap toString function
	 */
	private static String toHMString(HashMap<String, ForBranchMap> input) {
		String toReturn = "";
		for (String currentKey : input.keySet()) {
			ForBranchMap currentFHM = input.get(currentKey);
			toReturn += "[" + currentKey.toString() + " => " + currentFHM.lines + ", " + currentFHM.label + ", "
					+ currentFHM.isSecondTime + "]\n";
		}
		return toReturn;
	}

	private static String binToHex(String bin) {
		int decimal = Integer.parseInt(bin, 2);
		String hex = Integer.toString(decimal, 16);
		Integer generation = 4 - hex.length();
		hex = new String(new char[generation]).replace("\0", "0") + hex; // 4
																			// bytes
		return hex;
	}

	/*
	 * it gets a number and convert it to binary DONE: need to extend every
	 * value to 11-bit, just like opcode
	 * 
	 */

	private static String valToBin(String value) {
		String toReturn = "";
		switch (value.charAt(0)) {
		case 'b': {
			toReturn = value.substring(1);
			break;
		}
		case 'h': {
			int h = Integer.parseInt(value.substring(1), 16);
			toReturn = Integer.toBinaryString(h);
			break;
		}
		case 'd': {
			System.out.println("d");
			// if the char after d is '-', which means that the immediate is negative
			if(value.charAt(1) == '-') {
				// substring to get positive number
				toReturn = Integer.toBinaryString(Integer.parseInt(value.substring(2, value.length()), 8));
				// fill it with 1 in front
				int generation_neg = 11 - toReturn.length();
				// it will not be filled with 0 outside switch beacuse length is already 11
				toReturn = new String(new char[generation_neg]).replace("\0", "1") + toReturn;
			}else {
				toReturn = Integer.toBinaryString(Integer.parseInt(value.substring(1), 8));
			}
			System.out.println(toReturn);
			break;
		}
		default:
			break;
		}
		System.out.println(toReturn);
		int generation = 11 - toReturn.length();
		toReturn = new String(new char[generation]).replace("\0", "0") + toReturn;
		return toReturn;
	}

	/*
	 * it returns the index of the end of inst + 1 -> maybe good for substring
	 * later
	 * 
	 */
	private ForDetector instHandler(String instruction) {
		if (instruction.contains("bnez") || instruction.contains("bez") || instruction.contains("jal")) {
			int index = instruction.indexOf(" ");
			return new ForDetector(index, false, true);
		}
		if (instruction.contains(":")) { // it is label
			return new ForDetector(instruction.indexOf(":"), true, false);
		}
		return new ForDetector(instruction.indexOf(" "), false, false);
	}

	class ForDetector {
		int index;
		boolean isLabel;
		boolean isBranch;

		public ForDetector(int index, boolean isLabel, boolean isBranch) {
			this.index = index;
			this.isLabel = isLabel; // do not need to add it into hashMap
			this.isBranch = isBranch; // this means it has opcode
		}
	}

	class ForBranchMap {
		Integer lines;
		String label;
		// DONE: do we need is branch here? -> second time boolean
		boolean isSecondTime;

		public ForBranchMap(String label, Integer lines, boolean isSecondTime) {
			this.lines = lines;
			this.label = label;
			this.isSecondTime = isSecondTime;
		}
	}
}
