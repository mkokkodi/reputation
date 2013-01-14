predictions<- read.table(file="/Users/mkokkodi/git/reputation/data_results/real/results/results.csv",head=TRUE,sep=",")
summary(predictions)

predictions$
binPE <- predictions[predictions$model=="Binomial" && predictions$approach=="PE"]

wilcox.test(binPE$prediction, binPE$EMPrediction, paired=TRUE) 

wilcox.test(binPE$prediction, binPE$baseline, paired=TRUE) 