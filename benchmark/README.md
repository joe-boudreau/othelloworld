# Fixed benchmark positions

`positions-v1.csv` is the fixed set of 1,000 positions used to compare Othello
engines. `positions-v1-audit.csv` contains the same positions with their stage,
strength category, and frozen-reference score.

The set contains 600 approximately even positions and 100 positions in each of
the black, white, strong-black, and strong-white categories. It is split across
these stages:

- early: 4-12 occupied squares (334 positions)
- mid: 24-32 occupied squares (333 positions)
- late: 44-52 occupied squares (333 positions)

Candidates are generated with symmetry-reduced breadth-first search for the
early stage and reproducibly seeded exploratory V3 self-play for the mid and
late stages. Exact duplicates and all rotated/reflected equivalents are
removed. The candidates are scored from Black's perspective by deterministic
depth-7 alpha-beta search using the frozen V3 weights in `reference/v1`.

Regenerate and validate the files from the repository root with:

```shell
./gradlew generateBenchmarkPositions test
```

The expected SHA-256 for `positions-v1.csv` is:

```text
453061cec749c36f5422566f5b7ac5236a50ba931b504298c17d36116d640b55
```

Changing the positions, reference weights, or generation recipe should create
a new benchmark version rather than silently replacing v1.
