#!/bin/bash
set -eu

failures=0

for run in `seq 20`; do
    if ! rspec -e 'the bad test'; then
        failures=$(($failures + 1))
    fi

    echo "Test run $run complete, $failures failures"
done
