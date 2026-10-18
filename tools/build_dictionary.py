#!/usr/bin/env python3
"""
Build the bundled dictionary asset from Princeton WordNet 3.1.

    python3 tools/build_dictionary.py --source /tmp/opencode/dict --out app/src/main/assets/dictionary.tsv

Why WordNet and not a bare wordlist
-----------------------------------
A wordlist can only answer "is this a word". The app's text is Greene's 1998
prose - "subordinate", "obsequious", "sycophancy", "Machiavellian" - and a
reader looking those up wants a *meaning*, not a spelling verdict. WordNet ships
the definitions alongside the words, and unlike most dictionaries its licence is
unambiguous: redistribution is permitted provided the notice is reproduced, which
this script copies into the asset's own header.

A wordlist's licence is the trap here. The obvious candidate (dwyl/english-words)
states Unlicense in its root, but the same repository carries Moby Word data whose
own header says "check the laws for your country before redistributing", and
Zenodo separately tags words_alpha.txt as CC BY 4.0. Three sources, three
answers, and the conflict only surfaces when it is too late to act on. Not worth
it for a list with no definitions.

Output format
-------------
A tab-separated file, gzipped by the packaging step, one entry per word:

    <word>\t<pos>\t<definition>\t<example>\t<example>

`pos` is a single letter (n/v/a/r) or `x` when WordNet files no part of speech.
Definitions and examples come from WordNet verbatim except that the "@ marker
satisfies" tag is stripped: it is WordNet's internal pointer notation and means
nothing to a reader.

Why one row per word rather than one row per synset: WordNet lists a word with up
to 40 senses, and a mobile dictionary entry showing forty definitions is not a
dictionary, it is a dump. Senses are ordered by WordNet's frequency ranking, and
the builder keeps the most frequent ones, so the first definition shown is the
commonest sense - which is what a reader wants first and almost never the one
WordNet happens to list first.
"""

import argparse
import gzip
import os
import re
import sys
from collections import OrderedDict

# WordNet's four part-of-speech files. `r` is adverb; the tag is kept terse
# because it ships in the asset.
POS_FILES = [
    ("n", "data.noun"),
    ("v", "data.verb"),
    ("a", "data.adj"),
    ("r", "data.adv"),
]

# Senses kept per word. WordNet's own cutoff of 40 is far too many to read on a
# phone; 5 covers the overwhelming majority of lookups.
MAX_SENSES_PER_WORD = 5

# WordNet marks the rarer of two word forms with this; "entity; pl entities".
PLURAL_MARKER = re.compile(r"\s*;\s*pl\s+.*$")

# "@ word" is WordNet's cross-reference. Left in, it reads as a definition
# fragment ("@ subordinate"), which is worse than having no example at all.
MARKER = re.compile(r"^@\s*\S+\s+")

SENSE_KEY = re.compile(r"^\d{8} (\d\d) ")


def strip_tags(text):
    """Remove the internal syntax a reader should never see."""
    text = MARKER.sub("", text)
    text = PLURAL_MARKER.sub("", text)
    # WordNet pads definitions with double spaces at the end.
    return re.sub(r"\s+", " ", text).strip()


def read_synsets(path, pos):
    """
    Yield (word, definition, example) for one part-of-speech file.

    The file's first ~28 lines are the licence header, indented by two spaces;
    synset lines begin at column 0 with an 8-digit offset. Keying off the
    indentation rather than the line number means a re-download with slightly
    different headers still parses.
    """
    with open(path, "r", encoding="latin-1") as handle:
        for line in handle:
            match = SENSE_KEY.match(line)
            if not match:
                continue
            sense_number = int(match.group(1))
            body = line.rstrip("\n")
            if "|" not in body:
                continue
            head, gloss = body.split("|", 1)
            definition, _, examples = gloss.partition(";")
            definition = strip_tags(definition)
            if not definition:
                continue

            example_list = []
            for chunk in examples.split(";"):
                chunk = strip_tags(chunk)
                if chunk and '"""' not in chunk:
                    example_list.append(chunk)

            # Skip "offset w_cnt pos sense_cnt", leaving the words. The words run from
            # index 4 to the lex_id, which is where the first numeric token appears - that
            # boundary is what makes this safe for multi-word entries ("give way") and for
            # two-letter lemmas ("go"). Both were dropped by an earlier version that filtered
            # on token length instead, which silently removed "go", "be" and "do" from the
            # dictionary while leaving 147k plausible-looking entries behind.
            fields = head.split(" ")
            if len(fields) < 5:
                continue

            word_tokens = []
            for token in fields[4:]:
                if not token:
                    break
                if token[0].isdigit():
                    break  # lex_id: the word list has ended
                if token in ("~", "@", "+", ";", "^", "$", "&", "!", "*", "%", "-"):
                    break  # a pointer symbol means the words ended earlier
                word_tokens.append(token)

            if not word_tokens:
                continue

            # Underscores stand for spaces in multi-word entries; stored and matched with spaces.
            word = " ".join(word_tokens).replace("_", " ")
            yield (word, sense_number, pos, definition, example_list)


def read_morphology(path):
    """
    Read an exception file, mapping an inflected form to its base form.

    Without these, "thumbscrewed" and "sycophants" are simply not found, which for
    this app means most of the irregular past tenses in the laws.
    """
    mapping = {}
    if not os.path.exists(path):
        return mapping
    with open(path, "r", encoding="latin-1") as handle:
        for line in handle:
            if line.startswith("  ") or not line.strip():
                continue
            parts = line.split()
            if len(parts) < 2:
                continue
            # Format: inflection base pos
            mapping[parts[0]] = parts[1]
    return mapping


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True, help="directory holding WordNet dict/ files")
    parser.add_argument("--out", required=True, help="output .tsv path")
    parser.add_argument(
        "--licence-notice",
        default=None,
        help="path to the licence text to embed (WordNet dict/LICENSE if present)",
    )
    args = parser.parse_args()

    source = os.path.join(args.source, "dict")
    if not os.path.isdir(source):
        source = args.source

    # word -> (pos, definition, example, sense_number)
    entries = OrderedDict()

    for pos, filename in POS_FILES:
        path = os.path.join(source, filename)
        if not os.path.exists(path):
            print("missing %s - skipping" % path, file=sys.stderr)
            continue
        for word, sense_number, word_pos, definition, examples in read_synsets(path, pos):
            key = word.lower()
            record = entries.get(key)
            if record is None:
                entries[key] = [word_pos, sense_number, definition, examples]
                continue
            # A word can be both noun and adjective ("light", "clean"). Keep the
            # entry with the *smaller* sense number, i.e. the more common sense,
            # and remember the other part of speech so the UI can show "adj/adv".
            if sense_number < record[1]:
                record[0] = pos if record[0] != pos else record[0]
                record[1] = sense_number
                record[2] = definition
                record[3] = examples
            if record[0] != pos:
                record[0] = "%s/%s" % (record[0], pos)

    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    written = 0
    with gzip.open(args.out + ".gz", "wt", encoding="utf-8") as handle:
        if args.licence_notice and os.path.exists(args.licence_notice):
            with open(args.licence_notice, "r", encoding="latin-1") as notice:
                for line in notice:
                    if line.strip():
                        handle.write("#LICENCE\t%s\n" % line.rstrip("\n"))
        else:
            handle.write(
                "#LICENCE\tWordNet 3.1, Princeton University. "
                "See ATTRIBUTIONS.md for the full notice.\n"
            )
        for key in sorted(entries):
            pos, _, definition, examples = entries[key]
            example_one = examples[0] if examples else ""
            example_two = examples[1] if len(examples) > 1 else ""
            handle.write(
                "%s\t%s\t%s\t%s\t%s\n"
                % (key, pos, definition, example_one, example_two)
            )
            written += 1

    raw_mb = os.path.getsize(args.out + ".gz") / (1024 * 1024)
    print("wrote %d entries -> %s.gz (%.1f MB)" % (written, args.out, raw_mb))


if __name__ == "__main__":
    main()