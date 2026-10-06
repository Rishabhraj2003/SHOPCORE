#!/bin/sh
# 50 threads fight for 10 units of stock - checks that there is no overselling
java -cp out com.shopcore.demo.ConcurrencyDemo
