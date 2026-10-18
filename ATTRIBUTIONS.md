# Attributions

Third-party material bundled in this app, and where it came from.

The app is offline and holds no INTERNET permission, so nothing here is fetched at runtime. All of
it ships inside the APK.

---

## Princeton WordNet 3.1

**Used for:** the offline dictionary. Bundled as `app/src/main/assets/dictionary.tsv.gz`,
rebuilt by `tools/build_dictionary.py`.

**Source:** https://wordnetcode.princeton.edu/wn3.1.dict.tar.gz

**Licence:** WordNet 3.1 is distributed by Princeton University under a permissive licence that
grants use, copying, modification and distribution for any purpose, with one condition — the
following notice must appear on all copies:

> WordNet 3.1 Copyright 2011 by Princeton University. All rights reserved.
>
> THIS SOFTWARE AND DATABASE IS PROVIDED "AS IS" AND PRINCENTON UNIVERSITY MAKES NO
> REPRESENTATIONS OR WARRANTIES OF ANY KIND, EXPRESS OR IMPLIED. BY WAY OF EXAMPLE, BUT NOT
> LIMITATION, PRINCENTON UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES OF MERCHANT-
> ABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE,
> DATABASE OR DOCUMENTATION WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS,
> TRADEMARKS OR OTHER RIGHTS. THE NAME OF PRINCENTON UNIVERSITY OR PRINCETON MAY NOT BE USED IN
> ADVERTISING OR PUBLICITY PERTAINING TO DISTRIBUTION OF THE SOFTWARE OR DATABASE OR ANY
> DOCUMENTATION. TITLE TO COPYRIGHT IN THIS SOFTWARE, DATABASE AND ANY ASSOCIATED DOCUMENTATION
> SHALL AT ALL TIMES REMAIN WITH PRINCENTON UNIVERSITY AND LICENSEE AGREES TO PRESERVE SAME.

This notice is also embedded in the asset itself, as `#LICENCE` lines at the top of
`dictionary.tsv.gz`, so it travels with the data even if the asset is redistributed on its own.
`DictionaryCoverageTest` asserts that header is present.

### What was changed from the original

- Definitions, examples and lemmas are copied verbatim from WordNet.
- WordNet's internal notation is stripped where it would confuse a reader: the `@ marker`
  cross-reference prefix, and the `; pl ...` plural suffix.
- Multi-word lemmas have underscores replaced with spaces, so `give_way` is stored as `give way`.
- Only the senses WordNet ranks most frequent are kept, one entry per word. WordNet ships up to 40
  senses for a word; showing forty definitions on a phone is a data dump, not a dictionary.

### Known limitations

WordNet is a general-English lexicon dated 1993. It carries **no proper nouns** in its lemmas for
ordinary use (`Machiavelli` and `Machiavellian` are present as historical terms, but `Catherine de'
Medici`, `Bayezid` and `Ptolemy` are not), and it lacks modern coinages and some plain words
(`observance`, `servility` and `undermine` are all absent). The reader is told which dictionary was
consulted when a lookup misses, so a "not found" reads as a fact about WordNet rather than a bug.

Inflected forms are resolved by `Stemmer.kt` at lookup time rather than by expanding the asset,
because WordNet itself resolves them at query time with a stemmer that is not part of the
distribution.

---

## Robert Greene, *The 48 Laws of Power* (1998)

**Used for:** the text of the laws themselves — the core judgements, observances,
transgressions and reversals — and the law titles.

The laws are Greene's own synthesis and are **not** public domain. This app presents them as
quotation and commentary for personal reading. Exported notes carry the attribution so a file
leaving the app still says where it came from.

---

## Images

Portraits and illustrative photographs are openly licensed and credited in-app on each image's
detail page. No AI-generated or stock-photography faces are used.