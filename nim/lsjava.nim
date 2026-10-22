import osproc, strutils, sequtils, os, parseopt

proc run(cmd: string): string =
  var r = execCmdEx(cmd)
  if r[1] != 0: quit(r[1])
  r[0]

proc getNewOrChangedFiles(): seq[string] =
  run("git status -u --porcelain").splitLines().filterIt(
    not it.isEmptyOrWhitespace
  ).mapIt(it.splitWhitespace[1])

proc getAllFiles(): seq[string] =
  var res = run("git ls-files").splitLines().filterIt(
    not it.isEmptyOrWhitespace
  )

  for i in getNewOrChangedFiles(): res.addUnique(i)
  return res

var all = false
var format = ".java"
var hasHadArgument = false

for kind, key, val in getopt(commandLineParams()):
  case kind
  of cmdShortOption:
    if (key == "a"): all = true
  of cmdArgument:
    if hasHadArgument:
      echo "Cannot use more than one file extension"
      quit(1)
    hasHadArgument = true
    format = key
  else: discard

var newFiles = (if all: getAllFiles() else: getNewOrChangedFiles()).filterIt(
  it.fileExists() and it.splitFile().ext == format
)

for i in newFiles: echo i
