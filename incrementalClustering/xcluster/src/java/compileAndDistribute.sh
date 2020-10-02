#rm jperch.jar
javac -cp .:jfastemd.jar:moa-2019.05.1-SNAPSHOT.jar com/jperch/*.java
jar -cvf jperch.jar com/jperch/*.class
cp jperch.jar ~/Workspace/intraInterIndex/src/
