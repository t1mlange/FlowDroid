#!/usr/bin/env python3

import json
import sys
import glob
import re
from typing import List

def containsAttribute(attributes: dict, wantedAttr: str):
    for attr in attributes.keys():
        if attr == wantedAttr and attributes[attr] == True:
            return True
    return False

sigRegex = r"<.*?>"
mRegex = r"(public|protected|private) (.*)"
def getDef(stmt: dict) -> str:
    # print(stmt["IRs"][0])
    sig = re.search(sigRegex, stmt["IRs"][0]["IRstatement"]).group()
    method = re.search(mRegex, stmt["methodName"]).group(2)
    className = stmt["className"]
    return f"{sig} in <{className}: {method}>"
    

def parseCollectionFlow(data: dict):
    apk = data["fileName"]
    for finding in data["findings"]:
        source = getDef(finding["source"])
        sink = getDef(finding["sink"])
        isNegative = finding["isNegative"]
        isCollectionFlow = containsAttribute(finding["attributes"], "collections")

        if isNegative or isCollectionFlow:
            sources.add(source)
            sinks.add(sink)

        if not isNegative and isCollectionFlow:
            expectedFlows.append(f"resultMap.put(\"{apk}\", new Pair<>(\"{source}\", \"{sink}\"))")

sources = set()
sinks = set()
expectedFlows = []
def main(args: List[str]):
    if len(args) != 2:
        print("Missing folder!")
        exit(1)

    for fPath in glob.glob(args[1] + "/*.json"):
        with open(fPath) as f:
            data = json.load(f)
            parseCollectionFlow(data)

if __name__ == "__main__":
    main(sys.argv)
