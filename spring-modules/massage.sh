# Script for massaging manifest lines

for A in */src/main/resources/META-INF/MANIFEST.MF; 
do echo $A; 
  cp $A $A.bak;
  cat $A.bak | sed -e '/Import-Package: */,/^.*[^,] *$/{s/\"\?\${spring.version}\"\?/\"\${spring.import.version}\"/g}' > $A;
done
