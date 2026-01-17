#!/bin/bash
rm -f -- $TARGET_DIR/pid.txt
rm -f -- $TARGET_DIR/gattoslab*.jar
rm -rf -- $TARGET_DIR/scripts

mv ./resources/scripts $TARGET_DIR
