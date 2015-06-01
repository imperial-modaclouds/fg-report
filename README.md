# MODAClouds FG Reporter
Filling the Gap Report Generator

To run it
```
java -jar fg-report-<version>.jar dataFile reportFolder 
```
where dataFile is the file which FG Anazer generates containg the json data and reportFolder is the the folder where FG Reporter will generate the report.

Notice: while compiling the code, please in the eclipse do Export->Runnable JAR file->Package required libraries into generated JAR. Compiling from Maven will report missing font.xml file.
