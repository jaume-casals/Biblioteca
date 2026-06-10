#!/usr/bin/env bash
# Loop opencode review every N minutes. Ctrl+C to stop.
# Usage: ./scripts/loop-review.sh [MINUTES]

PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

while true; do
    
    ( cd .. && opencode run  < scripts/loop-review-prompt.txt )

    sleep_sec=$((MINUTES * 60))
    sleep "$sleep_sec"
done
