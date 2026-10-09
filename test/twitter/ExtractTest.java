/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ExtractTest {

    /*
     * Testing strategy
     *
     * getTimespan(tweets):
     *   partition on tweets.size(): 0, 1, >1
     *   partition on timestamps:    all equal, distinct
     *   partition on list order:    earliest/latest at the ends, earliest/latest
     *                               in the middle (out of chronological order)
     *   empty list: the postcondition is underdetermined (any zero-length
     *   interval is minimal), so we only assert start equals end.
     *
     * getMentionedUsers(tweets):
     *   partition on tweets.size(): 0, 1, >1
     *   partition on mentions per tweet: 0, 1, >1
     *   partition on position of mention: start, middle, end of text
     *   partition on case: same user in different cases ("@Bob", "@bob")
     *   partition on repetition: user mentioned once, more than once
     *   partition on username characters: letters only; digits, "_" and "-"
     *   partition on what surrounds "@":
     *       preceded by space/start, preceded by punctuation,
     *       preceded by a username character (email address, not a mention),
     *       lone "@" with no username after it (not a mention)
     *   partition on what ends the username: space, end of text, punctuation
     *   The returned case is underdetermined, so every assertion compares
     *   usernames after lowercasing them.
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T09:00:00Z");

    private static final Tweet tweet1 = new Tweet(1, "alyssa",
            "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle",
            "rivest talk in 30 minutes #hype", d2);

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    /** lowercases every element so assertions do not depend on returned case */
    private static Set<String> lower(Set<String> users) {
        Set<String> result = new HashSet<>();
        for (String u : users) {
            result.add(u.toLowerCase());
        }
        return result;
    }

    // ---------------- getTimespan ----------------

    // covers tweets.size() = 0
    @Test
    public void testGetTimespanEmptyList() {
        Timespan timespan = Extract.getTimespan(Collections.emptyList());
        assertEquals("expected zero-length timespan", timespan.getStart(),
                timespan.getEnd());
    }

    // covers tweets.size() = 1
    @Test
    public void testGetTimespanOneTweet() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1));
        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d1, timespan.getEnd());
    }

    // covers tweets.size() > 1, distinct timestamps, chronological order
    @Test
    public void testGetTimespanTwoTweets() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet2));
        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    // covers tweets.size() > 1, earliest and latest not at the ends of the list
    @Test
    public void testGetTimespanOutOfOrder() {
        Tweet middle = new Tweet(3, "user1", "middle", d1);
        Tweet earliest = new Tweet(4, "user2", "earliest", d3);
        Tweet latest = new Tweet(5, "user3", "latest", d2);
        Tweet alsoMiddle = new Tweet(6, "user4", "also middle", d1);
        Timespan timespan = Extract.getTimespan(Arrays.asList(middle, latest,
                earliest, alsoMiddle));
        assertEquals("expected start", d3, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    // covers tweets.size() > 1, all timestamps equal
    @Test
    public void testGetTimespanSameTimestamps() {
        Tweet a = new Tweet(3, "user1", "first", d1);
        Tweet b = new Tweet(4, "user2", "second", d1);
        Timespan timespan = Extract.getTimespan(Arrays.asList(a, b));
        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d1, timespan.getEnd());
    }

    // ---------------- getMentionedUsers ----------------

    // covers tweets.size() = 0
    @Test
    public void testGetMentionedUsersEmptyList() {
        Set<String> mentionedUsers =
                Extract.getMentionedUsers(Collections.emptyList());
        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    // covers tweets.size() = 1, 0 mentions
    @Test
    public void testGetMentionedUsersNoMention() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet1));
        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    // covers 1 mention in the middle, terminated by a space
    @Test
    public void testGetMentionedUsersOneMention() {
        Tweet tweet = new Tweet(3, "alyssa", "hey @bob how are you?", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        assertEquals("expected one user", new HashSet<>(Arrays.asList("bob")),
                lower(mentionedUsers));
    }

    // covers >1 mentions, at start and at end of text, terminated by end of text
    @Test
    public void testGetMentionedUsersStartAndEnd() {
        Tweet tweet = new Tweet(3, "alyssa", "@bob meet @charlie", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        assertEquals("expected two users",
                new HashSet<>(Arrays.asList("bob", "charlie")),
                lower(mentionedUsers));
    }

    // covers same user repeated with different case
    @Test
    public void testGetMentionedUsersCaseInsensitiveDuplicates() {
        Tweet tweet = new Tweet(3, "alyssa", "@Bob @bob @BOB @charlie", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        assertEquals("expected each user once", 2, mentionedUsers.size());
        assertEquals(new HashSet<>(Arrays.asList("bob", "charlie")),
                lower(mentionedUsers));
    }

    // covers "@" preceded by a username character: email address is not a mention
    @Test
    public void testGetMentionedUsersEmailIsNotMention() {
        Tweet tweet = new Tweet(3, "alyssa", "email me at bitdiddle@mit.edu", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        assertTrue("expected no mentions", mentionedUsers.isEmpty());
    }

    // covers lone "@" with no username after it
    @Test
    public void testGetMentionedUsersLoneAtSign() {
        Tweet tweet = new Tweet(3, "alyssa", "meet me @ 5pm", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        assertTrue("expected no mentions", mentionedUsers.isEmpty());
    }

    // covers username with digits, "_" and "-"; preceded and followed by punctuation
    @Test
    public void testGetMentionedUsersSpecialCharsAndPunctuation() {
        Tweet tweet = new Tweet(3, "alyssa", "thanks (@ben_bit-99), see you!", d1);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        assertEquals(new HashSet<>(Arrays.asList("ben_bit-99")),
                lower(mentionedUsers));
    }

    // covers tweets.size() > 1, same user mentioned across different tweets
    @Test
    public void testGetMentionedUsersMultipleTweets() {
        Tweet a = new Tweet(3, "alyssa", "@bob is great", d1);
        Tweet b = new Tweet(4, "charlie", "@david is awesome, right @BOB?", d2);
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(a, b));
        assertEquals(new HashSet<>(Arrays.asList("bob", "david")),
                lower(mentionedUsers));
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * Extract class that follows the spec. It will be run against several staff
     * implementations of Extract, which will be done by overwriting
     * (temporarily) your version of Extract with the staff's version.
     * DO NOT strengthen the spec of Extract or its methods.
     *
     * In particular, your test cases must not call helper methods of your own
     * that you have put in Extract, because that means you're testing a
     * stronger spec than Extract says. If you need such helper methods, define
     * them in a different class. If you only need them in this test class, then
     * keep them in this test class.
     */

}