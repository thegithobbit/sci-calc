#!/usr/bin/env python3
"""
Independent verification of the symbolic differentiator from the Android app.

The reference derivatives are computed with SymPy, a computer algebra system for Python.
For every test case the script checks two things:

  1. symbolic equivalence: simplify(f_sympy^(n) - f_app^(n)) == 0;
  2. numerical agreement: |f_sympy^(n)(x0) - f_app^(n)(x0)| <= tol * max(1, |value|).

A case passes if at least one check succeeds (simplify() does not always find
a canonical form, e.g. for abs(x)). The app's results are exported to a TSV
file by the unit test DerivativeExportTest.

Usage:
    pip install -r tools/requirements.txt
    ./gradlew test                       # creates app/build/derivative-check/derivatives.tsv
    python tools/check_derivatives.py    # compare with SymPy

    python tools/check_derivatives.py --function "sin(x^2)" --order 2   # one function
"""

import argparse
import csv
import sys
from pathlib import Path

import sympy as sp
from sympy.parsing.sympy_parser import (
    convert_xor,
    implicit_multiplication_application,
    parse_expr,
    standard_transformations,
)

x = sp.Symbol("x", real=True)

# The app's syntax -> SymPy objects. Note: in the app log() is base 10, ln() is natural.
NAMES = {
    "x": x,
    "e": sp.E,
    "pi": sp.pi,
    "sin": sp.sin, "cos": sp.cos, "tan": sp.tan, "cot": sp.cot,
    "asin": sp.asin, "acos": sp.acos, "atan": sp.atan,
    "sinh": sp.sinh, "cosh": sp.cosh, "tanh": sp.tanh,
    "ln": sp.log,
    "log": lambda a: sp.log(a, 10),
    "sqrt": sp.sqrt,
    "exp": sp.exp,
    "abs": sp.Abs,
}

# "^" as power and implicit multiplication ("2x", "3sin(x)") as in the app
TRANSFORMATIONS = standard_transformations + (implicit_multiplication_application, convert_xor)

DEFAULT_TSV = Path(__file__).resolve().parent.parent / "app" / "build" / "derivative-check" / "derivatives.tsv"
TOLERANCE = 1e-9


def to_sympy(text: str) -> sp.Expr:
    """Converts an expression written in the app's notation into a SymPy expression."""
    text = (text.replace("·", "*").replace("×", "*").replace("÷", "/")
                .replace("−", "-").replace("√", "sqrt").replace("π", "pi"))
    return parse_expr(text, local_dict=NAMES, transformations=TRANSFORMATIONS)


def symbolically_equal(a: sp.Expr, b: sp.Expr) -> bool:
    try:
        return sp.simplify(a - b) == 0
    except Exception:  # simplify may fail on exotic expressions; numeric check still applies
        return False


def check_file(path: Path) -> int:
    if not path.exists():
        print(f"File not found: {path}\nRun './gradlew test' first.", file=sys.stderr)
        return 2

    with path.open(encoding="utf-8") as fh:
        rows = list(csv.DictReader(fh, delimiter="\t"))

    reference_cache = {}   # (function, order) -> (reference derivative, symbolic check result)
    failures = 0
    max_error = 0.0

    print(f"{'f(x)':<18}{'n':>2}  {'x0':>4}  {'app':>14}  {'sympy':>14}  {'abs. error':>10}  result")
    print("-" * 82)

    for row in rows:
        function, order = row["function"], int(row["order"])
        x0, app_value = float(row["x0"]), float(row["value"])

        key = (function, order)
        if key not in reference_cache:
            reference = sp.diff(to_sympy(function), x, order)
            same = symbolically_equal(reference, to_sympy(row["derivative"]))
            reference_cache[key] = (reference, same)
        reference, symbolic_ok = reference_cache[key]

        ref_value = float(reference.subs(x, x0).evalf())
        error = abs(ref_value - app_value)
        max_error = max(max_error, error)
        numeric_ok = error <= TOLERANCE * max(1.0, abs(ref_value))

        if symbolic_ok:
            status = "OK (symbolic)"
        elif numeric_ok:
            status = "OK (numeric)"
        else:
            status = "FAIL"
            failures += 1

        print(f"{function:<18}{order:>2}  {x0:>4}  {app_value:>14.8g}  {ref_value:>14.8g}  {error:>10.1e}  {status}")

    symbolic_count = sum(1 for _, same in reference_cache.values() if same)
    print("-" * 82)
    print(f"Cases: {len(rows)}, failed: {failures}, max abs. error: {max_error:.2e}")
    print(f"Symbolically equivalent derivatives: {symbolic_count} of {len(reference_cache)}")
    return 1 if failures else 0


def show_single(function: str, order: int) -> int:
    f = to_sympy(function)
    d = sp.simplify(sp.diff(f, x, order))
    print(f"f(x)      = {f}")
    print(f"f^({order})(x) = {d}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Check the app's derivatives against SymPy.")
    parser.add_argument("tsv", nargs="?", type=Path, default=DEFAULT_TSV,
                        help="file exported by DerivativeExportTest (default: %(default)s)")
    parser.add_argument("--function", help="differentiate a single function instead of checking the file")
    parser.add_argument("--order", type=int, default=1, choices=(1, 2, 3))
    args = parser.parse_args()

    if args.function:
        return show_single(args.function, args.order)
    return check_file(args.tsv)


if __name__ == "__main__":
    sys.exit(main())
