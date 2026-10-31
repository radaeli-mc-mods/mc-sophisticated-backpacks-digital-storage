"""Eased link-animation timeline; mirrors LinkedUpgradeAppearance.PHASES on the client."""


def ease_in_out_cubic(t):
    return 4 * t ** 3 if t < 0.5 else 1 - (-2 * t + 2) ** 3 / 2


def ease_out_cubic(t):
    return 1 - (1 - t) ** 3


# (first strip frame, last strip frame, start ms, duration ms, easing)
# Strip: 0 unlinked, 1..27 rim fill, 28..37 diagonal wipe, 38..64 rim retract.
PHASES = [
    (0, 27, 0, 380, ease_in_out_cubic),
    (27, 37, 380, 280, ease_out_cubic),
    (37, 64, 720, 360, ease_in_out_cubic),
]
DURATION = PHASES[-1][2] + PHASES[-1][3]


def frame_at(ms):
    """Fractional strip frame shown `ms` after the link."""
    frame = 0.0
    for first, last, start, length, ease in PHASES:
        if ms < start:
            break
        frame = first + (last - first) * ease(min(1.0, (ms - start) / length))
    return frame
