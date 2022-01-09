#!/usr/bin/env bash

rsync -ru /Users/mjankows/agh/mgr/ii/ssp/labs/lab9/floodlight-1.2-lab7/* floodlight@10.0.0.13:~/floodlight-1.2-lab7
ssh floodlight@10.0.0.13 "cd /home/floodlight/floodlight-1.2-lab7/floodlight-1.2-lab7; ./run.sh"