#!/usr/bin/env bash

while true; do
  ( cd .. && opencode run  < scripts/loop-prompt-tune-prompt.txt )
  sleep 3600
done
