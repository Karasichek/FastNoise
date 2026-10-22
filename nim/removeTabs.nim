import osproc, strutils, sequtils, os, parseopt

var files: seq[string]
for i in 1 .. paramCount():
  var f = paramStr(i)
  if f.fileExists(): files.add(f)

for i in files:
  var og = readFile(i)
  var formatted = og.splitLines().mapIt(
    it.strip(leading = false).replace("\t", "  ")
  ).join("\n")
  if og != formatted:
    writeFile(i, formatted)
    echo i
