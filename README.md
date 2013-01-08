reputation
==========

This is the (extended version) of the model presented in my paper "Have you Done Anything Like That? Predicting Performance Using Inter-category Reputation". 


Run "Reputation.java" with "--help" to see all available optinos:

Available options:
------------------------------------------------------------------
-t		   	Train: crete regression files.
-g 			Generate the necessary training files from the raw instances.  
-e			Run evaluation. 
-w			Print results to files.  
-cv			Run cross validation
-f			Number of folds for cross validation.
-s			Split files to -f number of folds. The spliting is by contractor.
-p	 		Output predictions to "predictions.csv". 
-a			Show average accuracies for cross validation.
-n			Evaluate on train set.
------------------------------------------------------------------

The raw input file should be a csv in the form:

contractor,category,score 

All you have to change is the paths and the hierarchy levels in the config.properties file.
The code supports only two levels at this point: r, and its children, rl and rr.

A simple run with "train.csv" and "test.csv" raw data files wiill be the following:

java Reputation -g -t -e 


