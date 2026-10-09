/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract consists of methods that extract information from a list of tweets.
 *
 * DO NOT change the method signatures and specifications of these methods, but
 * you should implement their method bodies, and you may add new public or
 * private methods or classes if you like.
 */
public class Extract {

    /**
     * A username-mention: "@" followed by one or more username characters
     * (letters, digits, underscore, hyphen), where the "@" is NOT immediately
     * preceded by a username character. The greedy "+" means the match also
     * cannot be immediately followed by a username character.
     */
    private static final Pattern MENTION =
            Pattern.compile("(?<![A-Za-z0-9_-])@([A-Za-z0-9_-]+)");

    /**
     * Get the time period spanned by tweets.
     *
     * @param tweets
     *            list of tweets with distinct ids, not modified by this method.
     * @return a minimum-length time interval that contains the timestamp of
     *         every tweet in the list.
     */
    public static Timespan getTimespan(List<Tweet> tweets) {
        if (tweets.isEmpty()) {
            // Every interval vacuously contains all timestamps of an empty list,
            // so any zero-length interval is a minimum-length answer. The spec
            // leaves the exact instant underdetermined; we pick the epoch.
            return new Timespan(Instant.EPOCH, Instant.EPOCH);
        }

        Instant earliest = tweets.get(0).getTimestamp();
        Instant latest = earliest;
        for (Tweet tweet : tweets) {
            Instant t = tweet.getTimestamp();
            if (t.isBefore(earliest)) {
                earliest = t;
            }
            if (t.isAfter(latest)) {
                latest = t;
            }
        }
        return new Timespan(earliest, latest);
    }

    /**
     * Get usernames mentioned in a list of tweets.
     *
     * @param tweets
     *            list of tweets with distinct ids, not modified by this method.
     * @return the set of usernames who are mentioned in the text of the tweets.
     *         A username-mention is "@" followed by a Twitter username (as
     *         defined by Tweet.getAuthor()'s spec).
     *         The username-mention cannot be immediately preceded or followed by any
     *         character valid in a Twitter username.
     *         For this reason, an email address like bitdiddle@mit.edu does NOT
     *         contain a mention of the username mit.
     *         Twitter usernames are case-insensitive, and the returned set may
     *         include a username at most once.
     */
    public static Set<String> getMentionedUsers(List<Tweet> tweets) {
        Set<String> mentioned = new HashSet<>();
        for (Tweet tweet : tweets) {
            Matcher m = MENTION.matcher(tweet.getText());
            while (m.find()) {
                // normalise to lower case so "@Bob" and "@bob" count once
                mentioned.add(m.group(1).toLowerCase());
            }
        }
        return mentioned;
    }

}