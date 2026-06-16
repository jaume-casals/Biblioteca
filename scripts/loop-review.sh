#!/usr/bin/env bash
while true; do
( cd .. && opencode run  < scripts/loop-review-prompt.txt )
  sleep 1800
done
