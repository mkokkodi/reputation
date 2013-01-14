library(ggplot2)
library(grid)
results <- read.table(file="/Users/mkokkodi/git/kdd12/results/cv_results.csv",head=TRUE,sep=",")
summary(results)

mae_improvement <- (results$MAEBasline-results$MAEModel)/results$MAEBasline
improvements = transform(results,improvement=mae_improvement )
head(improvements)


binlabels<-c(expression(theta == 0.6),expression(theta == 0.7  ),expression(theta == 0.8),expression(theta == 0.9)," Baseline")
binomial<-improvements[improvements$Model=='Binomial',]
summary(binomial)
ob1 <- ggplot(binomial,aes(History, improvement*100,colour=Score,shape=Score)) 
ob1+geom_point(size=5)+geom_line(size = 1)+facet_wrap(~Approach,ncol=2)+ theme_bw(22) + xlab(expression(History - eta))+
  ylab("Improvement %")+scale_shape_discrete(labels=binlabels)+ 
  scale_colour_discrete(labels=binlabels)+labs(colour="",shape="")+
  theme(axis.title.y = element_text(vjust=-0.1),axis.title.x = element_text(vjust=-0.3),plot.margin = unit(c(1, 1, 1, 1), "cm"))

ggsave(file="/Users/mkokkodi/Documents/workspace/reputation_informs/figures/binomialCV.pdf",width=15,height=5,dpi=300)
  
multinomial<-improvements[improvements$Model=='Multinomial',]
ob1 <- ggplot(multinomial,aes(History, improvement*100,colour=Approach,shape=Approach)) 
ob1+geom_point(size=5)+geom_line(size = 1)+theme_bw(22)+xlab(expression(History - eta))+
  ylab("Improvement %")+labs(colour="",shape="")

ggsave(file="/Users/mkokkodi/Documents/workspace/reputation_informs/figures/multinomialCV.pdf",width=8,height=5,dpi=300)
