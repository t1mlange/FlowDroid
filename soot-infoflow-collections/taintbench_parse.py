#!/usr/bin/env python3

import json
import sys
import glob
import re
from typing import List, Tuple

ssmap = set()
expectedFlows = set()
testcase = []


def containsAttribute(attributes: dict, wantedAttr: str):
    for attr in attributes.keys():
        if attr == wantedAttr and attributes[attr] == True:
            return True
    return False


sigRegex = r"<.*>"
mRegex = r"(public|protected|private)? (abstract|static)?(.*?) (.*?)\(.*?\)"


def getDef(stmt: dict) -> Tuple[str, str, str]:
    # print(stmt["IRs"][0])
    sig = re.search(sigRegex, stmt["IRs"][0]["IRstatement"]).group()
    methodr = re.search(mRegex, stmt["methodName"])
    className = stmt["className"]
    return sig, className, methodr.group(4)


def parseCollectionFlow(data: dict):
    global sources, sinks
    apk = data["fileName"]
    for finding in data["findings"]:
        sourceSig, sourceClass, sourceCaller = getDef(finding["source"])
        sinkSig, sinkClass, sinkCaller = getDef(finding["sink"])
        isNegative = finding["isNegative"]
        isCollectionFlow = containsAttribute(finding["attributes"], "collections")

        if isCollectionFlow:
            ssmap.add(f"ssMap.put(\"{apk}\", \"{sourceSig} -> _SOURCE_\");")
            ssmap.add(f"ssMap.put(\"{apk}\", \"{sinkSig} -> _SINK_\");")
            obj = "NegativeResult" if isNegative else "ExpectedResult"
            expectedFlows.add(
                f"resultMap.put(\"{apk}\", new {obj}(\"{sourceSig}\", \"{sourceClass}\", \"{sourceCaller}\", \"{sinkSig}\", \"{sinkClass}\", \"{sinkCaller}\"));")

    testcase.append(f"""
    @Test
    public void test{apk.replace('.apk', '').capitalize()}() throws XmlPullParserException, IOException {'{'}
        SetupApplication app = initApplication(pathToAPKs + \"/\" + \"{apk}\");
        app.addResultsAvailableHandler((cfg, results) -> compareResults(\"{apk}\", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks(\"{apk}\"));
        System.out.println(\"{apk}: \" + results.getResultSet().size());
    {'}'}
    """)

def main(args: List[str]):
    global expectedFlows

    if len(args) != 2:
        print("Missing folder!")
        exit(1)

    for fPath in glob.glob(args[1] + "/*.json"):
        with open(fPath) as f:
            data = json.load(f)
            parseCollectionFlow(data)

    for ss in sorted(ssmap):
        print(ss)
    for expectedFlows in sorted(expectedFlows):
        print(expectedFlows)

    for tc in sorted(testcase):
        print(tc)

if __name__ == "__main__":
    main(sys.argv)
