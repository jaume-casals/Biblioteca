#!/usr/bin/env bash
# Meta-loop: every N minutes, ask opencode to propose an improved version of
# scripts/loop-review-prompt.txt. The optimizer is READ-ONLY on
# loop-review-prompt.txt; it emits a diff in loop-prompt-tune.log that the
# user reviews and applies manually. Never modifies production code.
#
# Usage: ./scripts/loop-prompt-tune.sh [MINUTES] [MAX_ITERS]
#   MINUTES   seconds between iterations (default 60). Accepts fractional values, e.g. 0.5.
#   MAX_ITERS stop after this many iterations (default 0 = forever).
#
# Stop with Ctrl+C (SIGINT/SIGTERM).


while true; do
   ( cd .. && opencode run  < scripts/loop-prompt-tune-prompt.txt ) 

    # Convert MINUTES (may be fractional) to seconds for sleep. awk is inOSIX and handles the multiplication portably.
    sleep_sec=$(awk -v m="$MINUTES" 'BEGIN { printf "%.0f", m * 60 }')
    if [ "$sleep_sec" -lt 1 ]; then
        sleep_sec=1
    fi
    printf '\033[1;30mSleeping %s min...\033[0m\n' "$MINUTES"
    sleep "$sleep_sec"
done
