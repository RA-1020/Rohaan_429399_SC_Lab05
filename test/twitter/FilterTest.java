/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FilterTest {

    /*
     * Testing strategy
     *
     * writtenBy(tweets, username):
     *   partition on tweets.size(): 0, 1, >1
     *   partition on number of matching tweets: 0, 1, >1
     *   partition on case of username vs author: same case, different case
     *   partition on order: result order must match input order
     *   partition on username prefix: author "alyssa" must not match "aly"
     *
     * inTimespan(tweets, timespan):
     *   partition on tweets.size(): 0, 1, >1
     *   partition on tweets inside timespan: none, some, all
     *   partition on tweet timestamp relative to timespan:
     *       before start, equal to start, strictly inside, equal to end, after end
     *   partition on timespan length: zero-length (start = end), positive
     *
     * containing(tweets, words):
     *   partition on tweets.size(): 0, 1, >1
     *   partition on words.size(): 0, 1, >1
     *   partition on matching tweets: 0, 1, >1
     *   partition on case: same case, different case
     *   partition on word position in text: start, middle, end
     *   partition on what counts as a word (words are bounded by spaces):
     *       substring of a longer word ("talk" vs "talking") does not match;
     *       word with attached punctuation ("talk!") is a different word;
     *       non-letter words ("#hype") match exactly
     *   partition on one tweet matching several words: returned once
     *   partition on order: result order must match input order
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");

    private static final Tweet tweet1 = new Tweet(1, "alyssa",
            "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle",
            "rivest talk in 30 minutes #hype", d2);

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ---------------- writtenBy ----------------

    // covers tweets.size() = 0
    @Test
    public void testWrittenByEmptyList() {
        List<Tweet> writtenBy = Filter.writtenBy(Collections.emptyList(), "alyssa");
        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    // covers tweets.size() = 1, 1 match, same case
    @Test
    public void testWrittenBySingleTweet() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1), "alyssa");
        assertEquals("expected singleton list", Arrays.asList(tweet1), writtenBy);
    }

    // covers tweets.size() > 1, 1 match
    @Test
    public void testWrittenByMultipleTweetsSingleResult() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2),
                "alyssa");
        assertEquals("expected singleton list", Arrays.asList(tweet1), writtenBy);
    }

    // covers tweets.size() > 1, 0 matches
    @Test
    public void testWrittenByNoMatches() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2),
                "charlie");
        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    // covers >1 matches, order preserved
    @Test
    public void testWrittenByMultipleMatchesInOrder() {
        Tweet tweet3 = new Tweet(3, "alyssa", "another tweet by alyssa", d2);
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet3, tweet2,
                tweet1), "alyssa");
        assertEquals("expected both tweets in input order", Arrays.asList(tweet3,
                tweet1), writtenBy);
    }

    // covers different case between author and username
    @Test
    public void testWrittenByCaseInsensitive() {
        Tweet tweet3 = new Tweet(3, "ALYSSA", "tweet in caps", d1);
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet3, tweet2),
                "AlYsSa");
        assertEquals(Arrays.asList(tweet3), writtenBy);
    }

    // covers username that is only a prefix of the author
    @Test
    public void testWrittenByPrefixDoesNotMatch() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1), "aly");
        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    // ---------------- inTimespan ----------------

    // covers tweets.size() = 0
    @Test
    public void testInTimespanEmptyList() {
        List<Tweet> inTimespan = Filter.inTimespan(Collections.emptyList(),
                new Timespan(d1, d2));
        assertTrue("expected empty list", inTimespan.isEmpty());
    }

    // covers all tweets strictly inside, order preserved
    @Test
    public void testInTimespanMultipleTweetsMultipleResults() {
        Instant testStart = Instant.parse("2016-02-17T09:00:00Z");
        Instant testEnd = Instant.parse("2016-02-17T12:00:00Z");
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2),
                new Timespan(testStart, testEnd));
        assertEquals("expected both tweets in input order", Arrays.asList(tweet1,
                tweet2), inTimespan);
    }

    // covers no tweets inside (all after end)
    @Test
    public void testInTimespanNoResults() {
        Instant testStart = Instant.parse("2016-02-17T09:00:00Z");
        Instant testEnd = Instant.parse("2016-02-17T09:30:00Z");
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2),
                new Timespan(testStart, testEnd));
        assertTrue("expected empty list", inTimespan.isEmpty());
    }

    // covers timestamps equal to start and equal to end (endpoints inclusive)
    @Test
    public void testInTimespanBoundaryInclusive() {
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2),
                new Timespan(d1, d2));
        assertEquals("endpoints are inside the timespan", Arrays.asList(tweet1,
                tweet2), inTimespan);
    }

    // covers some inside: one before start, one inside
    @Test
    public void testInTimespanPartialOverlap() {
        Instant testStart = Instant.parse("2016-02-17T10:30:00Z");
        Instant testEnd = Instant.parse("2016-02-17T11:30:00Z");
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2),
                new Timespan(testStart, testEnd));
        assertEquals(Arrays.asList(tweet2), inTimespan);
    }

    // covers zero-length timespan, tweet exactly at that instant, tweet after end
    @Test
    public void testInTimespanZeroLength() {
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2),
                new Timespan(d1, d1));
        assertEquals(Arrays.asList(tweet1), inTimespan);
    }

    // ---------------- containing ----------------

    // covers tweets.size() = 0
    @Test
    public void testContainingEmptyTweets() {
        List<Tweet> containing = Filter.containing(Collections.emptyList(),
                Arrays.asList("talk"));
        assertTrue("expected empty list", containing.isEmpty());
    }

    // covers words.size() = 0
    @Test
    public void testContainingEmptyWords() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2),
                Collections.emptyList());
        assertTrue("expected empty list", containing.isEmpty());
    }

    // covers 1 word, >1 matches, middle position, order preserved
    @Test
    public void testContaining() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2),
                Arrays.asList("talk"));
        assertEquals("expected both tweets in input order", Arrays.asList(tweet1,
                tweet2), containing);
    }

    // covers 0 matches with >1 words
    @Test
    public void testContainingNoMatches() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2),
                Arrays.asList("foo", "bar"));
        assertTrue("expected empty list", containing.isEmpty());
    }

    // covers 1 match, word at start of text
    @Test
    public void testContainingSingleWordAtStart() {
        Tweet tweet3 = new Tweet(3, "charlie", "hello world", d1);
        Tweet tweet4 = new Tweet(4, "david", "goodbye moon", d1);
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet3, tweet4),
                Arrays.asList("hello"));
        assertEquals(Arrays.asList(tweet3), containing);
    }

    // covers >1 words, each tweet matching a different word, word at end of text
    @Test
    public void testContainingMultipleWords() {
        Tweet tweet3 = new Tweet(3, "charlie", "hello there", d1);
        Tweet tweet4 = new Tweet(4, "david", "goodbye world", d1);
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet3,
                tweet4), Arrays.asList("hello", "world"));
        assertEquals(Arrays.asList(tweet3, tweet4), containing);
    }

    // covers different case between word and tweet text
    @Test
    public void testContainingCaseInsensitive() {
        Tweet tweet3 = new Tweet(3, "charlie", "Hello World", d1);
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet3),
                Arrays.asList("hELLo"));
        assertEquals(Arrays.asList(tweet3), containing);
    }

    // covers one tweet matching several words: returned exactly once
    @Test
    public void testContainingNoDuplicates() {
        Tweet tweet3 = new Tweet(3, "charlie", "rivest talk about rivest", d1);
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet3),
                Arrays.asList("rivest", "talk"));
        assertEquals("expected tweet once", Arrays.asList(tweet3), containing);
    }

    // covers substring of a longer word: "talk" does not match "talking"/"talks"
    @Test
    public void testContainingSubstringIsNotWord() {
        Tweet tweet3 = new Tweet(3, "charlie", "talking about talks", d1);
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet3),
                Arrays.asList("talk"));
        assertTrue("expected empty list", containing.isEmpty());
    }

    // covers punctuation attached to a word: "talk!" is a different word from "talk"
    @Test
    public void testContainingPunctuationIsPartOfWord() {
        Tweet tweet3 = new Tweet(3, "charlie", "lets talk!", d1);
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet3),
                Arrays.asList("talk"));
        assertTrue("words are bounded only by spaces", containing.isEmpty());
        List<Tweet> exact = Filter.containing(Arrays.asList(tweet3),
                Arrays.asList("talk!"));
        assertEquals(Arrays.asList(tweet3), exact);
    }

    // covers word made of non-letter characters
    @Test
    public void testContainingHashtagWord() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2),
                Arrays.asList("#hype"));
        assertEquals(Arrays.asList(tweet2), containing);
    }

    /*
     * Warning: all the tests you write here must be runnable against any Filter
     * class that follows the spec. It will be run against several staff
     * implementations of Filter, which will be done by overwriting
     * (temporarily) your version of Filter with the staff's version.
     * DO NOT strengthen the spec of Filter or its methods.
     *
     * In particular, your test cases must not call helper methods of your own
     * that you have put in Filter, because that means you're testing a stronger
     * spec than Filter says. If you need such helper methods, define them in a
     * different class. If you only need them in this test class, then keep them
     * in this test class.
     */

}