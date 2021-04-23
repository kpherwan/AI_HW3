rm ../../input.txt;
rm report.txt;
for filename in ../../input_*;
do
  cp "$filename" ../../input.txt;
  echo "RUNNING $filename" >> report.txt
  output="../../output${filename:11}"
  echo "COMPARE WITH $output" >> report.txt

  java homework.java;

  diff output.txt "$output"
  echo "\n"
done >> report.txt
