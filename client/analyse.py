#!signal-analysis/bin/python

import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

tss = []
firsts = []
ecgs = []
with open("result.csv", "r") as f:
    result = list(map(lambda line: list(map(int, line.split(","))), f.readlines()))
    for datas in result:
        tss.append(datas[0])
        firsts.append(datas[1])
        ecgs+=datas[1:]

plt.scatter(x=tss, y=firsts, marker='.')
plt.show()