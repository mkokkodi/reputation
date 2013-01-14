Reputation
==========

This is the (extended version) of the model presented in my paper "Have you Done Anything Like That? Predicting Performance Using Inter-category Reputation". 


Run "Reputation.java" with "--help" to see all available options. 

Available options:

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

The raw input file should be a csv in the form:

contractor,category,score 

All you have to change is the paths and the hierarchy levels in the config.properties file.
The code supports only two levels at this point: r, and its children, rl and rr.

A simple run with "train.csv" and "test.csv" raw data files wiill be the following:

java Reputation -g -t -e 


How to run Synthetic experiments in three steps:
----------------------------------------------

Sparse data - Hierarchies:

Create the raw files "syn\_cluster\_test.csv", "syn\_cluster\_train.csv" in the form contractor,category,score.
In the config. properties file setup:

hierarchyStructure=r,rl,rr
r=overall,non-technical,technical
r-realMap=non-technical:rl,technical:rr
rl=overall,writing,administrative,sales-and-marketing,one-more
rr=overall,web-dev,soft-dev,des-mult,two-more
category-mapping=0:overall,10:non-technical,20:technical,1:web-dev,2:soft-dev,5:writing,6:administrative,3:des-mult,7:sales-and-marketing,4:two-more,8:one-more
category-to-root=1:20,2:20,3:20,4:20,5:10,6:10,7:10,8:10
basedon=r:\_BasedOn\_0\_10\_20,rl:\_BasedOn\_0\_5\_6\_7\_8,rr:\_BasedOn\_0\_1\_2\_3\_4

1. Run Rputation -g -t  -p -n -e  -w  -yc
2. Run EMComputeLamabda 
3. Run Reputation -e -w -yc

No hierarchies:
Create the raw files "syn\_test\_cat3","syn\_train\_cat3", where 3 is the number of ctagories.
make Sure in the config.properties file you have:
hierarchyStructure=r
r=overall,non-technical,technical,one-more
category-mapping=0:overall,1:non-technical,2:technical,3:extra
basedon=r:\_BasedOn\_0\_1\_2\_3

1. Run Reputation -g -t -p -n -e -w -y
2. Run EMComputeLambda -c 3 (the number of categories)
3. Run Reputation -e -2 -y


How to run Cross Validation:
------------------------------
In the config.properties have everything the same as in the hold out evaluation. 
Remember to check outOfScore
1. Split the files to some folds: Run Reputation -cv -s -f 10
2. Generate and Train. Run reputation: -g -t -cv  -f 10 
3. Evaluate on Train. Run reputation -e -p -w -n -cv -f 10 (or combine steps 2 and 3)
4. Run EMComputeLambdas -c (for cross validation)


How to run hold out data:
----------------------------
Set up your config.properties. (including: outOfScore, hierarchy structure, priors, thresholds, mappings)
1.Create Train and evaluate on train: Reputation -g -t -p -w -n -e 
2. Run EMComputeLambdas -r (option to denote real)
3. Run Reputation -e -w

