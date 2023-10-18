basedir="$1"
if [ "$1" == "" ] || [ $# -gt 1 ]; then
  echo "basedir is empty"
  exit 1
fi

cd $basedir/testAPKs/TaintBench

apkcount=$(ls -1q *.apk | wc -l)
if [ $apkcount -eq 39 ]; then
  echo "APKs already present, skipping download..."
  exit 0
fi

wget -nc https://github.com/TaintBench/TaintBench/releases/download/TaintBenchSuite/TaintBench.zip
unzip -n TaintBench.zip
rm TaintBench.zip
