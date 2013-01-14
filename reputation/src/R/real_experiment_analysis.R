
library(ggplot2)
library(grid)
results <- read.table(file="/Users/mkokkodi/git/reputation/data_results/real/results/results.csv",head=TRUE,sep=",")
summary(results)

head(results)
mae_model_improvement<-(results$MAEBaseline -results$MAEModel)/results$MAEBaseline

improvements = transform(results,mae_improvement=mae_model_improvement)
head(improvements)



binlabels<-c(expression(theta == 0.6),expression(theta == 0.7),expression(theta == 0.8),expression(theta == 0.9)," Baseline")
binomial<-improvements[improvements$model=='Binomial',]
summary(binomial)
ob1 <- ggplot(binomial,aes(HistoryThreshold, mae_improvement*100,colour=factor(ScoreThreshold),shape=factor(exactModel))) 
ob1+geom_point(size=5)+facet_wrap(~approach,ncol=2)+theme_bw(22) + xlab(expression(History - eta))+
  ylab("Improvement %")+ 
  scale_colour_discrete(labels=binlabels)+labs(colour="",shape="")+
  theme(axis.title.y = element_text(vjust=-0.1),axis.title.x = element_text(vjust=-0.3),plot.margin = unit(c(1, 1, 1, 1), "cm"))

ggsave(file="/Users/mkokkodi/Dropbox/workspace/latex/reputation_informs/figures/binOdesk.pdf",width=14,height=5,dpi=300)


multinomial<-improvements[improvements$model=='Multinomial',]
ob1 <- ggplot(multinomial,aes(HistoryThreshold, mae_improvement*100,color=factor(approach),shape=factor(exactModel))) 
ob1+geom_point(size=5)+theme_bw(22)+xlab(expression(History - eta))+
  ylab("Improvement %")+labs(colour="",shape="")

ggsave(file="/Users/mkokkodi/Dropbox/workspace/latex/reputation_informs/figures/multOdesk.pdf",width=8,height=5,dpi=300)


#compare the two models.

compdata<-results[(results$model=='Binomial' & results$ScoreThreshold==0.9 & results$exactModel != 'Baseline') |(results$model=='Multinomial' & results$exactModel != 'Baseline'), ]
head(results)
ob1 <- ggplot(compdata,aes(HistoryThreshold, MAEModel,colour=model,shape=approach)) 
ob1+geom_point(size=5)+geom_line(size = 1.1)+facet_wrap(~exactModel,ncol=2)+theme_bw(22)+xlab(expression(History - eta))+
  ylab("MAE")+labs(colour="",shape="")

ggsave(file="/Users/mkokkodi/Dropbox/workspace/latex/reputation_informs/figures/comp.pdf",width=14,height=5,dpi=300)


