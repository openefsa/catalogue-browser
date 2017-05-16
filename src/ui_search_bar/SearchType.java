package ui_search_bar;

/**
 * Which search we want to perform?
 * Set EXACT_MATCH to search the entire text as a single word to get terms
 * set ANY_WORD to get all the terms which have at least one of the words
 * set ALL_WORDS to get all the terms which have all the words (order is not important)
 * @author avonva
 *
 */
public enum SearchType {
	EXACT_MATCH,
	ANY_WORD,
	ALL_WORDS
}
