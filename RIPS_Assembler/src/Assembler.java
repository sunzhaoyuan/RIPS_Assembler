import java.util.ArrayList;
import java.util.HashMap;

import com.sun.glass.ui.TouchInputSupport;

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
 * @author Zhaoyuan Sun and Shijun Yu at Rose-Hulman. Created Feb 8, 2017.
 */
public class Assembler {
	final Integer OPCODE_LENGTH = 5;
	final Integer NUM_OF_INST = 31;
	final String[] NAMES = { "add", "addi", "sub", "and", "or", "sll", "srl", "sra", "load", "store", "sstor", "lacc",
			"loadsp", "storesp", "addsp", "ja", "jal", "bez", "bnez", "slt", "sltsp", "addmsp", "submsp", "andi", "ori",
			"orui", "loadi", "input", "output", "stemp", "itemp" };

	HashMap<String, String> inst;

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
		// write in files
		try (PrintWriter writer = new PrintWriter(filePath.replaceAll(".txt", "") + "_Output" + ".txt")) {
			for (String str : file) { // write output in format
				writer.println(str);
			}
		}
		System.out.println("Process is finished. Please check your folder.");
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
		boolean hasError = false;
		File inputFile = new File(file);
		Assembler assembler = new Assembler();
		ArrayList<String> outputArray = new ArrayList<>();
		// 2 HashMaps for real-label and label-in-branch
		HashMap<String, Integer> branchMap = new HashMap<>();
		HashMap<String, Integer> labelMap = new HashMap<>();
		int currentLine = 0;
		try {
			@SuppressWarnings("resource")
			BufferedReader bReader = new BufferedReader(new FileReader(inputFile));
			String line = bReader.readLine();
			while (line != null) {
				line = line.trim(); // first delete leading and tailing
									// spaces,
									// then we can do detector
				ForDetector forDetector = assembler.instHandler(line);
				int index = forDetector.index;
				line = line.replaceAll("\\s+", ""); // replace all
													// whiteSpaces
													// and invisible chars
				if (forDetector.isLabel) {
					String label = line.substring(0, index);
					labelMap.put(label, currentLine);
				} else if (forDetector.isBranch) {
					String label = line.substring(index, line.length());
					String op = line.substring(0, index);
					String opCode = assembler.inst.get(op); // in Bin
					Integer num = 0;
					while (branchMap.containsKey(label)) {
						// same label but in different brach lines
						// first branch is just the label name
						// then it is label name + unique number
						label = label + " " + num;
						num++;
					}
					branchMap.put(label, currentLine);
					outputArray.add(opCode + label);
				} else {// other instructions
					String instruction = line.substring(0, index);
					if (!assembler.inst.containsKey(instruction)) {
						outputArray.add("invalid instruction!!! Please read our user manual.");
						hasError = true;
						break;
					}
					String opCode = assembler.inst.get(instruction);
					String value = line.substring(index, line.length());
					if (value.length() > 1) {
						Integer valueInDec = Integer.parseInt(value.substring(1));
						if (valueInDec > 2048 || valueInDec < -2048) {
							outputArray.add("Value out of bound! (2^11)");
							hasError = true;
							break;
						}
					}
					String valueBin = valToBin(value);
					if (valueBin.equals("Invalid Value Syntax.")) {
						hasError = true;
						outputArray.add("Invalid Value Syntax.");
						break;
					}
					String finalBin = opCode + valueBin;
					outputArray.add(finalBin); // firstly add them as binary
				}
				line = bReader.readLine(); // read next line
				currentLine++;
			}

			// second loop for correcting brach value
			if (!hasError) {
				for (int i = 0; i < outputArray.size(); i++) {
					String finalBin = outputArray.get(i);
					if (!finalBin.matches("[0-9]+")) { // check if it has only
														// numbers
						String operation = finalBin.substring(0, 5);
						String label = finalBin.substring(5, finalBin.length());
						int branchLine = branchMap.get(label);
						if (label.contains(" ")) {
							int trim = label.indexOf(" ");
							label = label.substring(0, trim);
						}
						// handle if label is invalid
						if (!labelMap.containsKey(label)) {
							outputArray.set(i, "Invalid Label.");
							break;
						}
						int labelLine = labelMap.get(label);
						int value = 0;
						if (labelLine > branchLine) {
							value = labelLine - branchLine - 1;
							int count = 0;
							for (String current : labelMap.keySet()) {
								if (branchLine < labelMap.get(current) && labelMap.get(current) < labelLine) {
									count++;
								}
							}
							value -= count;
						} else {
							value = labelLine - branchLine;
							int count = 0;
							for (String current : labelMap.keySet()) {
								if (labelLine < labelMap.get(current) && labelMap.get(current) < branchLine) {
									count++;
								}
							}
							value += count;
						}
						String result = "d" + value;
						String valInBin = valToBin(result); // value in Bin
						finalBin = operation + valInBin;
						String finalHex = binToHex(finalBin);
						outputArray.set(i, finalHex);
						// }
					} else { // finalBin contains only numbers
						String finalHex = binToHex(finalBin);
						outputArray.set(i, finalHex);
					}
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

	private static String binToHex(String bin) {
		int decimal = Integer.parseInt(bin, 2);
		String hex = Integer.toString(decimal, 16);
		Integer generation = 4 - hex.length();
		hex = new String(new char[generation]).replace("\0", "0") + hex; // 4
																			// bytes
		return hex;
	}

	/*
	 * it gets a number and convert it to binary
	 * 
	 */
	private static String valToBin(String value) {
		String toReturn = "";
		boolean hasError = false;
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
			// if the char after d is '-', which means that the immediate is
			// negative
			if (value.charAt(1) == '-') {
				// generate negative number algorithm
				toReturn = toNegativeBinaryString(value);
			} else {
				toReturn = Integer.toBinaryString(Integer.parseInt(value.substring(1), 10));
			}
			break;
		}
		default: {
			toReturn = "Invalid Value Syntax.";
			hasError = true;
			break;
		}
		}
		if (!hasError) {
			int generation = 11 - toReturn.length();
			toReturn = new String(new char[generation]).replace("\0", "0") + toReturn;
		}
		return toReturn;
	}

	public static String toNegativeBinaryString(String value) {
		String toReturn = "";
		// take the positive part of the negative number, minus 1, convert to
		// binary string
		toReturn = Integer.toBinaryString(Integer.parseInt(value.substring(2, value.length()), 10) - 1);
		// a dumb way to do bitwise flip (change 0 to 1, 1 to 0)
		toReturn = toReturn.replaceAll("0", "2");
		toReturn = toReturn.replaceAll("1", "0");
		toReturn = toReturn.replaceAll("2", "1");
		// fill it with 1 in front
		int generation_neg = 11 - toReturn.length();
		toReturn = new String(new char[generation_neg]).replace("\0", "1") + toReturn;
		return toReturn;
	}

	/*
	 * it returns the index of the end of inst + 1
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
}
