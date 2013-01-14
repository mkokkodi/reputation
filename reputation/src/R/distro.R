library(grid)
library(ggplot2)
library(MASS)  
distro<- read.table(file="/Users/mkokkodi/git/reputation/data_results/splited/raw/trainDistro.csv",head=TRUE,sep=",")

#levels=factor(round(distro$score,digits=1))

#Estimate multinomial prior for K=10
distrofreq<-table(factor(round(distro$score*0.5,digits=1)))
distrofreq2<-round(distrofreq/(10000))
distrofreq2

#Goodness of fit
probs = c(1/10000,1/10000,1/29, 1/10000, 1/10000, 1/10000,1/29, 1/29, 2/29, 2/29,22/29-5/10000) 
chisq.test(distrofreq, p=probs) 

summary(distro)
m <- ggplot(distrofreq)
m + geom_histogram(aes(y = ..density..)) + geom_density()