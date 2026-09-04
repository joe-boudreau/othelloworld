#!/usr/bin/env python3
"""Render symmetry-folded Othello square weights as phase heatmaps.

Example:
    training/.venv/bin/python training/visualize_weights.py \
        weights/v2_evaluator/weights_v3_candidate.json
"""

import argparse
import json
import math
from pathlib import Path
from typing import Any

import matplotlib.pyplot as plt
from matplotlib import colors


SYMMETRY_CLASS = (
    0, 1, 1, 1, 1, 1, 1, 0,
    1, 2, 3, 4, 4, 3, 2, 1,
    1, 3, 5, 6, 6, 5, 3, 1,
    1, 4, 6, 7, 7, 6, 4, 1,
    1, 4, 6, 7, 7, 6, 4, 1,
    1, 3, 5, 6, 6, 5, 3, 1,
    1, 2, 3, 4, 4, 3, 2, 1,
    0, 1, 1, 1, 1, 1, 1, 0,
)
PHASES = ("early", "mid", "late")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("weights", type=Path, help="JSON file with early, mid, and late arrays")
    parser.add_argument(
        "-o", "--output", type=Path,
        help="Image path (default: <weights file stem>_heatmap.png next to the JSON file)",
    )
    parser.add_argument("--scale", type=float, default=1000.0, help="Scale labels and colours by this amount (default: 1000)")
    return parser.parse_args()


def load_weights(path: Path) -> dict[str, list[float]]:
    with path.open(encoding="utf-8") as source:
        raw: Any = json.load(source)
    if not isinstance(raw, dict):
        raise ValueError("The JSON root must be an object keyed by phase.")

    result: dict[str, list[float]] = {}
    for phase in PHASES:
        values = raw.get(phase)
        if not isinstance(values, list) or len(values) != 8:
            raise ValueError(f"'{phase}' must be an array containing exactly 8 weights.")
        if not all(isinstance(value, (int, float)) and math.isfinite(value) for value in values):
            raise ValueError(f"'{phase}' must contain only finite numbers.")
        result[phase] = [float(value) for value in values]
    return result


def board_for(weights: list[float], scale: float) -> list[list[float]]:
    return [[weights[SYMMETRY_CLASS[row * 8 + column]] * scale for column in range(8)] for row in range(8)]


def render(weights: dict[str, list[float]], scale: float, output: Path) -> None:
    boards = {phase: board_for(weights[phase], scale) for phase in PHASES}
    largest_absolute_value = max(abs(value) for board in boards.values() for row in board for value in row)
    # A symmetric scale makes zero neutral and values comparable between phases.
    limit = largest_absolute_value or 1.0
    normalization = colors.TwoSlopeNorm(vmin=-limit, vcenter=0, vmax=limit)
    colour_map = plt.get_cmap("RdBu_r")

    figure, axes = plt.subplots(1, 3, figsize=(15, 5.8), constrained_layout=True)
    image = None
    for axis, phase in zip(axes, PHASES):
        board = boards[phase]
        image = axis.imshow(board, cmap=colour_map, norm=normalization, interpolation="nearest")
        axis.set_title(phase.capitalize())
        axis.set_xticks(range(8), list("ABCDEFGH"))
        axis.set_yticks(range(8), [str(rank) for rank in range(1, 9)])
        axis.set_xticks([index - 0.5 for index in range(9)], minor=True)
        axis.set_yticks([index - 0.5 for index in range(9)], minor=True)
        axis.grid(which="minor", color="black", linewidth=1)
        axis.tick_params(which="minor", bottom=False, left=False)

        for row in range(8):
            for column in range(8):
                value = board[row][column]
                red, green, blue, _ = colour_map(normalization(value))
                text_colour = "white" if 0.2126 * red + 0.7152 * green + 0.0722 * blue < 0.5 else "black"
                # Rounded labels follow the requested convention: -190.8 becomes -191.
                axis.text(column, row, str(round(value)), ha="center", va="center", color=text_colour, fontsize=10, fontweight="medium")

    assert image is not None
    figure.colorbar(image, ax=axes, shrink=0.78, pad=0.03, label=f"Weight × {scale:g}")
    figure.suptitle("Othello positional weights (shared colour scale)", fontsize=15)
    output.parent.mkdir(parents=True, exist_ok=True)
    figure.savefig(output, dpi=180, bbox_inches="tight")
    plt.close(figure)


def main() -> None:
    args = parse_args()
    if args.scale <= 0:
        raise ValueError("--scale must be greater than zero.")
    output = args.output or args.weights.with_name(f"{args.weights.stem}_heatmap.png")
    render(load_weights(args.weights), args.scale, output)
    print(f"Wrote {output}")


if __name__ == "__main__":
    main()
