library(ggplot2)
results <- read.table(file="/Users/mkokkodi/git/reputation/data_results/real/results/coeffs_cv.csv",head=TRUE,sep=",")
summary(results)

technicalPE06Bin<-results[results$level=='Technical' & results$Approach=='PE' & results$Score==0.6 & results$Model=='Binomial',]

mean(technicalPE06Bin$a11)
sqrt(var(technicalPE06Bin$a11))
summary(technical)

tmp<-results[,c(5,6,7,8,9,10,11,12,13,14,15,16)]
tmp

computeCoeffVariances <- function(){
levels<-c("Technical","Non-technical","Generic")
models<-c("Binomial","Multinomial")
approaches<-c("PE","RS")
scores<-c(0.6,0.7,0.8,0.9)
for(model in models){
 
  for(level in levels){
  
    for(approach in approaches){
    
     if (model == 'Multinomial'){
        curSet<-results[results$level==level & results$Approach==approach & results$Score==0.9 & results$Model==model,]
        n<-16
        cat(level,model,' - ',approach)
        printCoeffs(n,curSet)
      }else{
        if( level == 'Generic')
          n<-10
          else
            n<-16
      for(score in scores){
        curSet<-results[results$level==level & results$Approach==approach & results$Score==score & results$Model==model,]
      
        cat(level,model,score,approach)
          printCoeffs(n,curSet)
      }
      }
    }
  }
}

}

printCoeffs<-function(n,curSet){
  
  
  for (i in 5:n) {
    
    
    cat(sprintf(" %.3f & (%.3f) & ", mean(curSet[,i]), sqrt(var(curSet[,i]))))
  }
  cat(sprintf("\n"))
}

computeCoeffVariances()




for(approach in approaches){
  if(level == 'Generic'){
    curSet<-results[results$level==level & results$Approach==approach  & results$Model==model,]
    n<-10
    printCoeffs(n,curSet)
  }
}