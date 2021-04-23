rm ../../input.txt;
rm report.txt;
for filename in ../../input*;
do
  cp "$filename" ../../input.txt;
  #echo "RUNNING $filename"
  output="../../output${filename:11}"
  #echo "COMPARE WITH $output"
  java homework.java;
  if test -f "$output"; then
    echo "$output exists."
  else
    echo "$output does not exist."
  fi
  diff output.txt "$output"
done >> report.txt
