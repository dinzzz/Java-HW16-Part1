package hr.fer.zemris.searchengine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import hr.fer.zemris.math.MultidimensionalVector;

/**
 * Class which represents a console which serves as a search engine. This engine
 * searches the initially set database and presents the results to the user.
 * 
 * @author Dinz
 *
 */
public class Console {

	/**
	 * Delta.
	 */
	private static final double DELTA = 1E-3;

	/**
	 * List of result documents.
	 */
	private static List<String> results;

	/**
	 * Document count.
	 */
	private static int documentCount;

	/**
	 * List of stop words.
	 */
	private static List<String> stopWords;

	/**
	 * Map which stores words and the number of documents they appear in.
	 */
	private static Map<String, Double> wordCount;

	/**
	 * Vocabulary set.
	 */
	private static Set<String> words;

	/**
	 * Document vectors.
	 */
	private static Map<String, MultidimensionalVector> documentVectors;

	/**
	 * IDF vector.
	 */
	private static MultidimensionalVector idfVector;

	/**
	 * Main method which runs the console.
	 * 
	 * @param args
	 *            Arguments from the command prompt.
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Insufficient number of arguments.");
			return;
		}

		String dir = args[0];
		Path directory = Paths.get(dir);

		if (!directory.toFile().isDirectory()) {
			System.out.println("Program argument should be a directory.");
			return;
		}
		documentCount = documentCount(directory);
		stopWords = Files.readAllLines(Paths.get("src/main/resources/hrvatski_stoprijeci.txt"), StandardCharsets.UTF_8);
		for (int i = 0; i < stopWords.size(); i++) {
			String x = stopWords.get(i);
			if (x.trim().isEmpty()) {
				stopWords.remove(x);
			}
		}
		wordCount = extractWords(directory);
		words = wordCount.keySet();
		idfVector = calculateIDF(wordCount.values().stream().collect(Collectors.toList()));
		documentVectors = docVectors(directory);

		applyConsole();
	}

	/**
	 * Calculates the IDF vector.
	 * 
	 * @param collect
	 *            List which stores the number of word appearance in the couments.
	 * @return IDF vector.
	 */
	private static MultidimensionalVector calculateIDF(List<Double> collect) {
		List<Double> results = new ArrayList<>();
		for (int i = 0; i < collect.size(); i++) {
			results.add(Math.log10((double) documentCount / collect.get(i).doubleValue()));
		}
		return new MultidimensionalVector(results);
	}

	/**
	 * Method which applies the console.
	 * 
	 * @throws IOException
	 */
	private static void applyConsole() throws IOException {
		System.out.println("The size of the dictionary is: " + words.size());
		Scanner sc = new Scanner(System.in);

		while (true) {
			System.out.print("\nEnter command >");
			String input = sc.nextLine();

			if (input.toLowerCase().startsWith("query")) {
				produceQuery(input);
			} else if (input.toLowerCase().startsWith("type")) {
				produceType(input);
			} else if (input.trim().toLowerCase().equals("results")) {
				produceResults(input);
			} else if (input.toLowerCase().startsWith("exit")) {
				sc.close();
				break;
			} else {
				System.out.println("Invalid command.");
			}

		}

	}

	/**
	 * Method which produces the type result when type command is called.
	 * 
	 * @param input
	 *            Arguments of the command.
	 * @throws IOException
	 */
	private static void produceType(String input) throws IOException {
		input = input.replaceFirst("type", "").trim();

		try {

			int index = Integer.parseInt(input);

			if (results == null || results.isEmpty()) {
				System.out.println("No results.");
				return;
			}

			if (index < 1 || index > results.size()) {
				System.out.println("Invalid index.");
				return;
			}

			Path file = Paths.get(results.get(index - 1).split("\\s+")[2]);
			byte[] bts = Files.readAllBytes(file);
			String text = new String(bts, StandardCharsets.UTF_8);
			System.out.println(text);

		} catch (NumberFormatException ex) {
			System.out.println("Invalid input");
		}
	}

	/**
	 * Method which produces results when the result command is called.
	 * 
	 * @param input
	 *            Arguments of the command.
	 */
	private static void produceResults(String input) {
		if (results == null || results.isEmpty()) {
			System.out.println("No results.");
			return;
		}

		for (String result : results) {
			System.out.println(result);
		}

	}

	/**
	 * Method which produces appropriate results when the query command is called.
	 * 
	 * @param input
	 *            Arguments of the command.
	 */
	private static void produceQuery(String input) {
		input = input.replaceFirst("query", "").trim();

		String[] wordSplit = input.split("\\s+");
		MultidimensionalVector queryVector = newVector(wordSplit);
		if (queryVector == null) {
			return;
		}
		queryVector = queryVector.crossProduct(idfVector);
		Map<String, Double> resultMap = new TreeMap<>();

		for (Entry<String, MultidimensionalVector> entry : documentVectors.entrySet()) {
			double similarity = entry.getValue().similarity(queryVector);
			String path = entry.getKey();

			resultMap.put(path, similarity);
		}

		resultMap = resultMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
						LinkedHashMap::new));

		results = new ArrayList<>();
		int count = 0;
		for (Entry<String, Double> entry : resultMap.entrySet()) {
			String path = entry.getKey();
			double similarity = entry.getValue();

			if (similarity < DELTA) {
				continue;
			}

			String addition = String.format("[%d] (%.4f) %s", (count + 1), similarity, path);
			results.add(addition);
			System.out.println(addition);

			count++;
			if (count == 10) {
				break;
			}
		}

	}

	/**
	 * Method which creates a new document vector from given words.
	 * 
	 * @param wordSplit
	 *            Words.
	 * @return Newly created document vector.
	 */
	private static MultidimensionalVector newVector(String[] wordSplit) {
		StringBuilder sb = new StringBuilder();
		for (String word : wordSplit) {
			if (words.contains(word)) {
				sb.append(word + " ");
			}
		}

		String text = sb.toString();
		List<String> wordList = new ArrayList<>(Arrays.asList(wordSplit));

		if (text.trim().isEmpty()) {
			System.out.println("No results");
			return null;
		}

		StringBuilder sbq = new StringBuilder();
		sbq.append("Query is: [");
		int count = 0;
		for (String wrd : wordList) {
			sbq.append(wrd);
			if (count < wordList.size() - 1) {
				sbq.append(", ");
			}
			count++;
		}
		sbq.append("].\nBest ten results:");
		System.out.println(sbq.toString());

		List<Double> tflist = new ArrayList<>();
		for (String word : words) {

			double tf = 0;
			for (String wrd : wordList) {
				if (word.equals(wrd)) {
					tf++;
				}
			}

			tflist.add(tf);
		}

		return new MultidimensionalVector(tflist);

	}

	/**
	 * Calculates the number of documents in the context.
	 * 
	 * @param directory
	 *            Directory where the documents are stored.
	 * @return Number of documents.
	 */
	private static int documentCount(Path directory) {
		int count = 0;
		for (@SuppressWarnings("unused")
		File file : directory.toFile().listFiles()) {
			count++;
		}

		return count;
	}

	/**
	 * Method which creates a document vectors from the documents in the given
	 * directory.
	 * 
	 * @param directory
	 *            Given directory.
	 * @return Map of document vectors mapped by their paths.
	 * @throws IOException
	 */
	private static Map<String, MultidimensionalVector> docVectors(Path directory) throws IOException {
		Map<String, MultidimensionalVector> map = new LinkedHashMap<>();

		for (File file : directory.toFile().listFiles()) {
			String pth = file.getAbsolutePath();
			byte[] bts = Files.readAllBytes(file.toPath());
			String text = new String(bts, StandardCharsets.UTF_8).toLowerCase();
			text = text.replaceAll("[^\\p{IsAlphabetic}]", " ");
			List<String> wordlst = new ArrayList<>(Arrays.asList(text.trim().split("\\s+")));

			List<Double> tflist = new ArrayList<>();
			for (String word : words) {
				double tf = 0;

				for (String wrd : wordlst) {
					if (word.equals(wrd)) {
						tf++;
					}
				}

				tflist.add(tf);
			}
			MultidimensionalVector vector = new MultidimensionalVector(tflist).crossProduct(idfVector);
			map.put(pth, vector);
		}

		return map;
	}

	/**
	 * Method which extracts the words from the documents in the given directory.
	 * 
	 * @param directory
	 *            Given directory.
	 * @return Map.
	 * @throws IOException
	 */
	private static Map<String, Double> extractWords(Path directory) throws IOException {
		Map<String, Double> words = new LinkedHashMap<>();

		for (File file : directory.toFile().listFiles()) {
			List<String> addedWords = new ArrayList<>();
			byte[] bts = Files.readAllBytes(file.toPath());
			String text = new String(bts, StandardCharsets.UTF_8).toLowerCase();
			text = text.replaceAll("[^\\p{IsAlphabetic}]", " ");

			List<String> wordlst = new ArrayList<>(Arrays.asList(text.split("\\s+")));
			for (String word : wordlst) {
				if (!stopWords.contains(word) && !word.trim().isEmpty()) {
					if (!words.containsKey(word)) {
						words.put(word, 1.0);
						addedWords.add(word);
					} else if (!addedWords.contains(word)) {
						words.put(word, words.get(word).doubleValue() + 1);
						addedWords.add(word);
					}
				}
			}

		}

		return words;
	}

}
