#!/bin/bash

kill -9 `cat STB_1000_Sim.pid`
kill -9 `cat STB_2000_Sim.pid`
kill -9 `cat STB_3000_Sim.pid`

kill -9 `cat Technician_1000_Sim.pid`
kill -9 `cat Technician_2000_Sim.pid`
kill -9 `cat Technician_3000_Sim.pid`
