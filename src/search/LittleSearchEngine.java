package search;

import java.io.*;
import java.util.*;

/**
 * This class encapsulates an occurrence of a keyword in a document. It stores
 * the document name, and the frequency of occurrence in that document.
 * Occurrences are associated with keywords in an index hash table.
 * 
 * @author Sesh Venugopal
 * 
 */
class Occurrence {
	/**
	 * Document in which a keyword occurs.
	 */
	String document;

	/**
	 * The frequency (number of times) the keyword occurs in the above document.
	 */
	int frequency;

	/**
	 * Initializes this occurrence with the given document,frequency pair.
	 * 
	 * @param doc
	 *            Document name
	 * @param freq
	 *            Frequency
	 */
	public Occurrence(String doc, int freq) {
		document = doc;
		frequency = freq;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "(" + document + "," + frequency + ")";
	}
}

/**
 * This class builds an index of keywords. Each keyword maps to a set of
 * documents in which it occurs, with frequency of occurrence in each document.
 * Once the index is built, the documents can searched on for keywords.
 *
 */
public class LittleSearchEngine {

	/**
	 * This is a hash table of all keywords. The key is the actual keyword, and
	 * the associated value is an array list of all occurrences of the keyword
	 * in documents. The array list is maintained in descending order of
	 * occurrence frequencies.
	 */
	HashMap<String, ArrayList<Occurrence>> keywordsIndex;

	/**
	 * The hash table of all noise words - mapping is from word to itself.
	 */
	HashMap<String, String> noiseWords;

	/**
	 * Creates the keyWordsIndex and noiseWords hash tables.
	 */
	public LittleSearchEngine() {
		keywordsIndex = new HashMap<String, ArrayList<Occurrence>>(1000, 2.0f);
		noiseWords = new HashMap<String, String>(100, 2.0f);
	}

	/**
	 * This method indexes all keywords found in all the input documents. When
	 * this method is done, the keywordsIndex hash table will be filled with all
	 * keywords, each of which is associated with an array list of Occurrence
	 * objects, arranged in decreasing frequencies of occurrence.
	 * 
	 * @param docsFile
	 *            Name of file that has a list of all the document file names,
	 *            one name per line
	 * @param noiseWordsFile
	 *            Name of file that has a list of noise words, one noise word
	 *            per line
	 * @throws FileNotFoundException
	 *             If there is a problem locating any of the input files on disk
	 */
	public void makeIndex(String docsFile, String noiseWordsFile) throws FileNotFoundException {
		// load noise words to hash table
		Scanner sc = new Scanner(new File(noiseWordsFile));
		while (sc.hasNext()) {
			String word = sc.next();
			noiseWords.put(word, word);
		}

		// index all keywords
		sc = new Scanner(new File(docsFile));
		while (sc.hasNext()) {
			String docFile = sc.next();
			HashMap<String, Occurrence> kws = loadKeyWords(docFile);
			mergeKeyWords(kws);
		}

	}

	/**
	 * Scans a document, and loads all keywords found into a hash table of
	 * keyword occurrences in the document. Uses the getKeyWord method to
	 * separate keywords from other words.
	 * 
	 * @param docFile
	 *            Name of the document file to be scanned and loaded
	 * @return Hash table of keywords in the given document, each associated
	 *         with an Occurrence object
	 * @throws FileNotFoundException
	 *             If the document file is not found on disk
	 */
	public HashMap<String, Occurrence> loadKeyWords(String docFile) throws FileNotFoundException {
		Scanner sc = new Scanner(new File(docFile));
		String[] line;
		String word;
		HashMap<String, Occurrence> tr = new HashMap<String, Occurrence>();
		while (sc.hasNextLine()) {
			line = sc.nextLine().split(" ");
			for (int i = 0; i < line.length; i++) {
				word = getKeyWord(line[i]);
				if (word != null) {
					if (!tr.containsKey(word)) {
						tr.put(word, new Occurrence(docFile, 1));
					} else {
						tr.get(word).frequency++;
					}
				}
			}
		}
		/*
		 * for(Map.Entry<String, Occurrence> o : tr.entrySet()) {
		 * System.out.print(o.getValue().document + " -> " + o.getKey() + ": " +
		 * o.getValue().frequency +", "); }
		 */
		return tr;
	}

	/**
	 * Merges the keywords for a single document into the master keywordsIndex
	 * hash table. For each keyword, its Occurrence in the current document must
	 * be inserted in the correct place (according to descending order of
	 * frequency) in the same keyword's Occurrence list in the master hash
	 * table. This is done by calling the insertLastOccurrence method.
	 * 
	 * @param kws
	 *            Keywords hash table for a document
	 */
	public void mergeKeyWords(HashMap<String, Occurrence> kws) {
		for (Map.Entry<String, Occurrence> o : kws.entrySet()) {
			String s = o.getKey();
			if (!keywordsIndex.containsKey(s)) {
				ArrayList<Occurrence> occ = new ArrayList();
				occ.add(o.getValue());
				keywordsIndex.put(s, occ);
			} else {
				ArrayList<Occurrence> occ = keywordsIndex.get(s);
				occ.add(o.getValue());
				insertLastOccurrence(occ);
			}
		}
	}

	/**
	 * Given a word, returns it as a keyword if it passes the keyword test,
	 * otherwise returns null. A keyword is any word that, after being stripped
	 * of any TRAILING punctuation, consists only of alphabetic letters, and is
	 * not a noise word. All words are treated in a case-INsensitive manner.
	 * 
	 * Punctuation characters are the following: '.', ',', '?', ':', ';' and '!'
	 * 
	 * @param word
	 *            Candidate word
	 * @return Keyword (word without trailing punctuation, LOWER CASE)
	 */
	public String getKeyWord(String word) {
		String w = word.replaceAll("[^a-zA-Z ]+$", "").toLowerCase();
		String w2 = word.replaceAll("[^a-zA-Z ]", "").toLowerCase();
		if (w.isEmpty())
			return null;
		if (w.equals(w2)) {
			if (!noiseWords.containsKey(w))
				return w;
		}
		return null;
	}

	/**
	 * Inserts the last occurrence in the parameter list in the correct position
	 * in the same list, based on ordering occurrences on descending
	 * frequencies. The elements 0..n-2 in the list are already in the correct
	 * order. Insertion of the last element (the one at index n-1) is done by
	 * first finding the correct spot using binary search, then inserting at
	 * that spot.
	 * 
	 * @param occs
	 *            List of Occurrences
	 * @return Sequence of mid point indexes in the input list checked by the
	 *         binary search process, null if the size of the input list is 1.
	 *         This returned array list is only used to test your code - it is
	 *         not used elsewhere in the program.
	 */
	public ArrayList<Integer> insertLastOccurrence(ArrayList<Occurrence> occs) {
		int low = 0;
		int high = occs.size() - 2;
		ArrayList<Integer> arr = new ArrayList<Integer>();
		Occurrence key = occs.get(occs.size() - 1);
		int middle = 0;
		while (high >= low) {
			middle = (low + high) / 2;
			arr.add(middle);
			// System.out.println(middle);
			if (occs.get(middle).frequency == key.frequency) {
				break;
			}
			if (occs.get(middle).frequency > key.frequency) {
				low = middle + 1;
			}
			if (occs.get(middle).frequency < key.frequency) {
				high = middle - 1;
			}
		}
		Occurrence item = occs.get(middle);
		int shiftIndex = middle;
		if (key.frequency <= item.frequency) {
			shiftIndex++;
		}
		occs.add(shiftIndex, key);
		occs.remove(occs.size() - 1);
		/*
		 * for(int i = occs.size() - 1; i > shiftIndex; i--) { occs.set(i,
		 * occs.get(i - 1)); } occs.set(shiftIndex, key); for(Occurrence o:
		 * occs) { System.out.print(o.frequency + " "); }
		 */
		return arr;
	}

	/**
	 * Search result for "kw1 or kw2". A document is in the result set if kw1 or
	 * kw2 occurs in that document. Result set is arranged in descending order
	 * of occurrence frequencies. (Note that a matching document will only
	 * appear once in the result.) Ties in frequency values are broken in favor
	 * of the first keyword. (That is, if kw1 is in doc1 with frequency f1, and
	 * kw2 is in doc2 also with the same frequency f1, then doc1 will appear
	 * before doc2 in the result. The result set is limited to 5 entries. If
	 * there are no matching documents, the result is null.
	 * 
	 * @param kw1
	 *            First keyword
	 * @param kw1
	 *            Second keyword
	 * @return List of NAMES of documents in which either kw1 or kw2 occurs,
	 *         arranged in descending order of frequencies. The result size is
	 *         limited to 5 documents. If there are no matching documents, the
	 *         result is null.
	 */
	public ArrayList<String> top5search(String kw1, String kw2) {
		ArrayList<Occurrence> m1 = null;
		if (keywordsIndex.containsKey(kw1.toLowerCase()))
			m1 = keywordsIndex.get(kw1.toLowerCase());
		ArrayList<Occurrence> m2 = null;
		if (keywordsIndex.containsKey(kw2.toLowerCase()))
			m2 = keywordsIndex.get(kw2.toLowerCase());
		ArrayList<Occurrence> combo = new ArrayList();
		if (m1 != null)
			combo.addAll(m1);
		if (m2 != null)
			combo.addAll(m2);
		bubbleSort(combo);
		ArrayList<String> result = new ArrayList();
		outerloop: for (int i = 0; i < combo.size(); i++) {
			if(result.size() == 5) break;
			Occurrence item = combo.get(i);
			for (int j = 0; j < result.size(); j++) {
				String item2 = result.get(j);
				if (item.document.equals(item2)) {
					continue outerloop;
				}
			}
			result.add(item.document);
		}
		if(result.size() == 0) return null;
		return result;
	}

	static void bubbleSort(ArrayList<Occurrence> num) {
		if (num.size() == 0)
			return;
		int j;
		boolean flag = true;
		Occurrence temp;
		while (flag) {
			flag = false;
			for (j = 0; j < num.size() - 1; j++) {
				if (num.get(j).frequency < num.get(j + 1).frequency) {
					temp = num.get(j);
					num.set(j, num.get(j + 1));
					num.set(j + 1, temp);
					flag = true;
				}
			}
		}
	}
}
