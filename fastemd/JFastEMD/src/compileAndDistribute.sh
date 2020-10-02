rm jfastemd.jar
javac com/telmomenezes/jfastemd/*.java
jar -cvf jfastemd.jar com/telmomenezes/jfastemd/*.class
cp jfastemd.jar ~/Workspace/cluster-method/xcluster/src/java/
cp jfastemd.jar ~/Workspace/intraInterIndex/src/
