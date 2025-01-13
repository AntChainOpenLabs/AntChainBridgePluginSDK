/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.pluginserver.cli.shell;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jline.reader.Binding;
import org.jline.reader.Candidate;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser.ParseContext;
import org.jline.reader.Reference;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.ReaderUtils;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.Levenshtein;
import org.jline.utils.Log;

public class GlLineReader extends LineReaderImpl {

    public GlLineReader(Terminal terminal) throws IOException {
        super(terminal);
    }

    public GlLineReader(Terminal terminal, String appName) throws IOException {
        super(terminal, appName);
    }

    public GlLineReader(Terminal terminal, String appName, Map<String, Object> variables) {
        super(terminal, appName, variables);
    }

    @Override
    protected boolean insertTab() {
        return isSet(Option.INSERT_TAB) && false;
    }

    @Override
    protected boolean doComplete(CompletionType lst, boolean useMenu, boolean prefix) {
        // Try to expand history first
        // If there is actually an expansion, bail out now
        try {
            if (expandHistory()) {
                return true;
            }
        } catch (Exception e) {
            Log.info("Error while expanding history", e);
            return false;
        }

        // Parse the command line
        ParsedLine line;
        try {
            line = parser.parse(buf.toString(), buf.cursor(), ParseContext.COMPLETE);
        } catch (Exception e) {
            Log.info("Error while parsing line", e);
            return false;
        }

        // Find completion candidates
        List<Candidate> candidates = new ArrayList<>();
        try {
            if (completer != null) {
                completer.complete(this, line, candidates);
            }
        } catch (Exception e) {
            Log.info("Error while finding completion candidates", e);
            return false;
        }

        if (lst == CompletionType.ExpandComplete || lst == CompletionType.Expand) {
            String w = expander.expandVar(line.word());
            if (!line.word().equals(w)) {
                if (prefix) {
                    buf.backspace(line.wordCursor());
                } else {
                    buf.move(line.word().length() - line.wordCursor());
                    buf.backspace(line.word().length());
                }
                buf.write(w);
                return true;
            }
            if (lst == CompletionType.Expand) {
                return false;
            } else {
                lst = CompletionType.Complete;
            }
        }

        boolean caseInsensitive = isSet(Option.CASE_INSENSITIVE);
        int errors = getInt(ERRORS, DEFAULT_ERRORS);

        // Build a list of sorted candidates
        NavigableMap<String, List<Candidate>> sortedCandidates =
            new TreeMap<>(caseInsensitive ? String.CASE_INSENSITIVE_ORDER : null);
        for (Candidate cand : candidates) {
            sortedCandidates
                .computeIfAbsent(AttributedString.fromAnsi(cand.value()).toString(), s -> new ArrayList<>())
                .add(cand);
        }

        // Find matchers
        // TODO: glob completion
        List<Function<Map<String, List<Candidate>>,
            Map<String, List<Candidate>>>> matchers;
        Predicate<String> exact;
        if (prefix) {
            String wp = line.word().substring(0, line.wordCursor());
            matchers = Arrays.asList(
                simpleMatcher(s -> s.startsWith(wp)),
                simpleMatcher(s -> s.contains(wp)),
                typoMatcher(wp, errors)
            );
            exact = s -> s.equals(wp);
        } else if (isSet(Option.COMPLETE_IN_WORD)) {
            String wd = line.word();
            String wp = wd.substring(0, line.wordCursor());
            String ws = wd.substring(line.wordCursor());
            Pattern p1 = Pattern.compile(Pattern.quote(wp) + ".*" + Pattern.quote(ws) + ".*");
            Pattern p2 = Pattern.compile(".*" + Pattern.quote(wp) + ".*" + Pattern.quote(ws) + ".*");
            matchers = Arrays.asList(
                simpleMatcher(s -> p1.matcher(s).matches()),
                simpleMatcher(s -> p2.matcher(s).matches()),
                typoMatcher(wd, errors)
            );
            exact = s -> s.equals(wd);
        } else {
            String wd = line.word();
            matchers = Arrays.asList(
                simpleMatcher(s -> s.startsWith(wd)),
                simpleMatcher(s -> s.contains(wd)),
                typoMatcher(wd, errors)
            );
            exact = s -> s.equals(wd);
        }
        // Find matching candidates
        Map<String, List<Candidate>> matching = Collections.emptyMap();
        for (Function<Map<String, List<Candidate>>,
            Map<String, List<Candidate>>> matcher : matchers) {
            matching = matcher.apply(sortedCandidates);
            if (!matching.isEmpty()) {
                break;
            }
        }

        // If we have no matches, bail out
        if (matching.isEmpty()) {
            return false;
        }

        // If we only need to display the list, do it now
        if (lst == CompletionType.List) {
            List<Candidate> possible = matching.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toList());
            doList(possible, line.word(), false);
            return !possible.isEmpty();
        }

        // Check if there's a single possible match
        Candidate completion = null;
        // If there's a single possible completion
        if (matching.size() == 1) {
            completion = matching.values().stream().flatMap(Collection::stream)
                .findFirst().orElse(null);
        }
        // Or if RECOGNIZE_EXACT is set, try to find an exact match
        else if (isSet(Option.RECOGNIZE_EXACT)) {
            completion = matching.values().stream().flatMap(Collection::stream)
                .filter(Candidate::complete)
                .filter(c -> exact.test(c.value()))
                .findFirst().orElse(null);
        }
        // Complete and exit
        if (completion != null && !completion.value().isEmpty()) {
            if (prefix) {
                buf.backspace(line.wordCursor());
            } else {
                buf.move(line.line().length() - line.cursor());
                buf.backspace(line.line().length());
            }
            buf.write(completion.value());
            if (completion.suffix() != null) {
                redisplay();

                Binding op = readBinding(getKeys());
                if (op != null) {
                    String chars = getString(REMOVE_SUFFIX_CHARS, DEFAULT_REMOVE_SUFFIX_CHARS);
                    String ref = op instanceof Reference ? ((Reference)op).name() : null;
                    if (SELF_INSERT.equals(ref) && chars.indexOf(getLastBinding().charAt(0)) >= 0
                        || ACCEPT_LINE.equals(ref)) {
                        buf.backspace(completion.suffix().length());
                        if (getLastBinding().charAt(0) != ' ') {
                            buf.write(' ');
                        }
                    }
                    pushBackBinding(true);
                }
            }

            List<Candidate> possible = matching.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toList());

            if (isSet(Option.AUTO_LIST)) {
                if (!doList(possible, line.word(), true)) {
                    return true;
                }
            }

            return true;
        }

        List<Candidate> possible = matching.entrySet().stream()
            .flatMap(e -> e.getValue().stream())
            .collect(Collectors.toList());

        if (useMenu) {
            buf.move(line.word().length() - line.wordCursor());
            buf.backspace(line.word().length());
            doMenu(possible, line.word());
            return true;
        }

        // Find current word and move to end
        String current;
        if (prefix) {
            current = line.word().substring(0, line.wordCursor());
        } else {
            current = line.word();
            buf.move(current.length() - line.wordCursor());
        }
        // Now, we need to find the unambiguous completion
        // TODO: need to find common suffix
        String commonPrefix = null;
        for (String key : matching.keySet()) {
            commonPrefix = commonPrefix == null ? key : getCommonStart(commonPrefix, key, caseInsensitive);
        }
        boolean hasUnambiguous = commonPrefix.startsWith(current) && !commonPrefix.equals(current);

        if (hasUnambiguous) {
            buf.backspace(current.length());
            buf.write(commonPrefix);
            current = commonPrefix;
            if ((!isSet(Option.AUTO_LIST) && isSet(Option.AUTO_MENU))
                || (isSet(Option.AUTO_LIST) && isSet(Option.LIST_AMBIGUOUS))) {
                if (!nextBindingIsComplete()) {
                    return true;
                }
            }
        }
        if (isSet(Option.AUTO_LIST)) {
            if (!doList(possible, current, true)) {
                return true;
            }
        }
        if (isSet(Option.AUTO_MENU)) {
            buf.backspace(current.length());
            doMenu(possible, line.word());
        }
        return true;
    }

    int getInt(String name, int def) {
        return ReaderUtils.getInt(this, name, def);
    }

    private String getCommonStart(String str1, String str2, boolean caseInsensitive) {
        int[] s1 = str1.codePoints().toArray();
        int[] s2 = str2.codePoints().toArray();
        int len = 0;
        while (len < Math.min(s1.length, s2.length)) {
            int ch1 = s1[len];
            int ch2 = s2[len];
            if (ch1 != ch2 && caseInsensitive) {
                ch1 = Character.toUpperCase(ch1);
                ch2 = Character.toUpperCase(ch2);
                if (ch1 != ch2) {
                    ch1 = Character.toLowerCase(ch1);
                    ch2 = Character.toLowerCase(ch2);
                }
            }
            if (ch1 != ch2) {
                break;
            }
            len++;
        }
        return new String(s1, 0, len);
    }

    private void pushBackBinding(boolean skip) {
        String s = getLastBinding();
        if (s != null) {
            bindingReader.runMacro(s);
            skipRedisplay = skip;
        }
    }

    String getString(String name, String def) {
        return ReaderUtils.getString(this, name, def);
    }

    private Function<Map<String, List<Candidate>>,
        Map<String, List<Candidate>>> simpleMatcher(Predicate<String> pred) {
        return m -> m.entrySet().stream()
            .filter(e -> pred.test(e.getKey()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private Function<Map<String, List<Candidate>>,
        Map<String, List<Candidate>>> typoMatcher(String word, int errors) {
        return m -> {
            Map<String, List<Candidate>> map = m.entrySet().stream()
                .filter(e -> distance(word, e.getKey()) < errors)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            if (map.size() > 1) {
                map.computeIfAbsent(word, w -> new ArrayList<>())
                    .add(new Candidate(word, word, "original", null, null, null, false));
            }
            return map;
        };
    }

    private int distance(String word, String cand) {
        if (word.length() < cand.length()) {
            int d1 = Levenshtein.distance(word, cand.substring(0, Math.min(cand.length(), word.length())));
            int d2 = Levenshtein.distance(word, cand);
            return Math.min(d1, d2);
        } else {
            return Levenshtein.distance(word, cand);
        }
    }

}
